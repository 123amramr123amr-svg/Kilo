package com.personalaiagent.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.personalaiagent.R
import com.personalaiagent.core.AppError

/** Localises every [AppError] case. */
@Composable
fun appErrorText(error: AppError): String = when (error) {
    AppError.NoProvider -> stringResource(R.string.error_no_provider)
    AppError.NoModel -> stringResource(R.string.error_no_model)
    is AppError.MissingKey -> stringResource(R.string.error_missing_key, error.providerName)
    is AppError.InvalidInput -> when (error.reason) {
        "name" -> stringResource(R.string.providers_error_name)
        "url" -> stringResource(R.string.providers_error_url)
        "insecure_url" -> stringResource(R.string.providers_error_url)
        "key" -> stringResource(R.string.providers_error_key)
        "empty" -> stringResource(R.string.error_empty_message)
        "malformed_backup", "unsupported_version" ->
            stringResource(R.string.backup_import_failed, error.reason)
        else -> stringResource(R.string.error_unknown)
    }
    AppError.Network -> stringResource(R.string.error_network)
    AppError.Timeout -> stringResource(R.string.error_timeout)
    AppError.Auth -> stringResource(R.string.error_auth)
    AppError.RateLimit -> stringResource(R.string.error_rate_limit)
    is AppError.Server -> stringResource(R.string.error_server, error.code)
    AppError.HttpNotFound -> stringResource(R.string.error_http_not_found)
    AppError.NotFound -> stringResource(R.string.error_not_found)
    AppError.Parse -> stringResource(R.string.error_parse)
    AppError.Cancelled -> stringResource(R.string.error_cancelled)
    AppError.NoWorkspace -> stringResource(R.string.error_no_workspace)
    AppError.SecureStorage -> stringResource(R.string.error_secure_storage)
    is AppError.Unknown -> when (error.message) {
        "agent_round_limit" -> stringResource(R.string.error_tool_limit)
        "workspace_permission_denied" -> stringResource(R.string.workspace_no_permission)
        "not_found" -> stringResource(R.string.state_empty)
        "attachment_failed", "unreadable", "unsupported_type" ->
            stringResource(R.string.attachment_failed)
        "attachment_empty" -> stringResource(R.string.attachment_empty)
        "app_no_launcher", "app_not_found" -> stringResource(R.string.error_unknown)
        // Show the underlying reason instead of hiding it behind "something
        // went wrong" — it is the only way a user can report or fix it.
        else -> error.message?.takeIf { it.isNotBlank() }
            ?.let { stringResource(R.string.error_unknown_detail, it) }
            ?: stringResource(R.string.error_unknown)
    }
}
