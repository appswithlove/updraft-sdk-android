buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.coveralls)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.updraft) apply false
    alias(libs.plugins.loco) apply false
    id("signing")
}
