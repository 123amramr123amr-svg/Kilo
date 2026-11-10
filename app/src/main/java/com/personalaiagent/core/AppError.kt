package com.personalaiagent.core

/**
 * Every failure the app can surface to the user.
 *
 * Keeping these as explicit cases (instead of raw exceptions) means the UI can
 * always render a localised, actionable message and tests can assert on causes.
 */
sealed class AppError {

    /** No provider has been configured yet. */
    data object NoProvider : AppError()

    /** No enabled model is available for the selected provider. */
    data object NoModel : AppError()

    /** The provider has no API key stored on this device. */
    data class MissingKey(val providerName: String) : AppError()

    /** Input validation failed before reaching the network. */
    data class InvalidInput(val reason: String) : AppError()

    data object Network : AppError()
    data object Timeout : AppError()
    data object Auth : AppError()

    /**
     * The provider asked us to slow down (HTTP 429).
     *
     * @param retryAfterSeconds what the `Retry-After` header said, when present —
     *   the retry loop waits exactly that long instead of guessing.
     * @param detail the provider's own sentence ("Rate limit reached for gpt-4o…"),
     *   shown as-is: a bare "rate limited" hides which limit was hit.
     */
    data class RateLimit(
        val retryAfterSeconds: Int? = null,
        val detail: String? = null,
    ) : AppError()

    /** The account is out of credit/quota — retrying cannot fix it. */
    data class QuotaExceeded(val detail: String? = null) : AppError()

    /** The prompt is longer than the model's context window. */
    data object ContextTooLong : AppError()

    data class Server(val code: Int, val detail: String? = null) : AppError()

    /**
     * The provider answered 404 for the requested path or model. Almost always a
     * wrong base URL (missing `/v1`) or a model id that provider does not serve.
     */
    data object HttpNotFound : AppError()

    /** A local row disappeared (provider deleted while a screen was open). */
    data object NotFound : AppError()
    data object Parse : AppError()
    data object Cancelled : AppError()

    /** The agent has no workspace folder to work in yet. */
    data object NoWorkspace : AppError()

    /** The Android Keystore / encrypted storage could not be used. */
    data object SecureStorage : AppError()

    data class Unknown(val message: String?) : AppError()
}
