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
    data object RateLimit : AppError()
    data class Server(val code: Int) : AppError()
    data object Parse : AppError()
    data object Cancelled : AppError()

    /** The agent has no workspace folder to work in yet. */
    data object NoWorkspace : AppError()

    /** The Android Keystore / encrypted storage could not be used. */
    data object SecureStorage : AppError()

    data class Unknown(val message: String?) : AppError()
}
