package com.appswithlove.updraft.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShakeMathTest {
    private val g = 9.80665f

    @Test
    fun restingPhone_noShake() {
        assertFalse(isShakeGForce(0f, 0f, g, g))
    }

    @Test
    fun hardShake_detected() {
        assertTrue(isShakeGForce(3f * g, 0f, 0f, g))
    }
}
