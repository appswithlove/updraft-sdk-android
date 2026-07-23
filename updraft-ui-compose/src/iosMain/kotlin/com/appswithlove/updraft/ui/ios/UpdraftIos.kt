package com.appswithlove.updraft.ui.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.UpdraftEvent
import com.appswithlove.updraft.ui.dialogs.FeedbackDisabledDialog
import com.appswithlove.updraft.ui.dialogs.FeedbackHintDialog
import com.appswithlove.updraft.ui.dialogs.UpdateAvailableDialog
import com.appswithlove.updraft.ui.feedback.FeedbackScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UIModalPresentationOverFullScreen
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

fun UpdraftFeedbackViewController(screenshotPng: ByteArray?, onClose: () -> Unit): UIViewController =
    ComposeUIViewController {
        FeedbackScreen(screenshotPng = screenshotPng, onClose = onClose)
    }

private fun dialogViewController(event: UpdraftEvent, onDismissed: () -> Unit): UIViewController {
    val viewController = ComposeUIViewController {
        when (event) {
            is UpdraftEvent.UpdateAvailable -> UpdateAvailableDialog(
                event = event,
                onOpen = { url -> Updraft.openUpdateUrl(url); onDismissed() },
                onLater = onDismissed,
            )
            UpdraftEvent.ShowFeedbackHint -> FeedbackHintDialog(onDismiss = onDismissed)
            UpdraftEvent.FeedbackDisabled -> FeedbackDisabledDialog(onDismiss = onDismissed)
            UpdraftEvent.FeedbackRequested, UpdraftEvent.CloseFeedback, is UpdraftEvent.Error -> Unit
        }
    }
    viewController.view.backgroundColor = UIColor.clearColor
    viewController.modalPresentationStyle = UIModalPresentationOverFullScreen
    return viewController
}

private fun keyWindow(): UIWindow? =
    UIApplication.sharedApplication.windows
        .filterIsInstance<UIWindow>()
        .firstOrNull { it.isKeyWindow() }

private fun topmostViewController(): UIViewController? {
    var top = keyWindow()?.rootViewController
    while (top?.presentedViewController != null) {
        top = top?.presentedViewController
    }
    return top
}

object UpdraftIos {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var started = false

    fun autoWire() {
        if (started) return
        started = true
        scope.launch {
            // Wait until the host app calls Updraft.start()
            while (!runCatching { Updraft.settings }.isSuccess) delay(100)
            Updraft.events.collect { event ->
                when (event) {
                    is UpdraftEvent.UpdateAvailable,
                    UpdraftEvent.ShowFeedbackHint,
                    UpdraftEvent.FeedbackDisabled,
                    -> presentDialog(event)

                    UpdraftEvent.FeedbackRequested -> presentFeedback()

                    UpdraftEvent.CloseFeedback, is UpdraftEvent.Error -> Unit
                }
            }
        }
    }

    private fun presentDialog(event: UpdraftEvent) {
        val presenter = topmostViewController() ?: return
        lateinit var dialogVc: UIViewController
        dialogVc = dialogViewController(event) {
            dialogVc.dismissViewControllerAnimated(true, completion = null)
        }
        presenter.presentViewController(dialogVc, animated = true, completion = null)
    }

    private fun presentFeedback() {
        val presenter = topmostViewController() ?: return
        val screenshot = Updraft.takePendingScreenshot()
        lateinit var feedbackVc: UIViewController
        feedbackVc = UpdraftFeedbackViewController(screenshot) {
            feedbackVc.dismissViewControllerAnimated(true, completion = null)
            Updraft.onFeedbackUiClosed()
        }
        feedbackVc.modalPresentationStyle = UIModalPresentationFullScreen
        presenter.presentViewController(feedbackVc, animated = true, completion = null)
    }
}
