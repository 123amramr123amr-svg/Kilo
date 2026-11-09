@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.personalaiagent.ui.screens.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.core.AppError
import com.personalaiagent.domain.model.AiModel
import com.personalaiagent.domain.model.Attachment
import com.personalaiagent.domain.model.ChatMessage
import com.personalaiagent.domain.model.DraftReview
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.repository.ChatRepository
import com.personalaiagent.domain.repository.ModelRepository
import com.personalaiagent.domain.repository.ProviderRepository
import com.personalaiagent.domain.repository.SettingsRepository
import com.personalaiagent.domain.usecase.ReviewDraftUseCase
import com.personalaiagent.domain.usecase.agent.AgentRunController
import com.personalaiagent.domain.usecase.agent.ToolActivityUi
import com.personalaiagent.platform.AttachmentLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val isLoading: Boolean = true,
    val hasProviders: Boolean = false,
    val models: List<AiModel> = emptyList(),
    val activeModel: AiModel? = null,
    val messages: List<ChatMessage> = emptyList(),
    val conversationId: String? = null,
    val input: String = "",
    val isGenerating: Boolean = false,
    val streamingMessageId: String? = null,
    val streamingText: String = "",
    val error: AppError? = null,
    val ttsEnabled: Boolean = false,
    val streamingEnabled: Boolean = true,
    val workspaceName: String? = null,
    val toolsEnabled: Boolean = false,
    val toolActivity: List<ToolActivityUi> = emptyList(),
    /** Steps the agent committed to for the current request. */
    val plan: List<String> = emptyList(),
    val reviewFirst: Boolean = true,
    val isReviewing: Boolean = false,
    val review: DraftReview? = null,
    val reviewAnswers: Map<Int, String> = emptyMap(),
    val attachments: List<Attachment> = emptyList(),
    val isAttaching: Boolean = false,
) {
    val canSend: Boolean get() = (input.isNotBlank() || attachments.isNotEmpty()) &&
        activeModel != null && !isGenerating && !isReviewing
}

sealed interface ChatEvent {
    data class SpeakAnswer(val text: String) : ChatEvent
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val providerRepository: ProviderRepository,
    private val modelRepository: ModelRepository,
    private val settingsRepository: SettingsRepository,
    private val reviewDraft: ReviewDraftUseCase,
    private val runController: AgentRunController,
    private val attachmentLoader: AttachmentLoader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<ChatEvent>(Channel.BUFFERED)
    val events: Flow<ChatEvent> = eventChannel.receiveAsFlow()

    private val conversationId = MutableStateFlow<String?>(null)
    private val selectedModelId = MutableStateFlow<String?>(null)
    private var followLatestConversation = true
    private var lastCompletedRuns = 0

    init {
        observeConversation()
        observeMessages()
        observeModels(settingsRepository)
        observeProviders()
        observeRun()
    }

    private fun observeConversation() {
        viewModelScope.launch {
            chatRepository.observeLatestConversation().collect { conversation ->
                if (followLatestConversation) {
                    conversationId.value = conversation?.id
                    _uiState.update { it.copy(conversationId = conversation?.id) }
                }
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            conversationId
                .flatMapLatest { id ->
                    if (id == null) flowOf(emptyList()) else chatRepository.observeMessages(id)
                }
                .collect { messages ->
                    _uiState.update { it.copy(messages = messages, isLoading = false) }
                }
        }
    }

    private fun observeModels(settingsRepository: SettingsRepository) {
        viewModelScope.launch {
            combine(
                modelRepository.observeEnabledModels(),
                selectedModelId,
                settingsRepository.settings,
            ) { models, selected, settings ->
                Triple(models, selected, settings)
            }.collect { (models, selected, settings) ->
                val active = models.firstOrNull { it.id == selected }
                    ?: models.firstOrNull { it.isDefault }
                    ?: models.firstOrNull()
                _uiState.update {
                    it.copy(
                        models = models,
                        activeModel = active,
                        ttsEnabled = settings.ttsEnabled,
                        streamingEnabled = settings.streamingEnabled,
                        workspaceName = settings.workspaceName,
                        toolsEnabled = settings.hasWorkspace && settings.agentFileToolsEnabled,
                        reviewFirst = settings.reviewRequests,
                    )
                }
            }
        }
    }

    private fun observeProviders() {
        viewModelScope.launch {
            providerRepository.observeProviders().collect { providers ->
                _uiState.update { it.copy(hasProviders = providers.isNotEmpty()) }
            }
        }
    }

    /**
     * Mirrors the application-scoped run into the screen, so a run started from
     * here (or still going after the screen was recreated) renders correctly.
     */
    private fun observeRun() {
        viewModelScope.launch {
            runController.state.collect { run ->
                if (run.conversationId != null && followLatestConversation) {
                    conversationId.value = run.conversationId
                }
                _uiState.update {
                    it.copy(
                        isGenerating = run.isRunning,
                        streamingText = run.streamingText,
                        streamingMessageId = run.assistantMessageId,
                        toolActivity = run.toolActivity,
                        plan = run.plan.ifEmpty { it.plan },
                        error = run.error ?: it.error,
                    )
                }
                if (run.completedRuns > lastCompletedRuns) {
                    lastCompletedRuns = run.completedRuns
                    run.finishedText?.let { text -> eventChannel.trySend(ChatEvent.SpeakAnswer(text)) }
                }
            }
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun onVoiceInput(value: String) {
        val text = value.trim()
        if (text.isEmpty()) return
        _uiState.update {
            val existing = it.input.trimEnd()
            it.copy(input = if (existing.isEmpty()) text else "$existing $text")
        }
    }

    fun selectModel(modelId: String) {
        selectedModelId.value = modelId
    }

    fun toggleReviewFirst() {
        val next = !_uiState.value.reviewFirst
        _uiState.update { it.copy(reviewFirst = next) }
        viewModelScope.launch { settingsRepository.setReviewRequests(next) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
        runController.clearError()
    }

    // ------------------------------------------------------------- attachments

    fun attach(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAttaching = true, error = null) }
            val result = runCatching { attachmentLoader.load(uri) }.getOrNull()
            val attachments = result?.getOrNull()
            when {
                attachments == null -> _uiState.update {
                    it.copy(isAttaching = false, error = AppError.Unknown("attachment_failed"))
                }

                attachments.isEmpty() -> _uiState.update {
                    it.copy(isAttaching = false, error = AppError.Unknown("attachment_empty"))
                }

                else -> _uiState.update {
                    it.copy(isAttaching = false, attachments = it.attachments + attachments)
                }
            }
        }
    }

    fun removeAttachment(index: Int) {
        _uiState.update { current ->
            current.copy(attachments = current.attachments.filterIndexed { i, _ -> i != index })
        }
    }

    fun clearAttachments() {
        _uiState.update { it.copy(attachments = emptyList()) }
    }

    // ------------------------------------------------------------ review flow

    /** Called by the send button: reviews the request first, then runs it. */
    fun send() {
        val state = _uiState.value
        if (state.isGenerating || state.isReviewing) return
        val text = state.input.trim()
        if (text.isEmpty() && state.attachments.isEmpty()) return
        if (state.activeModel == null) {
            _uiState.update { it.copy(error = AppError.NoModel) }
            return
        }
        if (text.isEmpty()) {
            // Attachments without text: nothing to clarify.
            execute(text = "", plan = emptyList())
            return
        }
        if (!state.reviewFirst) {
            execute(text = text, plan = emptyList())
            return
        }
        _uiState.update { it.copy(isReviewing = true, error = null) }
        viewModelScope.launch {
            val review = runCatching {
                reviewDraft(text, _uiState.value.activeModel?.id)
            }.getOrNull()
            _uiState.update {
                it.copy(
                    isReviewing = false,
                    review = review,
                    reviewAnswers = emptyMap(),
                )
            }
        }
    }

    fun answerQuestion(index: Int, answer: String) {
        _uiState.update { current ->
            current.copy(reviewAnswers = current.reviewAnswers + (index to answer))
        }
    }

    fun dismissReview() {
        _uiState.update { it.copy(review = null, reviewAnswers = emptyMap()) }
    }

    /** Runs the improved prompt plus the answers the user picked. */
    fun confirmReview() {
        val state = _uiState.value
        val review = state.review ?: return
        val answers = review.questions.mapIndexedNotNull { index, question ->
            state.reviewAnswers[index]?.let { answer -> "${question.question} → $answer" }
        }
        val text = buildString {
            append(review.improvedPrompt.ifBlank { state.input.trim() })
            if (answers.isNotEmpty()) {
                append("\n\n")
                append(answers.joinToString("\n"))
            }
        }.trim()
        val plan = review.plan
        _uiState.update { it.copy(review = null, reviewAnswers = emptyMap()) }
        execute(text = text, plan = plan)
    }

    /** Skips the suggestions and sends exactly what the user typed. */
    fun sendWithoutReview() {
        val text = _uiState.value.input.trim()
        _uiState.update { it.copy(review = null, reviewAnswers = emptyMap()) }
        execute(text = text, plan = emptyList())
    }

    fun stopGenerating() {
        runController.stop()
        _uiState.update { it.copy(isGenerating = false) }
    }

    fun newChat() {
        runController.stop()
        followLatestConversation = false
        conversationId.value = null
        _uiState.update {
            it.copy(
                conversationId = null,
                messages = emptyList(),
                input = "",
                error = null,
                streamingText = "",
                streamingMessageId = null,
                isGenerating = false,
                toolActivity = emptyList(),
                plan = emptyList(),
                attachments = emptyList(),
            )
        }
    }

    fun deleteCurrentConversation() {
        val id = _uiState.value.conversationId ?: return
        runController.stop()
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
            followLatestConversation = false
            conversationId.value = null
            _uiState.update { it.copy(messages = emptyList(), conversationId = null, error = null) }
        }
    }

    fun retryLastMessage() {
        val lastUser = _uiState.value.messages.lastOrNull { it.role == MessageRole.USER } ?: return
        if (_uiState.value.isGenerating) return
        execute(lastUser.content, plan = emptyList())
    }

    private fun execute(text: String, plan: List<String>) {
        val state = _uiState.value
        val attachments = state.attachments
        followLatestConversation = true
        _uiState.update {
            it.copy(
                input = "",
                isGenerating = true,
                error = null,
                streamingText = "",
                streamingMessageId = null,
                toolActivity = emptyList(),
                plan = plan,
                attachments = emptyList(),
            )
        }
        runController.start(
            conversationId = if (followLatestConversation) conversationId.value else null,
            text = text.ifBlank { attachments.firstOrNull()?.sourceLabel ?: "الملف المرفق" },
            modelId = selectedModelId.value ?: state.activeModel?.id,
            plan = plan,
            attachments = attachments,
        )
    }
}
