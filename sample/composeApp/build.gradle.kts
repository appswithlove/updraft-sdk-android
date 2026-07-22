import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.updraft)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val generateSampleKeys = tasks.register("generateSampleKeys") {
    val appKeyAndroid = localProperties.getProperty("updraft.appKey.android") ?: ""
    val appKeyIos = localProperties.getProperty("updraft.appKey.ios") ?: ""
    val sdkKey = localProperties.getProperty("updraft.sdkKey") ?: ""
    val outputDir = layout.buildDirectory.dir("generated/sampleKeys/kotlin")
    inputs.property("appKeyAndroid", appKeyAndroid)
    inputs.property("appKeyIos", appKeyIos)
    inputs.property("sdkKey", sdkKey)
    outputs.dir(outputDir)
    doLast {
        val packageDir = outputDir.get().asFile.resolve("com/appswithlove/updraftsdk")
        packageDir.mkdirs()
        packageDir.resolve("SampleKeys.kt").writeText(
            """
            package com.appswithlove.updraftsdk

            object SampleKeys {
                const val APP_KEY_ANDROID = "$appKeyAndroid"
                const val APP_KEY_IOS = "$appKeyIos"
                const val SDK_KEY = "$sdkKey"
            }
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "SampleApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateSampleKeys)
        }
        commonMain.dependencies {
            implementation(project(":updraft-core"))
            implementation(project(":updraft-ui-compose"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        androidMain.dependencies {
            implementation(project(":updraft-sdk"))
            implementation(libs.androidx.activity.compose)
        }
    }
}

android {
    namespace = "com.appswithlove.updraftsdk"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.appswithlove.updraftsdk"
        minSdk = 23
        targetSdk = 36
        versionCode = 5
        versionName = "1.4"
    }

    signingConfigs {
        create("release") {
            storeFile = file("updraft_test.jks")
            storePassword = "appswithlove"
            keyAlias = "release"
            keyPassword = "appswithlove"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

val updraftUploadUrl: String = findProperty("updraft_uploadUrl") as? String ?: ""
updraft {
    urls = mapOf("Release" to listOf(updraftUploadUrl))
}
