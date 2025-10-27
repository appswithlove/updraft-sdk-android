package com.appswithlove.updraft.manager

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.appswithlove.updraft.interactor.CheckFeedbackEnabledInteractor
import com.appswithlove.updraft.interactor.CheckFeedbackResultModel
import com.appswithlove.updraft.presentation.UpdraftSdkUi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class CheckFeedbackEnabledManager(
    private val updraftSdkUi: UpdraftSdkUi,
    private val checkFeedbackEnabledInteractor: CheckFeedbackEnabledInteractor
) : DefaultLifecycleObserver, ShakeDetectorManager.ShakeDetectorListener {

    private var checkFeedbackDisposable: Disposable? = null

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        runFeedbackCheck()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        checkFeedbackDisposable?.dispose()
    }

    private fun runFeedbackCheck() {
        checkFeedbackDisposable?.dispose()

        checkFeedbackDisposable = checkFeedbackEnabledInteractor.run()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ checkFeedbackResultModel ->
                if (!checkFeedbackResultModel.isFeedbackEnabled && updraftSdkUi.isCurrentlyShowingFeedback) {
                    updraftSdkUi.closeFeedback()
                    return@subscribe
                }

                if (checkFeedbackResultModel.showAlert) {
                    when (checkFeedbackResultModel.alertType) {
                        CheckFeedbackResultModel.ALERT_TYPE_FEEDBACK_DISABLED ->
                            updraftSdkUi.showFeedbackDisabledAlert()
                        CheckFeedbackResultModel.ALERT_TYPE_HOW_TO_GIVE_FEEDBACK ->
                            updraftSdkUi.showHowToGiveFeedbackAlert()
                    }
                }
            }, { throwable ->
                throwable.printStackTrace()
            })
    }

    override fun onShakeDetected() {
        updraftSdkUi.showFeedback()
        runFeedbackCheck()
    }
}
