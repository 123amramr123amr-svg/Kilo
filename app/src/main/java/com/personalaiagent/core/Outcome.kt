package com.personalaiagent.core

/** Result type used by repositories and use cases where failure is expected. */
sealed interface Outcome<out T> {

    data class Success<T>(val value: T) : Outcome<T>

    data class Failure(val error: AppError) : Outcome<Nothing>

    val isSuccess: Boolean get() = this is Success

    fun getOrNull(): T? = (this as? Success)?.value
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T, R> Outcome<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (AppError) -> R,
): R = when (this) {
    is Outcome.Success -> onSuccess(value)
    is Outcome.Failure -> onFailure(error)
}
