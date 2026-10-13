package com.amrstudio.ui.chat

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amrstudio.data.model.Chat
import com.amrstudio.data.model.Message
import com.amrstudio.data.model.Message.MessageRole
import com.amrstudio.data.model.Model
import com.amrstudio.data.repository.ChatRepository
import com.amrstudio.data.repository.MessageRepository
import com.amrstudio.data.repository.ModelRepository
import com.amrstudio.utils.IdGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val modelRepository: ModelRepository,
    application: Application
) : ViewModel() {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats = _chats.asStateFlow()

    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat = _currentChat.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _availableModels = MutableStateFlow<List<Model>>(emptyList())
    val availableModels = _availableModels.asStateFlow()

    private val _selectedModel = MutableStateFlow<Model?>(null)
    val selectedModel = _selectedModel.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isStreaming = MutableStateFlow<Boolean>(false)
    val isStreaming = _isStreaming.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    var currentUserId: String? = null

    fun loadUserChats(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            chatRepository.getUserChats(userId).collect { chats ->
                _chats.value = chats
                if (_currentChat.value == null && chats.isNotEmpty()) {
                    selectChat(chats[0])
                }
            }
        }

        viewModelScope.launch {
            modelRepository.activeModels.collect { models ->
                _availableModels.value = models
                if (_selectedModel.value == null && models.isNotEmpty()) {
                    _selectedModel.value = models[0]
                }
            }
        }
    }

    fun selectChat(chat: Chat) {
        _currentChat.value = chat
        _messages.value = emptyList()
        _isLoading.value = true

        viewModelScope.launch {
            messageRepository.getChatMessages(chat.id).collect { messages ->
                _messages.value = messages
                _isLoading.value = false
            }
        }
    }

    fun createNewChat(title: String = "محادثة جديدة"): Chat {
        val chat = Chat(
            id = IdGenerator.generate(),
            userId = currentUserId ?: "",
            title = title,
            modelId = _selectedModel.value?.id ?: "",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        viewModelScope.launch {
            chatRepository.createChat(chat)
        }
        return chat
    }

    fun selectModel(model: Model) {
        _selectedModel.value = model
    }

    fun sendMessage(content: String, role: MessageRole = MessageRole.USER) {
        val chat = _currentChat.value ?: return
        val model = _selectedModel.value

        val userMessage = Message(
            id = IdGenerator.generate(),
            chatId = chat.id,
            role = role,
            content = content,
            modelId = model?.id,
            createdAt = LocalDateTime.now()
        )

        val newMessages = _messages.value + userMessage
        _messages.value = newMessages

        viewModelScope.launch {
            messageRepository.saveMessage(userMessage)
            chatRepository.updateChat(chat.copy(
                updatedAt = LocalDateTime.now(),
                title = if (chat.title == "محادثة جديدة") generateTitle(content) else chat.title
            ))
        }

        if (role == MessageRole.USER) {
            generateResponse(content, model)
        }
    }

    private fun generateResponse(userContent: String, model: Model?) {
        _isStreaming.value = true

        // This is a placeholder for actual AI integration
        // In production, this would call the appropriate API
        viewModelScope.launch {
            try {
                val response = simulateAiResponse(userContent, model)
                val assistantMessage = Message(
                    id = IdGenerator.generate(),
                    chatId = _currentChat.value!!.id,
                    role = MessageRole.ASSISTANT,
                    content = response,
                    modelId = model?.id,
                    createdAt = LocalDateTime.now()
                )
                _messages.value = _messages.value + assistantMessage
                messageRepository.saveMessage(assistantMessage)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isStreaming.value = false
            }
        }
    }

    private fun simulateAiResponse(input: String, model: Model?): String {
        // Simulated response - replace with actual API call
        return "هذا رد محاكي من ${model?.name ?? "النموذج المحدد"} على رسالتك: $input\n\n" +
               "في الإصدار النهائي، سيتم الاتصال بواجهة برمجة التطبيقات (API) للنموذج المحدد لتوليد رد ذكي حقيقي."
    }

    private fun generateTitle(content: String): String {
        val words = content.trim().split(" ")
        return words.take(5).joinToString(" ") + if (words.size > 5) "..." else ""
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            messageRepository.deleteChatMessages(chatId)
            chatRepository.deleteChat(chatId)
            val remaining = _chats.value.filter { it.id != chatId }
            _chats.value = remaining
            if (_currentChat.value?.id == chatId) {
                _currentChat.value = remaining.firstOrNull()
                remaining.firstOrNull()?.let { selectChat(it) }
            }
        }
    }

    fun cancelGeneration() {
        _isStreaming.value = false
        // Cancel any ongoing network request here
    }
}