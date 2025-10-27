package com.appswithlove.updraft.feedback.form

import android.os.Handler
import android.os.Looper
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.api.ApiWrapper
import com.appswithlove.updraft.feedback.FeedbackActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class FeedbackFormPresenter {

    private var view: FeedbackFormContract.View? = null
    private var compositeDisposable: CompositeDisposable? = null
    private val apiWrapper: ApiWrapper? = Updraft.getInstance()?.apiWrapper
    private var sendFeedbackDisposable: Disposable? = null

    fun attachView(view: FeedbackFormContract.View) {
        this.view = view
        compositeDisposable = CompositeDisposable()
    }

    fun detachView() {
        compositeDisposable?.dispose()
        view = null
    }

    fun onSendButtonClicked() {
        val currentView = view ?: return

        currentView.showProgress()

        val disposable = apiWrapper?.sendMobileFeedback(
            currentView.getSelectedChoice(),
            currentView.getDescription(),
            currentView.getEmail(),
            FeedbackActivity.SAVED_SCREENSHOT
        )
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(
                { progress -> currentView.updateProgress(progress) },
                { t ->
                    t.printStackTrace()
                    currentView.showErrorMessage(t)
                },
                {
                    currentView.showSuccessMessage()
                    closeAfterDelay()
                }
            )

        disposable?.let { d ->
            compositeDisposable?.add(d)
            sendFeedbackDisposable = d
        }
    }

    fun onProgressCancelClicked() {
        sendFeedbackDisposable?.takeIf { !it.isDisposed }?.dispose()
        view?.hideProgress()
    }

    private fun closeAfterDelay() {
        Handler(Looper.getMainLooper()).postDelayed(
            { view?.closeFeedback() },
            1000
        )
    }
}
