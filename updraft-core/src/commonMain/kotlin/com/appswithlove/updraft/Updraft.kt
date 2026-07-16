package com.appswithlove.updraft

import com.appswithlove.updraft.api.UpdraftApi
import com.appswithlove.updraft.api.UpdraftApiContract
import com.appswithlove.updraft.interactor.CheckFeedbackEnabledInteractor
import com.appswithlove.updraft.interactor.CheckFeedbackResultModel
import com.appswithlove.updraft.interactor.CheckUpdateInteractor
import com.appswithlove.updraft.platform.KeyValueStore
import com.appswithlove.updraft.platform.ShakeDetector
import com.appswithlove.updraft.platform.createAppForegroundObserver
import com.appswithlove.updraft.platform.createKeyValueStore
import com.appswithlove.updraft.platform.createScreenshotGrabber
import com.appswithlove.updraft.platform.createShakeDetector
import com.appswithlove.updraft.platform.currentAppInfo
import com.appswithlove.updraft.platform.openUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

internal class UpdraftController(
    private val settings: UpdraftSettings,
    private val api: UpdraftApiContract,
    store: KeyValueStore,
    private val scope: CoroutineScope,
) {
    private val checkUpdateInteractor = CheckUpdateInteractor(api)
    private val checkFeedbackInteractor = CheckFeedbackEnabledInteractor(api, store)

    private val _events = MutableSharedFlow<UpdraftEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<UpdraftEvent> = _events

    private var updateAlertShown = false
    private var pendingScreenshot: ByteArray? = null

    var feedbackUiPresenter: FeedbackUiPresenter? = null

    fun onForeground() {
        checkForUpdate()
        checkFeedbackEnabled()
    }

    fun checkForUpdate() {
        scope.launch {
            try {
                val result = checkUpdateInteractor.checkUpdate()
                val url = result.url
                if (result.showAlert && url != null && !updateAlertShown) {
                    updateAlertShown = true
                    _events.tryEmit(
                        UpdraftEvent.UpdateAvailable(url, result.version, result.yourVersion, result.createAt),
                    )
                }
            } catch (t: Throwable) {
                _events.tryEmit(UpdraftEvent.Error(t))
            }
        }
    }

    private fun checkFeedbackEnabled() {
        scope.launch {
            try {
                val result = checkFeedbackInteractor.run()
                if (result.showAlert) {
                    when (result.alertType) {
                        CheckFeedbackResultModel.AlertType.FeedbackDisabled ->
                            _events.tryEmit(UpdraftEvent.FeedbackDisabled)
                        CheckFeedbackResultModel.AlertType.HowToGiveFeedback ->
                            if (settings.showFeedbackAlert) _events.tryEmit(UpdraftEvent.ShowFeedbackHint)
                    }
                }
                if (!result.isFeedbackEnabled) {
                    _events.tryEmit(UpdraftEvent.CloseFeedback)
                }
            } catch (t: Throwable) {
                _events.tryEmit(UpdraftEvent.Error(t))
            }
        }
    }

    fun onFeedbackTriggered(screenshotPng: ByteArray?) {
        pendingScreenshot = screenshotPng
        val presenter = feedbackUiPresenter
        if (presenter != null) {
            presenter.presentFeedback(screenshotPng)
        } else {
            _events.tryEmit(UpdraftEvent.FeedbackRequested)
        }
    }

    fun takePendingScreenshot(): ByteArray? {
        val screenshot = pendingScreenshot
        pendingScreenshot = null
        return screenshot
    }

    fun sendFeedback(screenshotPng: ByteArray, type: FeedbackType, description: String, email: String): Flow<Double> =
        api.sendFeedback(screenshotPng, type, description, email)
}

object Updraft {
    private var controller: UpdraftController? = null
    private var currentSettings: UpdraftSettings? = null
    private var shakeDetector: ShakeDetector? = null

    val settings: UpdraftSettings
        get() = checkNotNull(currentSettings) { "Must call Updraft.start() first" }

    val events: SharedFlow<UpdraftEvent>
        get() = requireController().events

    fun start(settings: UpdraftSettings) {
        if (controller != null) return
        currentSettings = settings
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val api = UpdraftApi(settings, currentAppInfo())
        val store = createKeyValueStore(CheckFeedbackEnabledInteractor.STORE_NAME)
        val newController = UpdraftController(settings, api, store, scope)
        controller = newController

        if (settings.feedbackEnabled) {
            val detector = createShakeDetector { showFeedback() }
            shakeDetector = detector
            detector.start()
        }
        createAppForegroundObserver(
            onForeground = { newController.onForeground() },
            onBackground = { },
        ).start()
    }

    fun checkForUpdate() = requireController().checkForUpdate()

    fun showFeedback() {
        val screenshot = createScreenshotGrabber().capturePng()
        requireController().onFeedbackTriggered(screenshot)
    }

    fun sendFeedback(screenshotPng: ByteArray, type: FeedbackType, description: String, email: String): Flow<Double> =
        requireController().sendFeedback(screenshotPng, type, description, email)

    fun openUpdateUrl(url: String) = openUrl(url)

    fun setFeedbackUiPresenter(presenter: FeedbackUiPresenter?) {
        requireController().feedbackUiPresenter = presenter
    }

    fun onFeedbackUiClosed() {
        shakeDetector?.setEnabled(true)
    }

    internal fun takePendingScreenshot(): ByteArray? = requireController().takePendingScreenshot()

    private fun requireController(): UpdraftController =
        checkNotNull(controller) { "Must call Updraft.start() first" }
}
