package com.appswithlove.updraft.platform

interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

expect fun createKeyValueStore(name: String): KeyValueStore
