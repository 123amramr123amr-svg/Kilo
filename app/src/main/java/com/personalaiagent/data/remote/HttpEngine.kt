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

    /** Streams raw SSE events. Cancelling the collector cancels the HTTP call. */
    fun streamEvents(
        url: String,
        headers: Map<String, String>,
        jsonBody: String,
    ): Flow<SseEvent> = callbackFlow {
        val parser = SseStreamParser()
        val call = client.newCall(buildRequest(url, headers, jsonBody))

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(if (call.isCanceled()) CancellationException("http cancelled") else e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        close(ProviderException(httpStatusToAppError(resp.code)))
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
            if (!resp.isSuccessful) throw ProviderException(httpStatusToAppError(resp.code))
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
            if (!resp.isSuccessful) throw ProviderException(httpStatusToAppError(resp.code))
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

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
