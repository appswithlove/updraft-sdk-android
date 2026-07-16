package com.appswithlove.updraft.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.ui.feedback.FeedbackScreen

class UpdraftFeedbackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screenshot = pendingScreenshot
        pendingScreenshot = null
        setContent {
            FeedbackScreen(
                screenshotPng = screenshot,
                onClose = { finish() },
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Updraft.onFeedbackUiClosed()
    }

    companion object {
        private var pendingScreenshot: ByteArray? = null

        fun launch(context: Context, screenshotPng: ByteArray?) {
            pendingScreenshot = screenshotPng
            context.startActivity(
                Intent(context, UpdraftFeedbackActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            (context as? Activity)?.overridePendingTransition(0, 0)
        }
    }
}
