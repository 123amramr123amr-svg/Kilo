package com.personalaiagent.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object Formatters {

    fun dateTime(millis: Long): String {
        val format = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        return format.format(Date(millis))
    }

    fun date(millis: Long): String {
        val format = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        return format.format(Date(millis))
    }

    fun duration(millis: Long): String = when {
        millis < 1000 -> "${millis}ms"
        millis < 60_000 -> String.format(Locale.getDefault(), "%.1fs", millis / 1000f)
        else -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - minutes * 60
            "${minutes}m ${seconds}s"
        }
    }

    fun tokens(tokenIn: Int?, tokenOut: Int?): String =
        "${tokenIn ?: "–"} / ${tokenOut ?: "–"}"

    fun compactNumber(value: Int?): String = when {
        value == null -> "—"
        value >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", value / 1_000_000f)
        value >= 1000 -> String.format(Locale.getDefault(), "%dK", value / 1000)
        else -> value.toString()
    }

    /** Human readable byte size, e.g. `1.4 KB`. */
    fun bytes(value: Long): String = when {
        value < 0 -> "—"
        value < 1024 -> "$value B"
        value < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", value / 1024f)
        value < 1024L * 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f MB", value / (1024f * 1024f))
        else -> String.format(Locale.getDefault(), "%.1f GB", value / (1024f * 1024f * 1024f))
    }

    fun initials(name: String): String = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "AI" }
}
