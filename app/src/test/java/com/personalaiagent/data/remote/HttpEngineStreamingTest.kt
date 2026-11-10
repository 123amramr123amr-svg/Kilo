package com.personalaiagent.data.remote

import com.personalaiagent.core.AppError
import com.personalaiagent.core.Outcome
import com.personalaiagent.domain.model.ChatTurn
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.model.ModelParams
import com.personalaiagent.domain.model.Provider
import com.personalaiagent.domain.model.ProviderKind
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** End-to-end test of the streaming HTTP layer against a local mock server. */
class HttpEngineStreamingTest {

    private lateinit var server: MockWebServer
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun engine() = HttpEngine(OkHttpClient())

    private fun client(baseUrl: String) = OpenAiCompatibleClient(engine(), json)

    private fun request(baseUrl: String) = ChatRequest(
        kind = ProviderKind.OPENAI_COMPATIBLE,
        baseUrl = baseUrl,
        apiKey = "sk-secret",
        modelId = "gpt-4o",
        messages = listOf(ChatTurn(MessageRole.USER, "hi")),
        params = ModelParams(streaming = true),
    )

    private fun provider(baseUrl: String) = Provider(
        id = "p1",
        name = "OpenAI",
        kind = ProviderKind.OPENAI_COMPATIBLE,
        baseUrl = baseUrl,
        isDefault = true,
        hasKey = true,
        status = com.personalaiagent.domain.model.ProviderStatus.CONNECTED,
        lastCheckedAt = null,
        createdAt = 0L,
    )

    @Test
    fun `streams server sent events into deltas`() = runTest {
        val body = buildString {
            append("data: {\"choices\":[{\"delta\":{\"content\":\"Hel\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"lo\"}}]}\n\n")
            append("data: {\"choices\":[],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":2}}\n\n")
            append("data: [DONE]\n\n")
        }
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(body),
        )

        val chunks = client(server.url("/v1").toString()).stream(request(server.url("/v1").toString())).toList()

        val text = chunks.filterIsInstance<ChatChunk.Delta>().joinToString("") { it.text }
        assertEquals("Hello", text)
        assertTrue(chunks.filterIsInstance<ChatChunk.Usage>().any { it.tokenIn == 5 && it.tokenOut == 2 })
        assertTrue(chunks.last() is ChatChunk.Completed)

        val recorded = server.takeRequest()
        assertEquals("/v1/chat/completions", recorded.path)
        assertEquals("Bearer sk-secret", recorded.getHeader("Authorization"))
        assertTrue(recorded.body.readUtf8().contains("\"stream\":true"))
    }

    @Test
    fun `maps unauthorized response to auth error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"invalid key"}"""))

        val error = runCatching {
            client(server.url("/v1").toString()).stream(request(server.url("/v1").toString())).toList()
        }.exceptionOrNull()

        assertTrue(error is ProviderException)
        assertEquals(AppError.Auth, (error as ProviderException).error)
    }

    @Test
    fun `maps rate limit response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))
        val error = runCatching {
            client(server.url("/v1").toString()).stream(request(server.url("/v1").toString())).toList()
        }.exceptionOrNull()
        assertEquals(AppError.RateLimit(), (error as ProviderException).error)
    }

    @Test
    fun `fetches and filters the model catalog`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"data":[
                        {"id":"gpt-4o"},
                        {"id":"text-embedding-3-small"},
                        {"id":"whisper-1"}
                    ]}""",
                ),
        )
        val result = client(server.url("/v1").toString()).fetchModels(
            provider(server.url("/v1").toString()),
            "sk-secret",
        )
        assertTrue(result is Outcome.Success)
        val models = (result as Outcome.Success).value
        assertEquals(listOf("gpt-4o"), models.map { it.remoteId })
        assertEquals("Bearer sk-secret", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `non streaming request returns a single delta`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"choices":[{"message":{"role":"assistant","content":"Full answer"}}]}"""),
        )
        val base = server.url("/v1").toString()
        val chunks = client(base).stream(request(base).copy(params = ModelParams(streaming = false))).toList()

        assertEquals("Full answer", chunks.filterIsInstance<ChatChunk.Delta>().single().text)
        assertTrue(chunks.last() is ChatChunk.Completed)
        assertTrue(server.takeRequest().body.readUtf8().contains("\"stream\":false"))
    }
}
