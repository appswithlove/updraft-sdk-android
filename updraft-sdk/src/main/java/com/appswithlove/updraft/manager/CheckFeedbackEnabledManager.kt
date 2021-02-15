package com.appswithlove.updraft.manager

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.appswithlove.updraft.interactor.CheckFeedbackEnabledInteractor
import com.appswithlove.updraft.interactor.CheckFeedbackResultModel
import com.appswithlove.updraft.manager.ShakeDetectorManager.ShakeDetectorListener
import com.appswithlove.updraft.presentation.UpdraftSdkUi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class CheckFeedbackEnabledManager(
    private val mUpdraftSdkUi: UpdraftSdkUi,
    private val mCheckFeedbackEnabledInteractor: CheckFeedbackEnabledInteractor
) : LifecycleObserver, ShakeDetectorListener {
    private var mCheckFeedbackDisposable: Disposable? = null
    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        runFeedbackCheck()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        if (mCheckFeedbackDisposable != null) {
            mCheckFeedbackDisposable!!.dispose()
        }
    }

    private fun runFeedbackCheck() {
        if (mCheckFeedbackDisposable != null) {
            mCheckFeedbackDisposable!!.dispose()
        }
        mCheckFeedbackDisposable = mCheckFeedbackEnabledInteractor
            .run()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { checkFeedbackResultModel: CheckFeedbackResultModel ->
                    if (!checkFeedbackResultModel.isFeedbackEnabled && mUpdraftSdkUi.isCurrentlyShowingFeedback) {
                        mUpdraftSdkUi.closeFeedback()
                        return@subscribe
                    }
                    if (checkFeedbackResultModel.isShowAlert) {
                        if (checkFeedbackResultModel.alertType == CheckFeedbackResultModel.ALERT_TYPE_FEEDBACK_DISABLED) {
                            mUpdraftSdkUi.showFeedbackDisabledAlert()
                        }
                        if (checkFeedbackResultModel.alertType == CheckFeedbackResultModel.ALERT_TYPE_HOW_TO_GIVE_FEEDBACK) {
                            mUpdraftSdkUi.showHowToGiveFeedbackAlert()
                        }
                    }
                }) { obj: Throwable -> obj.printStackTrace() }
    }

    override fun onShakeDetected() {
        mUpdraftSdkUi.showFeedback()
        runFeedbackCheck()
    }
}
