package com.personalaiagent.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.personalaiagent.core.Outcome
import com.personalaiagent.domain.repository.AppInfo
import com.personalaiagent.domain.repository.DeviceControl
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens apps, links and searches with standard Android intents.
 *
 * Only *cold-start* actions are used (`ACTION_MAIN`, `ACTION_VIEW`,
 * `ACTION_WEB_SEARCH`). The app never injects touches into another app; that
 * would require an accessibility service, which is intentionally not shipped.
 */
@Singleton
class AndroidDeviceControl @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceControl {

    override suspend fun launchableApps(limit: Int): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching {
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }.getOrDefault(emptyList())

        return resolved
            .mapNotNull { info ->
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = runCatching { info.loadLabel(context.packageManager).toString() }
                    .getOrDefault(packageName)
                AppInfo(label = label, packageName = packageName)
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .take(limit)
    }

    override suspend fun openApp(query: String): Outcome<String> {
        val needle = query.trim()
        if (needle.isEmpty()) return Outcome.Failure(com.personalaiagent.core.AppError.InvalidInput("app"))
        val apps = launchableApps(limit = 200)

        val match = apps.firstOrNull { it.packageName.equals(needle, ignoreCase = true) }
            ?: apps.firstOrNull { it.label.equals(needle, ignoreCase = true) }
            ?: apps.firstOrNull { it.label.contains(needle, ignoreCase = true) }
            ?: apps.firstOrNull { it.packageName.contains(needle.lowercase()) }

        if (match == null) {
            val visible = apps.take(12).joinToString(", ") { it.label }
            return Outcome.Failure(
                com.personalaiagent.core.AppError.Unknown("app_not_found:$visible"),
            )
        }

        val launch = context.packageManager.getLaunchIntentForPackage(match.packageName)
            ?: return Outcome.Failure(com.personalaiagent.core.AppError.Unknown("app_no_launcher"))
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launch)
            Outcome.Success("Opened ${match.label} (${match.packageName}).")
        } catch (e: Exception) {
            Outcome.Failure(com.personalaiagent.core.AppError.Unknown(e.message))
        }
    }

    override suspend fun openUrl(url: String): Outcome<String> {
        val normalized = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("www.") -> "https://$url"
            else -> "https://$url"
        }
        return startView(Uri.parse(normalized)).let { error ->
            if (error == null) Outcome.Success("Opened $normalized.") else Outcome.Failure(error)
        }
    }

    override suspend fun webSearch(query: String): Outcome<String> {
        val text = query.trim()
        if (text.isEmpty()) return Outcome.Failure(com.personalaiagent.core.AppError.InvalidInput("query"))
        // ACTION_WEB_SEARCH opens the user's default browser/assistant with the query;
        // fall back to a plain URL when no app handles it.
        val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", text)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val started = runCatching { context.startActivity(intent) }.isSuccess
        if (started) return Outcome.Success("Searched for \"$text\".")
        val encoded = Uri.encode(text)
        return startView(Uri.parse("https://www.google.com/search?q=$encoded")).let { error ->
            if (error == null) Outcome.Success("Searched for \"$text\".") else Outcome.Failure(error)
        }
    }

    override suspend fun shareText(text: String): Outcome<String> {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Outcome.Success("Opened the share sheet.")
        } catch (e: Exception) {
            Outcome.Failure(com.personalaiagent.core.AppError.Unknown(e.message))
        }
    }

    private fun startView(uri: Uri): com.personalaiagent.core.AppError? = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        null
    } catch (e: Exception) {
        com.personalaiagent.core.AppError.Unknown(e.message)
    }
}
