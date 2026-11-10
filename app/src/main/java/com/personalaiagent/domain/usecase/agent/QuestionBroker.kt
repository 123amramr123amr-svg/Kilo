package com.personalaiagent.domain.usecase.agent

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/** A question the agent is waiting on, drawn as a dialog in the chat. */
data class PendingQuestion(
    val id: String,
    val question: String,
    val options: List<String> = emptyList(),
)

/**
 * The bridge between the `question` tool and the screen.
 *
 * The tool really waits: the agent loop is suspended on a [CompletableDeferred] until
 * the user types an answer (or taps an option), the question times out, or the run is
 * stopped. Nothing is simulated — if the user never answers, the model is told so and
 * has to decide on its own.
 */
@Singleton
class QuestionBroker @Inject constructor() {

    private val _pending = MutableStateFlow<PendingQuestion?>(null)
    val pending: StateFlow<PendingQuestion?> = _pending.asStateFlow()

    private val mutex = Mutex()
    private var waiter: CompletableDeferred<String?>? = null
    private var counter = 0

    suspend fun ask(
        question: String,
        options: List<String> = emptyList(),
        timeoutMillis: Long = DEFAULT_TIMEOUT_MS,
    ): String = mutex.withLock {
        val deferred = CompletableDeferred<String?>()
        waiter = deferred
        counter += 1
        _pending.value = PendingQuestion(
            id = "q-$counter",
            question = question.trim(),
            options = options.filter { it.isNotBlank() }.take(4),
        )
        val answer = try {
            withTimeout(timeoutMillis) { deferred.await() }
        } catch (timeout: TimeoutCancellationException) {
            null
        } finally {
            _pending.value = null
            waiter = null
        }
        answer?.takeIf { it.isNotBlank() } ?: "The user did not answer the question."
    }

    /** Called by the UI. Ignored when nothing is waiting. */
    fun answer(text: String) {
        waiter?.complete(text)
    }

    fun cancelQuestion() {
        waiter?.complete(null)
    }

    /** Clears a question that was left behind by a stopped run. */
    fun reset() {
        waiter?.complete(null)
        waiter = null
        _pending.value = null
    }

    val isWaiting: Boolean get() = _pending.value != null

    private companion object {
        /** Long enough to think, short enough not to hold the agent forever. */
        const val DEFAULT_TIMEOUT_MS = 180_000L
    }
}
