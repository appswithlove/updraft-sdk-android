package com.apsswithlove.updraft_sdk.manager;

import android.app.Application;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;
import com.apsswithlove.updraft_sdk.interactor.CheckUpdateInteractor;
import com.apsswithlove.updraft_sdk.presentation.UpdraftSdkUi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by satori on 3/27/18.
 */

public class AppUpdateManager implements LifecycleObserver, UpdraftSdkUi.Listener {

    private CheckUpdateInteractor mCheckUpdateInteractor;
    private UpdraftSdkUi mUpdraftSdkUi;
    private Disposable mCheckUpdateDisposable;

    public AppUpdateManager(Application application,
                            CheckUpdateInteractor checkUpdateInteractor,
                            UpdraftSdkUi updraftSdkUi) {
        mCheckUpdateInteractor = checkUpdateInteractor;
        mUpdraftSdkUi = updraftSdkUi;
    }

    public void start() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        mUpdraftSdkUi.showStartAlert();
        mUpdraftSdkUi.setListener(this);
        if (mCheckUpdateDisposable != null) {
            mCheckUpdateDisposable.dispose();
        }
        mCheckUpdateDisposable = mCheckUpdateInteractor
                .checkUpdate()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        checkUpdateResultModel -> {
                            if (checkUpdateResultModel.isShowAlert()) {
                                mUpdraftSdkUi.showAlert(checkUpdateResultModel.getUrl());
                            }
                        },
                        Throwable::printStackTrace
                );
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        mUpdraftSdkUi.setListener(null);
        if (mCheckUpdateDisposable != null) {
            mCheckUpdateDisposable.dispose();
        }
    }

    @Override
    public void onOkClicked(String url) {
        mUpdraftSdkUi.openUrl(url);
    }
}
