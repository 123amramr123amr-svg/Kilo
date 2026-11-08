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
    AppError.Parse -> stringResource(R.string.error_parse)
    AppError.Cancelled -> stringResource(R.string.error_cancelled)
    AppError.NoWorkspace -> stringResource(R.string.error_no_workspace)
    AppError.SecureStorage -> stringResource(R.string.error_secure_storage)
    is AppError.Unknown -> when (error.message) {
        "agent_round_limit" -> stringResource(R.string.error_tool_limit)
        "workspace_permission_denied" -> stringResource(R.string.workspace_no_permission)
        "not_found" -> stringResource(R.string.state_empty)
        else -> stringResource(R.string.error_unknown)
    }
}
