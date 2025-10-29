package com.appswithlove.updraft.manager

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.appswithlove.updraft.interactor.CheckFeedbackEnabledInteractor
import com.appswithlove.updraft.interactor.CheckFeedbackResultModel
import com.appswithlove.updraft.presentation.UpdraftSdkUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CheckFeedbackEnabledManager(
    private val updraftSdkUi: UpdraftSdkUi,
    private val checkFeedbackEnabledInteractor: CheckFeedbackEnabledInteractor
) : DefaultLifecycleObserver, ShakeDetectorManager.ShakeDetectorListener {

    private var checkFeedbackJob: Job? = null

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        runFeedbackCheck(owner)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        checkFeedbackJob?.cancel()
    }

    private fun runFeedbackCheck(owner: LifecycleOwner) {
        checkFeedbackJob?.cancel()

        checkFeedbackJob = owner.lifecycleScope.launch {
            try {
                val checkFeedbackResultModel = checkFeedbackEnabledInteractor.run()

                if (!checkFeedbackResultModel.isFeedbackEnabled && updraftSdkUi.isCurrentlyShowingFeedback) {
                    updraftSdkUi.closeFeedback()
                    return@launch
                }

                if (checkFeedbackResultModel.showAlert) {
                    when (checkFeedbackResultModel.alertType) {
                        CheckFeedbackResultModel.ALERT_TYPE_FEEDBACK_DISABLED ->
                            updraftSdkUi.showFeedbackDisabledAlert()

                        CheckFeedbackResultModel.ALERT_TYPE_HOW_TO_GIVE_FEEDBACK ->
                            updraftSdkUi.showHowToGiveFeedbackAlert()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onShakeDetected() {
        updraftSdkUi.showFeedback()

        // Run feedback check again in a global lifecycle scope since Shake might happen outside lifecycle event
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            try {
                val checkFeedbackResultModel = checkFeedbackEnabledInteractor.run()

                if (!checkFeedbackResultModel.isFeedbackEnabled && updraftSdkUi.isCurrentlyShowingFeedback) {
                    updraftSdkUi.closeFeedback()
                    return@launch
                }

                if (checkFeedbackResultModel.showAlert) {
                    when (checkFeedbackResultModel.alertType) {
                        CheckFeedbackResultModel.ALERT_TYPE_FEEDBACK_DISABLED ->
                            updraftSdkUi.showFeedbackDisabledAlert()

                        CheckFeedbackResultModel.ALERT_TYPE_HOW_TO_GIVE_FEEDBACK ->
                            updraftSdkUi.showHowToGiveFeedbackAlert()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
