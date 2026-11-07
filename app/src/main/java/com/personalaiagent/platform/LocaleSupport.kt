package com.personalaiagent.platform

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.text.TextUtils
import android.view.View
import java.util.Locale

/** Synchronous mirror of the UI language, readable from `attachBaseContext`. */
interface LocaleMirror {
    fun read(): String
    fun write(tag: String)
    fun clear()
}

class PrefsLocaleMirror(private val preferences: SharedPreferences) : LocaleMirror {

    override fun read(): String = preferences.getString(KEY, DEFAULT_TAG) ?: DEFAULT_TAG

    override fun write(tag: String) {
        preferences.edit().putString(KEY, tag).apply()
    }

    override fun clear() {
        preferences.edit().remove(KEY).apply()
    }

    companion object {
        const val PREFS_NAME = "pai_ui"
        const val DEFAULT_TAG = "en"
        private const val KEY = "language_tag"

        fun preferences(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}

object AppLocale {

    /** Returns a context whose resources are bound to [tag] with the right direction. */
    fun wrap(base: Context, tag: String): Context {
        val locale = Locale.forLanguageTag(tag.ifBlank { PrefsLocaleMirror.DEFAULT_TAG })
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(configuration)
    }

    fun isRtl(tag: String): Boolean {
        val locale = Locale.forLanguageTag(tag.ifBlank { PrefsLocaleMirror.DEFAULT_TAG })
        return TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
    }
}
