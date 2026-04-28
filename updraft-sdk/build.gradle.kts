import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.loco)
    alias(libs.plugins.maven.publish)
    id("base")
}

android {
    namespace = "com.appswithlove.updraft"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        targetSdk = 36
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_11
        sourceCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
            freeCompilerArgs.add("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(libs.appcompat)
    implementation(libs.material)

    implementation(libs.retrofit)
    implementation(libs.adapter.rxjava2)
    implementation(libs.logging.interceptor)

    implementation(libs.ink)

    implementation(libs.lifecycle.extensions)
    implementation(libs.lifecycle.runtime)
    annotationProcessor(libs.lifecycle.compiler)
    implementation(libs.androidx.startup)

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.converter.kotlinx.serialization)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}

Loco {
    config {
        apiKey = "sOLd6p87EQ9zFoOeI8ytGBvGhKNIFo4f"
        lang = listOf("en", "de")
        defLang = "en"
        resDir = "$projectDir/src/main/res"
        fallbackLang = "en"
        orderByAssetId = true
        hideComments = true
    }
}
