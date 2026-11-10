package com.personalaiagent.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.domain.model.ConversationSummary
import com.personalaiagent.domain.repository.ChatRepository
import com.personalaiagent.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val loading: Boolean = true,
    val query: String = "",
    val conversations: List<ConversationSummary> = emptyList(),
    val pinnedIds: Set<String> = emptySet(),
    /** Card waiting for a new name, shown in the rename dialog. */
    val renaming: ConversationSummary? = null,
    /** Card waiting for a delete confirmation. */
    val deleting: ConversationSummary? = null,
) {
    val pinned: List<ConversationSummary> get() = conversations.filter { it.id in pinnedIds }
    val others: List<ConversationSummary> get() = conversations.filter { it.id !in pinnedIds }
    val isSearching: Boolean get() = query.isNotBlank()
    val isEmpty: Boolean get() = !loading && conversations.isEmpty()
}

/**
 * Backs the conversation history: every chat with its own name, its last line
 * and how much was said in it, searchable, pinnable, renamable, deletable.
 *
 * The list is built by one SQL query (see `ConversationDao.observeSummaries`),
 * so opening it stays instant even after hundreds of conversations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            query
                .flatMapLatest { term -> chatRepository.observeConversationSummaries(term) }
                .collect { summaries ->
                    _uiState.update { state ->
                        state.copy(
                            conversations = order(summaries, state.pinnedIds),
                            loading = false,
                        )
                    }
                }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        pinnedIds = settings.pinnedConversations,
                        conversations = order(state.conversations, settings.pinnedConversations),
                    )
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        query.value = value
    }

    fun togglePin(conversation: ConversationSummary) {
        val next = _uiState.value.pinnedIds.toMutableSet().apply {
            if (!add(conversation.id)) remove(conversation.id)
        }
        _uiState.update {
            it.copy(pinnedIds = next, conversations = order(it.conversations, next))
        }
        viewModelScope.launch { settingsRepository.setPinnedConversations(next) }
    }

    fun requestRename(conversation: ConversationSummary) {
        _uiState.update { it.copy(renaming = conversation) }
    }

    fun dismissRename() {
        _uiState.update { it.copy(renaming = null) }
    }

    fun commitRename(title: String) {
        val target = _uiState.value.renaming ?: return
        val clean = title.trim()
        _uiState.update { it.copy(renaming = null) }
        if (clean.isEmpty() || clean == target.title) return
        viewModelScope.launch { runCatching { chatRepository.renameConversation(target.id, clean) } }
    }

    fun requestDelete(conversation: ConversationSummary) {
        _uiState.update { it.copy(deleting = conversation) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleting = null) }
    }

    fun confirmDelete() {
        val target = _uiState.value.deleting ?: return
        _uiState.update { it.copy(deleting = null) }
        viewModelScope.launch {
            runCatching { chatRepository.deleteConversation(target.id) }
            if (target.id in _uiState.value.pinnedIds) {
                val next = _uiState.value.pinnedIds - target.id
                _uiState.update { it.copy(pinnedIds = next) }
                runCatching { settingsRepository.setPinnedConversations(next) }
            }
        }
    }

    /** Pinned first, then newest first — the order the list is drawn in. */
    private fun order(
        conversations: List<ConversationSummary>,
        pinned: Set<String>,
    ): List<ConversationSummary> = conversations.sortedWith(
        compareByDescending<ConversationSummary> { it.id in pinned }
            .thenByDescending { it.updatedAt },
    )
}
