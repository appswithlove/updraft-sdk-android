package com.appswithlove.updraft.manager

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.appswithlove.updraft.interactor.CheckUpdateInteractor
import com.appswithlove.updraft.interactor.CheckUpdateResultModel
import com.appswithlove.updraft.presentation.UpdraftSdkUi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by satori on 3/27/18.
 */
class AppUpdateManager(
    private val mCheckUpdateInteractor: CheckUpdateInteractor,
    private val mUpdraftSdkUi: UpdraftSdkUi
) : LifecycleObserver, UpdraftSdkUi.Listener {
    private var mCheckUpdateDisposable: Disposable? = null
    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        mUpdraftSdkUi.showFeedbackAlert()
        mUpdraftSdkUi.setListener(this)
        if (mCheckUpdateDisposable != null) {
            mCheckUpdateDisposable!!.dispose()
        }
        mCheckUpdateDisposable = mCheckUpdateInteractor
            .checkUpdate()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { checkUpdateResultModel: CheckUpdateResultModel ->
                    if (checkUpdateResultModel.isShowAlert) {
                        mUpdraftSdkUi.showUpdateAvailableAlert(checkUpdateResultModel.url)
                    }
                }) { obj: Throwable -> obj.printStackTrace() }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        mUpdraftSdkUi.setListener(null)
        if (mCheckUpdateDisposable != null) {
            mCheckUpdateDisposable!!.dispose()
        }
    }

    override fun onOkClicked(url: String) {
        mUpdraftSdkUi.openUrl(url)
    }
}