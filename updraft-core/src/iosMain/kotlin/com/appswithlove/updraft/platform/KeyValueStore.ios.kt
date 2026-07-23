package com.appswithlove.updraft.platform

import platform.Foundation.NSUserDefaults

private class UserDefaultsKeyValueStore(private val defaults: NSUserDefaults) : KeyValueStore {
    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else default

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
    }
}

actual fun createKeyValueStore(name: String): KeyValueStore =
    UserDefaultsKeyValueStore(NSUserDefaults(suiteName = name))
