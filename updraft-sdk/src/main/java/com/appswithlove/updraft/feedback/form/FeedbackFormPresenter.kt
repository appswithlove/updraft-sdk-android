package com.appswithlove.updraft.feedback.form

import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.api.ApiWrapper
import com.appswithlove.updraft.feedback.FeedbackActivity
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class FeedbackFormPresenter : CoroutineScope {

    private var view: FeedbackFormContract.View? = null
    private val apiWrapper: ApiWrapper? = Updraft.getInstance()?.apiWrapper

    private var presenterJob: Job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + presenterJob

    private var sendJob: Job? = null

    fun attachView(view: FeedbackFormContract.View) {
        this.view = view
    }

    fun detachView() {
        cancel()
        view = null
    }

    fun onSendButtonClicked() {
        val currentView = view ?: return
        val api = apiWrapper ?: return

        currentView.showProgress()

        sendJob?.cancel()
        sendJob = launch {
            try {
                withContext(Dispatchers.IO) {
                    api.sendMobileFeedback(
                        currentView.getSelectedChoice(),
                        currentView.getDescription(),
                        currentView.getEmail(),
                        FeedbackActivity.SAVED_SCREENSHOT
                    ).collect { progress ->
                        withContext(Dispatchers.Main) {
                            currentView.updateProgress(progress)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    currentView.showSuccessMessage()
                    closeAfterDelay()
                }
            } catch (_: CancellationException) {
                // Job was cancelled, ignore
            } catch (t: Throwable) {
                t.printStackTrace()
                withContext(Dispatchers.Main) {
                    currentView.showErrorMessage(t)
                }
            }
        }
    }

    fun onProgressCancelClicked() {
        sendJob?.cancel()
        view?.hideProgress()
    }

    private fun closeAfterDelay() {
        launch {
            delay(1000)
            view?.closeFeedback()
        }
    }
}
