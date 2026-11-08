package com.personalaiagent.domain.usecase

import com.personalaiagent.data.remote.ChatChunk
import com.personalaiagent.data.remote.ChatRequest
import com.personalaiagent.data.remote.ProviderClientRouter
import com.personalaiagent.domain.model.AiModel
import com.personalaiagent.domain.model.ClarifyQuestion
import com.personalaiagent.domain.model.DraftReview
import com.personalaiagent.domain.model.ProviderKind
import com.personalaiagent.domain.repository.CredentialStore
import com.personalaiagent.domain.repository.ModelRepository
import com.personalaiagent.domain.repository.ProviderRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Turns a rough request into a plan: a few clarifying questions, a checklist of
 * steps, and a sharper prompt. Runs before the work starts so the agent does not
 * guess. If the provider cannot be reached a local review is produced instead,
 * so the flow never blocks sending.
 */
class ReviewDraftUseCase @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val modelRepository: ModelRepository,
    private val credentialStore: CredentialStore,
    private val clients: ProviderClientRouter,
    private val json: Json,
) {

    suspend operator fun invoke(prompt: String, preferredModelId: String?): DraftReview {
        val text = prompt.trim()
        if (text.isEmpty()) return DraftReview()

        val model = resolveModel(preferredModelId) ?: return fallback(text)
        val provider = providerRepository.getProvider(model.providerId) ?: return fallback(text)
        val key = credentialStore.read(provider.id) ?: return fallback(text)

        val request = ChatRequest(
            kind = provider.kind,
            baseUrl = provider.baseUrl,
            apiKey = key,
            modelId = model.remoteId,
            messages = listOf(
                com.personalaiagent.domain.model.ChatTurn(
                    role = com.personalaiagent.domain.model.MessageRole.SYSTEM,
                    content = SYSTEM_PROMPT,
                ),
                com.personalaiagent.domain.model.ChatTurn(
                    role = com.personalaiagent.domain.model.MessageRole.USER,
                    content = text,
                ),
            ),
            params = model.params.copy(
                streaming = false,
                systemPrompt = if (provider.kind == ProviderKind.ANTHROPIC) SYSTEM_PROMPT else "",
                temperature = 0.3f,
                maxTokens = 900,
            ),
        )

        val response = runCatching {
            clients.forKind(provider.kind).stream(request).toList()
        }.getOrNull() ?: return fallback(text)

        val raw = response.filterIsInstance<ChatChunk.Delta>().joinToString("") { it.text }
        return parse(raw) ?: fallback(text)
    }

    /** Parses the model's JSON answer, tolerating prose or code fences around it. */
    fun parse(raw: String): DraftReview? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val element = runCatching { json.parseToJsonElement(raw.substring(start, end + 1)) }.getOrNull()
            as? JsonObject ?: return null

        val questions = (element["questions"] as? JsonArray).orEmpty().mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val question = (obj["question"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
            if (question.isEmpty()) return@mapNotNull null
            ClarifyQuestion(
                question = question,
                options = (obj["options"] as? JsonArray).orEmpty().mapNotNull {
                    (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
                }.take(4),
                allowMultiple = (obj["multiple"] as? JsonPrimitive)?.contentOrNull == "true",
            )
        }.take(4)

        val plan = (element["plan"] as? JsonArray).orEmpty().mapNotNull {
            (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
        }.take(8)

        val improved = (element["improved_prompt"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        val clarity = (element["clarity"] as? JsonPrimitive)?.intOrNull?.coerceIn(0, 100)

        val review = DraftReview(
            questions = questions,
            plan = plan,
            improvedPrompt = improved,
            clarity = clarity,
        )
        return review.takeIf { it.hasContent }
    }

    /** Works with no network: asks about the things requests usually miss. */
    fun fallback(prompt: String): DraftReview {
        val text = prompt.trim()
        val questions = mutableListOf<ClarifyQuestion>()
        val lower = text.lowercase()

        if (!lower.contains("عربي") && !lower.contains("english") && !lower.contains("لغة")) {
            questions += ClarifyQuestion(
                question = "بأي لغة تريد النتيجة النهائية؟",
                options = listOf("العربية", "English", "نفس لغة طلبي"),
            )
        }
        if (looksLikeFileWork(lower)) {
            questions += ClarifyQuestion(
                question = "أين أحفظ الملفات؟",
                options = listOf("في مساحة العمل الحالية", "مجلد جديد داخل مساحة العمل"),
            )
        }
        if (!lower.contains("مختصر") && !lower.contains("مفصل") && !lower.contains("قصير")) {
            questions += ClarifyQuestion(
                question = "ما المستوى المطلوب من التفصيل؟",
                options = listOf("مختصر ومباشر", "متوسط", "تفصيلي مع أمثلة"),
            )
        }

        val plan = buildList {
            add("فهم الطلب وتحديد الملفات أو الخطوات المطلوبة")
            if (looksLikeFileWork(lower)) add("إنشاء/تعديل الملفات داخل مساحة العمل")
            add("تنفيذ العمل والتحقق من النتيجة")
            add("تلخيص ما تم مع أسماء الملفات")
        }

        return DraftReview(
            questions = questions.take(3),
            plan = plan,
            improvedPrompt = buildString {
                append(text)
                append("\n\nملاحظات للتنفيذ: التزم بمساحة العمل الحالية، اكتب محتوى كاملًا بلا اختصارات")
                append("، واذكر أسماء الملفات التي أنشأتها أو عدّلتها في النهاية.")
            },
            clarity = null,
            isFallback = true,
        )
    }

    private fun looksLikeFileWork(lower: String): Boolean = FILE_HINTS.any { lower.contains(it) }

    private suspend fun resolveModel(preferredModelId: String?): AiModel? {
        preferredModelId?.let { id ->
            modelRepository.getModel(id)?.takeIf { it.enabled }?.let { return it }
        }
        modelRepository.getDefaultModel()?.takeIf { it.enabled }?.let { return it }
        return modelRepository.firstEnabledModel()
    }

    companion object {
        private val FILE_HINTS = listOf(
            "ملف", "ملفات", "مجلد", "اكتب", "أنشئ", "انشئ", "عدّل", "عدل", "احفظ",
            "file", "folder", "write", "create", "edit", "save", "report", "تقرير",
        )

        internal val SYSTEM_PROMPT = """
            You are the planning step of an on-device agent. The user will read your answer
            before any work starts, so be short and concrete.

            Return ONLY JSON with this shape:
            {
              "clarity": 0-100,
              "questions": [{"question": "...", "options": ["...", "..."], "multiple": false}],
              "plan": ["step 1", "step 2"],
              "improved_prompt": "a sharper version of the user's request"
            }

            Rules:
            - At most 3 questions, each answerable with one tap where possible.
            - Ask only what really changes the result (language, length, target file, tone).
            - The plan has 3-6 short steps.
            - improved_prompt keeps the user's intent and adds the missing constraints;
              it must read like the user wrote it.
            - Answer in the user's language.
        """.trimIndent()
    }
}
