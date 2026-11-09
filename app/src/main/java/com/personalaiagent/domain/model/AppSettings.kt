package com.personalaiagent.domain.model

data class AppSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val theme: ThemeMode = ThemeMode.MIDNIGHT,
    val ttsEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val streamingEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val displayName: String = "",
    val createdAt: Long = 0L,
    /** SAF tree URI of the folder the agent may work in (null = not chosen). */
    val workspaceUri: String? = null,
    val workspaceName: String? = null,
    /** Master switch for the file tools. */
    val agentFileToolsEnabled: Boolean = true,
    /**
     * Ask clarifying questions and show a task list before starting work.
     * Off by default: it costs a whole extra round trip before the answer starts,
     * and the toggle in the composer turns it back on whenever it is wanted.
     */
    val reviewRequests: Boolean = false,
    /** Show the live "what is the agent doing" panel in the chat. */
    val showAgentActivity: Boolean = true,
) {
    val hasWorkspace: Boolean get() = !workspaceUri.isNullOrBlank()
}
