package com.personalaiagent.domain.repository

import com.personalaiagent.core.Outcome

/** A launchable app on the phone, as seen by the agent. */
data class AppInfo(val label: String, val packageName: String)

/**
 * Deliberately narrow device automation: the agent may *open* apps and URLs and
 * start a web search, but it never taps inside another app's UI.
 */
interface DeviceControl {
    /** Apps with a launcher entry. Empty when the platform refuses the query. */
    suspend fun launchableApps(limit: Int = 60): List<AppInfo>

    /** Opens the best match for [query] (label or package) or returns the candidates. */
    suspend fun openApp(query: String): Outcome<String>

    suspend fun openUrl(url: String): Outcome<String>

    suspend fun webSearch(query: String): Outcome<String>

    suspend fun shareText(text: String): Outcome<String>
}
