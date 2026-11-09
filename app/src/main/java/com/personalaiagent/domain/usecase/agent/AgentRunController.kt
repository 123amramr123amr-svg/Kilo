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
    private var watchdog: Job? = null

    /** Last time any event arrived; the watchdog aborts a silent run. */
    @Volatile private var lastEventAt: Long = 0L

    /** Set by the watchdog so cancellation is reported as a timeout, not a stop. */
    @Volatile private var stalled = false

    /**
     * Starts a run. Returns false when one is already in progress, so the caller
     * never leaves the UI in a "generating" state that nothing will ever finish.
     */
    fun start(
        conversationId: String?,
        text: String,
        modelId: String?,
        plan: List<String> = emptyList(),
        attachments: List<com.personalaiagent.domain.model.Attachment> = emptyList(),
    ): Boolean {
        if (_state.value.isRunning) return false
        _state.value = AgentRunState(isRunning = true, plan = plan, conversationId = conversationId)
        stalled = false
        lastEventAt = System.currentTimeMillis()
        startService(plan.firstOrNull())

        job = scope.launch {
            // Text is appended locally and pushed to the UI at most ~16x/second:
            // one state change per token re-composed the whole list and made the
            // screen feel stuck on slow phones.
            val streamed = StringBuilder()
            var lastFlush = 0L

            fun flush(force: Boolean = false) {
                val now = System.currentTimeMillis()
                if (!force && now - lastFlush < STREAM_FLUSH_MS) return
                lastFlush = now
                _state.value = _state.value.copy(streamingText = streamed.toString())
            }

            try {
                sendMessage(conversationId, text, modelId, attachments).collect { event ->
                    lastEventAt = System.currentTimeMillis()
                    when (event) {
                        is SendMessageEvent.Started -> {
                            flush(force = true)
                            _state.value = _state.value.copy(
                                conversationId = event.conversationId,
                                assistantMessageId = event.assistantMessageId,
                            )
                        }

                        is SendMessageEvent.Delta -> {
                            streamed.append(event.text)
                            flush()
                        }

                        is SendMessageEvent.ToolActivity -> {
                            flush(force = true)
                            _state.value = _state.value.copy(
                                toolActivity = _state.value.toolActivity + ToolActivityUi(
                                    toolName = event.toolName,
                                    detail = event.detail,
                                    ok = event.ok,
                                ),
                            )
                        }

                        is SendMessageEvent.Completed -> {
                            _state.value = _state.value.copy(
                                isRunning = false,
                                streamingText = "",
                                assistantMessageId = null,
                                finishedText = event.fullText,
                                completedRuns = _state.value.completedRuns + 1,
                            )
                        }

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
                _state.value = _state.value.copy(
                    isRunning = false,
                    streamingText = "",
                    assistantMessageId = null,
                    error = if (stalled) com.personalaiagent.core.AppError.Timeout else _state.value.error,
                )
                if (stalled) failures.trySend(com.personalaiagent.core.AppError.Timeout)
                throw cancelled
            } catch (unexpected: Exception) {
                // A crash inside the pipeline must never freeze the composer.
                val error = (unexpected as? com.personalaiagent.data.remote.ProviderException)?.error
                    ?: com.personalaiagent.core.AppError.Network
                _state.value = _state.value.copy(
                    isRunning = false,
                    streamingText = "",
                    assistantMessageId = null,
                    error = error,
                )
                failures.trySend(error)
            } finally {
                watchdog?.cancel()
                stopService()
            }
        }

        val started = job
        if (started != null) {
            watchdog = scope.launch {
                while (started.isActive) {
                    kotlinx.coroutines.delay(5_000)
                    val idle = System.currentTimeMillis() - lastEventAt
                    if (idle > com.personalaiagent.core.AppConstants.RUN_STALL_TIMEOUT_MS) {
                        stalled = true
                        started.cancel()
                        break
                    }
                }
            }
        }
        return true
    }

    fun stop() {
        stalled = false
        job?.cancel()
        watchdog?.cancel()
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

    private companion object {
        /** How often streamed text is pushed to the UI while a run is active. */
        const val STREAM_FLUSH_MS = 60L
    }
}
