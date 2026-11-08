package com.personalaiagent.domain.usecase.agent

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.personalaiagent.core.AppError
import com.personalaiagent.di.ApplicationScope
import com.personalaiagent.domain.usecase.SendMessageEvent
import com.personalaiagent.domain.usecase.SendMessageUseCase
import com.personalaiagent.platform.AgentRunService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class ToolActivityUi(
    val toolName: String,
    val detail: String,
    val ok: Boolean,
    val done: Boolean = true,
)

/** Everything the UI needs to render a run, including one that survives the screen. */
data class AgentRunState(
    val isRunning: Boolean = false,
    val conversationId: String? = null,
    val assistantMessageId: String? = null,
    val streamingText: String = "",
    val toolActivity: List<ToolActivityUi> = emptyList(),
    val plan: List<String> = emptyList(),
    val error: AppError? = null,
    val finishedText: String? = null,
    /** Increments on every completed run so the UI can react once. */
    val completedRuns: Int = 0,
)

/**
 * Owns the running agent job in an application-scoped coroutine, so leaving the
 * chat screen, locking the phone or switching apps does not cancel the work.
 * A foreground service keeps the process alive while a run is active.
 */
@Singleton
class AgentRunController @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val sendMessage: SendMessageUseCase,
) {

    private val _state = MutableStateFlow(AgentRunState())
    val state: StateFlow<AgentRunState> = _state.asStateFlow()

    private val failures = Channel<AppError>(Channel.CONFLATED)
    val errors = failures.receiveAsFlow()

    private var job: Job? = null

    fun start(
        conversationId: String?,
        text: String,
        modelId: String?,
        plan: List<String> = emptyList(),
        attachments: List<com.personalaiagent.domain.model.Attachment> = emptyList(),
    ) {
        if (_state.value.isRunning) return
        _state.value = AgentRunState(isRunning = true, plan = plan, conversationId = conversationId)
        startService(plan.firstOrNull())

        job = scope.launch {
            try {
                sendMessage(conversationId, text, modelId, attachments).collect { event ->
                    when (event) {
                        is SendMessageEvent.Started -> _state.value = _state.value.copy(
                            conversationId = event.conversationId,
                            assistantMessageId = event.assistantMessageId,
                        )

                        is SendMessageEvent.Delta -> _state.value = _state.value.copy(
                            streamingText = _state.value.streamingText + event.text,
                        )

                        is SendMessageEvent.ToolActivity -> _state.value = _state.value.copy(
                            toolActivity = _state.value.toolActivity + ToolActivityUi(
                                toolName = event.toolName,
                                detail = event.detail,
                                ok = event.ok,
                            ),
                        )

                        is SendMessageEvent.Completed -> _state.value = _state.value.copy(
                            isRunning = false,
                            streamingText = "",
                            assistantMessageId = null,
                            finishedText = event.fullText,
                            completedRuns = _state.value.completedRuns + 1,
                        )

                        is SendMessageEvent.Failed -> {
                            _state.value = _state.value.copy(
                                isRunning = false,
                                streamingText = "",
                                assistantMessageId = null,
                                error = event.error,
                            )
                            failures.trySend(event.error)
                        }
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                _state.value = _state.value.copy(isRunning = false, streamingText = "")
                throw cancelled
            } finally {
                stopService()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.value = _state.value.copy(isRunning = false, streamingText = "", assistantMessageId = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun attachConversation(conversationId: String?) {
        _state.value = _state.value.copy(conversationId = conversationId)
    }

    /** True when a run is active; the chat screen uses it to re-attach on open. */
    val isRunning: Boolean get() = _state.value.isRunning

    private fun startService(title: String?) {
        val intent = Intent(context, AgentRunService::class.java).apply {
            putExtra(AgentRunService.EXTRA_TITLE, title)
        }
        runCatching { ContextCompat.startForegroundService(context, intent) }
    }

    private fun stopService() {
        runCatching { context.stopService(Intent(context, AgentRunService::class.java)) }
    }
}
