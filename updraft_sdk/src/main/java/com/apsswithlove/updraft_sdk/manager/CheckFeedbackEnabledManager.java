package com.apsswithlove.updraft_sdk.manager;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;
import com.apsswithlove.updraft_sdk.interactor.CheckFeedbackEnabledInteractor;
import com.apsswithlove.updraft_sdk.interactor.CheckFeedbackResultModel;
import com.apsswithlove.updraft_sdk.presentation.UpdraftSdkUi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CheckFeedbackEnabledManager implements LifecycleObserver, ShakeDetectorManager.ShakeDetectorListener {

    private UpdraftSdkUi mUpdraftSdkUi;
    private CheckFeedbackEnabledInteractor mCheckFeedbackEnabledInteractor;

    private Disposable mCheckFeedbackDisposable;

    public CheckFeedbackEnabledManager(UpdraftSdkUi updraftSdkUi,
                                       CheckFeedbackEnabledInteractor checkFeedbackEnabledInteractor) {
        mUpdraftSdkUi = updraftSdkUi;
        mCheckFeedbackEnabledInteractor = checkFeedbackEnabledInteractor;
    }

    public void start() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        runFeedbackCheck();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        if (mCheckFeedbackDisposable != null) {
            mCheckFeedbackDisposable.dispose();
        }
    }

    private void runFeedbackCheck() {
        if (mCheckFeedbackDisposable != null) {
            mCheckFeedbackDisposable.dispose();
        }
        mCheckFeedbackDisposable = mCheckFeedbackEnabledInteractor
                .run()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        checkFeedbackResultModel -> {
                            if (!checkFeedbackResultModel.isFeedbackEnabled() && mUpdraftSdkUi.isCurrentlyShowingFeedback()) {
                                mUpdraftSdkUi.closeFeedback();
                                return;
                            }
                            if (checkFeedbackResultModel.isShowAlert()) {
                                if (checkFeedbackResultModel.getAlertType() == CheckFeedbackResultModel.ALERT_TYPE_FEEDBACK_DISABLED) {
                                    mUpdraftSdkUi.showFeedbackDisabledAlert();
                                }
                                if (checkFeedbackResultModel.getAlertType() == CheckFeedbackResultModel.ALERT_TYPE_HOW_TO_GIVE_FEEDBACK) {
                                    mUpdraftSdkUi.showHowToGiveFeedbackAlert();
                                }
                            }
                        }
                );
    }

    @Override
    public void onShakeDetected() {
        mUpdraftSdkUi.showFeedback();
        runFeedbackCheck();
    }
}