package com.personalaiagent.data.remote

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * Thin OkHttp wrapper that exposes streaming and request/response helpers with
 * coroutine cancellation support. No request or response body is ever logged.
 */
class HttpEngine(private val client: OkHttpClient) {

    /**
     * Same connection pool, but without a read timeout: long "thinking" models
     * legitimately stay silent for a while before the first token, and the
     * previous 90 s limit was reported to the user as "the provider timed out".
     */
    private val streamingClient: OkHttpClient by lazy {
        client.newBuilder()
            .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
    }

    /** Streams raw SSE events. Cancelling the collector cancels the HTTP call. */
    fun streamEvents(
        url: String,
        headers: Map<String, String>,
        jsonBody: String,
    ): Flow<SseEvent> = callbackFlow {
        val parser = SseStreamParser()
        // A stream is not a request/response: the model may think for a minute
        // before the first token. The read timeout must not fire during that
        // silence — the run watchdog (120 s with no event at all) is what
        // decides a stream is dead.
        val call = streamingClient.newCall(buildRequest(url, headers, jsonBody))

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(if (call.isCanceled()) CancellationException("http cancelled") else e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        close(failureOf(resp))
                        return
                    }
                    val source = resp.body?.source()
                    if (source == null) {
                        close(ProviderException(com.personalaiagent.core.AppError.Parse))
                        return
                    }
                    try {
                        while (true) {
                            val line = source.readUtf8Line() ?: break
                            parser.consume(line)?.let { event -> trySendBlocking(event) }
                            if (call.isCanceled()) break
                        }
                        parser.flush()?.let { event -> trySendBlocking(event) }
                        close()
                    } catch (e: IOException) {
                        close(e)
                    } catch (e: Exception) {
                        close(e)
                    }
                }
            }
        })

        awaitClose { call.cancel() }
    }
        .buffer(Channel.UNLIMITED)

    suspend fun getJson(url: String, headers: Map<String, String>): String {
        val request = Request.Builder().url(url).get().apply {
            headers.forEach { (name, value) -> addHeader(name, value) }
        }.build()
        return execute(request)
    }

    suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        jsonBody: String,
    ): String {
        val request = buildRequest(url, headers, jsonBody)
        return execute(request)
    }

    /** Downloads a small binary resource (e.g. a generated image URL). */
    suspend fun getBytes(url: String): ByteArray {
        val request = Request.Builder().url(url).get().build()
        val call = client.newCall(request)
        val response = suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            if (call.isCanceled()) CancellationException("http cancelled") else e,
                        )
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) continuation.resume(response) else response.close()
                }
            })
        }
        response.use { resp ->
            if (!resp.isSuccessful) throw failureOf(resp)
            return resp.body?.bytes() ?: throw ProviderException(com.personalaiagent.core.AppError.Parse)
        }
    }

    private suspend fun execute(request: Request): String {
        val call = client.newCall(request)
        val response = suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            if (call.isCanceled()) CancellationException("http cancelled") else e,
                        )
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) continuation.resume(response) else response.close()
                }
            })
        }

        response.use { resp ->
            if (!resp.isSuccessful) throw failureOf(resp)
            return resp.body?.string() ?: throw ProviderException(com.personalaiagent.core.AppError.Parse)
        }
    }

    private fun buildRequest(url: String, headers: Map<String, String>, jsonBody: String): Request {
        val body = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        return Request.Builder()
            .url(url)
            .post(body)
            .apply { headers.forEach { (name, value) -> addHeader(name, value) } }
            .build()
    }

    /**
     * Reads an error response once and keeps everything the UI needs: the HTTP
     * status, the provider's own sentence and the `Retry-After` hint.
     */
    private fun failureOf(resp: Response): ProviderException {
        val retryAfter = ProviderErrors.retryAfterSeconds(
            resp.header("Retry-After")
                ?: resp.header("x-ratelimit-reset-requests")
                ?: resp.header("x-ratelimit-reset"),
        )
        val body = runCatching {
            resp.peekBody(ERROR_BODY_LIMIT).string()
        }.getOrNull()
        val error = ProviderErrors.toAppError(resp.code, body, retryAfter)
        return ProviderException(error, retryAfterSeconds = retryAfter, httpCode = resp.code)
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Enough of an error page to read the sentence, never the whole body. */
        const val ERROR_BODY_LIMIT = 8_192L
    }
}
