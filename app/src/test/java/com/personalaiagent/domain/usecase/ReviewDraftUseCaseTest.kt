package com.personalaiagent.domain.usecase

import com.personalaiagent.data.remote.ProviderClientRouter
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ReviewDraftUseCaseTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun useCase(client: FakeProviderClient = FakeProviderClient()): ReviewDraftUseCase {
        val credentials = FakeCredentialStore().apply { keys["p1"] = "sk-test" }
        return ReviewDraftUseCase(
            providerRepository = FakeProviderRepository(SampleData.provider()),
            modelRepository = FakeModelRepository(SampleData.model()),
            credentialStore = credentials,
            clients = ProviderClientRouter(client, client),
            json = json,
        )
    }

    @Test
    fun `parses questions, plan and improved prompt from the model`() {
        val review = useCase().parse(
            """
            Here you go:
            ```json
            {
              "clarity": 55,
              "questions": [
                {"question": "بأي لغة؟", "options": ["العربية", "English"], "multiple": false},
                {"question": "كم صفحة؟", "options": ["1", "3"]}
              ],
              "plan": ["قراءة الملف", "كتابة الملخص"],
              "improved_prompt": "اكتب ملخصًا من صفحة واحدة بالعربية"
            }
            ```
            """.trimIndent(),
        )

        assertNotNull(review)
        assertEquals(2, review!!.questions.size)
        assertEquals(listOf("العربية", "English"), review.questions.first().options)
        assertEquals(listOf("قراءة الملف", "كتابة الملخص"), review.plan)
        assertEquals("اكتب ملخصًا من صفحة واحدة بالعربية", review.improvedPrompt)
        assertEquals(55, review.clarity)
    }

    @Test
    fun `garbage output is rejected instead of crashing`() {
        assertEquals(null, useCase().parse("I cannot help with that."))
        assertEquals(null, useCase().parse("{not json"))
    }

    @Test
    fun `fallback always asks something and improves the prompt`() {
        val review = useCase().fallback("اكتب تقرير عن المبيعات")
        assertTrue(review.isFallback)
        assertTrue(review.questions.isNotEmpty())
        assertTrue(review.plan.isNotEmpty())
        assertTrue(review.improvedPrompt.startsWith("اكتب تقرير"))
        assertTrue(review.improvedPrompt.contains("ملاحظات للتنفيذ"))
    }

    @Test
    fun `provider answer is used when the client returns json`() = runTest {
        val client = FakeProviderClient(
            chunks = listOf(
                com.personalaiagent.data.remote.ChatChunk.Delta(
                    """{"clarity":80,"questions":[],"plan":["خطوة"],"improved_prompt":"نفّذ"}""",
                ),
                com.personalaiagent.data.remote.ChatChunk.Completed,
            ),
        )
        val review = useCase(client).invoke("اعمل حاجة", null)
        assertTrue(!review.isFallback)
        assertEquals(listOf("خطوة"), review.plan)
    }
}
