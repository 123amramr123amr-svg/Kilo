@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.personalaiagent.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.core.AppError
import com.personalaiagent.domain.model.AiModel
import com.personalaiagent.domain.model.ChatMessage
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.repository.ChatRepository
import com.personalaiagent.domain.repository.ModelRepository
import com.personalaiagent.domain.repository.ProviderRepository
import com.personalaiagent.domain.repository.SettingsRepository
import com.personalaiagent.domain.usecase.SendMessageEvent
import com.personalaiagent.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
) {
    val canSend: Boolean get() = input.isNotBlank() && activeModel != null && !isGenerating
}

data class ToolActivityUi(
    val toolName: String,
    val detail: String,
    val ok: Boolean,
    val done: Boolean = true,
)

sealed interface ChatEvent {
    data class SpeakAnswer(val text: String) : ChatEvent
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val providerRepository: ProviderRepository,
    private val modelRepository: ModelRepository,
    settingsRepository: SettingsRepository,
    private val sendMessage: SendMessageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<ChatEvent>(Channel.BUFFERED)
    val events: Flow<ChatEvent> = eventChannel.receiveAsFlow()

    private val conversationId = MutableStateFlow<String?>(null)
    private val selectedModelId = MutableStateFlow<String?>(null)
    private var followLatestConversation = true
    private var sendJob: Job? = null

    init {
        observeConversation()
        observeMessages()
        observeModels(settingsRepository)
        observeProviders()
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun newChat() {
        sendJob?.cancel()
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
            )
        }
    }

    fun deleteCurrentConversation() {
        val id = _uiState.value.conversationId ?: return
        sendJob?.cancel()
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
            followLatestConversation = false
            conversationId.value = null
            _uiState.update { it.copy(messages = emptyList(), conversationId = null, error = null) }
        }
    }

    fun send() {
        val state = _uiState.value
        val text = state.input.trim()
        if (text.isEmpty() || state.isGenerating) return
        if (state.activeModel == null) {
            _uiState.update { it.copy(error = AppError.NoModel) }
            return
        }
        execute(text)
    }

    fun retryLastMessage() {
        val lastUser = _uiState.value.messages.lastOrNull { it.role == MessageRole.USER } ?: return
        if (_uiState.value.isGenerating) return
        execute(lastUser.content)
    }

    private fun execute(text: String) {
        _uiState.update {
            it.copy(
                input = "",
                isGenerating = true,
                error = null,
                streamingText = "",
                streamingMessageId = null,
                toolActivity = emptyList(),
            )
        }
        val targetConversation = if (followLatestConversation) conversationId.value else null

        sendJob = viewModelScope.launch {
            sendMessage(
                conversationId = targetConversation,
                text = text,
                preferredModelId = selectedModelId.value ?: _uiState.value.activeModel?.id,
            ).collect { event ->
                when (event) {
                    is SendMessageEvent.Started -> {
                        followLatestConversation = true
                        conversationId.value = event.conversationId
                        _uiState.update {
                            it.copy(
                                conversationId = event.conversationId,
                                streamingMessageId = event.assistantMessageId,
                            )
                        }
                    }

                    is SendMessageEvent.Delta -> {
                        _uiState.update { it.copy(streamingText = it.streamingText + event.text) }
                    }

                    is SendMessageEvent.ToolActivity -> {
                        _uiState.update { current ->
                            current.copy(
                                toolActivity = current.toolActivity + ToolActivityUi(
                                    toolName = event.toolName,
                                    detail = event.detail,
                                    ok = event.ok,
                                ),
                            )
                        }
                    }

                    is SendMessageEvent.Completed -> {
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                streamingText = "",
                                streamingMessageId = null,
                            )
                        }
                        eventChannel.trySend(ChatEvent.SpeakAnswer(event.fullText))
                    }

                    is SendMessageEvent.Failed -> {
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                streamingText = "",
                                streamingMessageId = null,
                                error = event.error,
                            )
                        }
                    }
                }
            }
        }.also { job ->
            job.invokeOnCompletion {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun stopGenerating() {
        sendJob?.cancel()
        _uiState.update { it.copy(isGenerating = false) }
    }

    override fun onCleared() {
        super.onCleared()
        sendJob?.cancel()
    }
}
