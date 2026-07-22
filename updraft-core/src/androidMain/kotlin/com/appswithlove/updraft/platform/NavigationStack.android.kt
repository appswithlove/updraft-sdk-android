package com.appswithlove.updraft.platform

actual fun currentNavigationStack(): String =
    CurrentActivityManager.stack
        .filterNot { it.javaClass.name.startsWith("com.appswithlove.updraft") }
        .joinToString(", ") { it.javaClass.simpleName }
