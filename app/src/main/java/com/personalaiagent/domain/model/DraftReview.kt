package com.personalaiagent.domain.model

/** One clarifying question the agent asks before doing the work. */
data class ClarifyQuestion(
    val question: String,
    val options: List<String> = emptyList(),
    val allowMultiple: Boolean = false,
)

/**
 * The agent's read on a request *before* executing it: what it needs to know,
 * what it plans to do, and a sharper version of the prompt.
 */
data class DraftReview(
    val questions: List<ClarifyQuestion> = emptyList(),
    val plan: List<String> = emptyList(),
    val improvedPrompt: String = "",
    /** 0..100, how workable the original request already is. */
    val clarity: Int? = null,
    /** True when the model could not be reached and this was built locally. */
    val isFallback: Boolean = false,
) {
    val hasContent: Boolean get() = questions.isNotEmpty() || plan.isNotEmpty() || improvedPrompt.isNotBlank()
}
