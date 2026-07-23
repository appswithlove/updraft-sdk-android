package com.appswithlove.updraft.platform

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosSmokeTest {
    @Test
    fun keyValueStore_roundTrips() {
        val store = createKeyValueStore("updraft_test_suite")
        store.putBoolean("k", true)
        assertTrue(store.getBoolean("k", false))
    }

    @Test
    fun appInfo_populates() {
        val info = currentAppInfo()
        assertNotNull(info.systemVersion)
    }
}
