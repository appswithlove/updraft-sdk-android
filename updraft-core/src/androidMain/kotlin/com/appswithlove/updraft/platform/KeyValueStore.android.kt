package com.appswithlove.updraft.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

private class SharedPrefsKeyValueStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }
}

actual fun createKeyValueStore(name: String): KeyValueStore =
    SharedPrefsKeyValueStore(UpdraftContext.application.getSharedPreferences(name, Context.MODE_PRIVATE))
