package com.personalaiagent.data.secure

import android.content.SharedPreferences

/**
 * Minimal key/value abstraction over private storage.
 *
 * Keeping this tiny interface separate from [android.content.SharedPreferences]
 * makes the credential store fully unit-testable without Android instrumentation.
 */
interface KeyValueStore {
    fun get(key: String): String?
    fun contains(key: String): Boolean
    /** Returns true when the value was durably written. */
    fun put(key: String, value: String): Boolean
    fun remove(key: String)
    fun clear()
}

class SharedPreferencesKeyValueStore(
    private val preferences: SharedPreferences,
) : KeyValueStore {

    override fun get(key: String): String? = preferences.getString(key, null)

    override fun contains(key: String): Boolean = preferences.contains(key)

    override fun put(key: String, value: String): Boolean =
        preferences.edit().putString(key, value).commit()

    override fun remove(key: String) {
        preferences.edit().remove(key).commit()
    }

    override fun clear() {
        preferences.edit().clear().commit()
    }
}
