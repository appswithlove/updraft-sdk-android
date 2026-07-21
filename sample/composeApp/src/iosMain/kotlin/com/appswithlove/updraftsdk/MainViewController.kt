package com.appswithlove.updraftsdk

import androidx.compose.ui.window.ComposeUIViewController
import com.appswithlove.updraft.LogLevel
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.UpdraftSettings
import com.appswithlove.updraft.ui.ios.UpdraftIos
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { SampleApp() }

fun startUpdraft() {
    Updraft.start(
        UpdraftSettings(
            appKey = "",
            sdkKey = "",
            baseUrl = UpdraftSettings.BASE_URL_STAGING,
            logLevel = LogLevel.Debug,
            showFeedbackAlert = true,
        ),
    )
    UpdraftIos.autoWire()
}
