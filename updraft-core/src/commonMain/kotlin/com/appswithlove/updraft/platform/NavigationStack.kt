package com.appswithlove.updraft.platform

/**
 * Snapshot of the host app's current navigation stack as comma-separated
 * screen names, excluding Updraft's own screens. Empty when unavailable.
 */
expect fun currentNavigationStack(): String
