package com.appswithlove.updraft;

import android.app.Application;
import com.appswithlove.updraft.api.ApiWrapper;
import com.appswithlove.updraft.interactor.CheckFeedbackEnabledInteractor;
import com.appswithlove.updraft.interactor.CheckUpdateInteractor;
import com.appswithlove.updraft.manager.AppUpdateManager;
import com.appswithlove.updraft.manager.CheckFeedbackEnabledManager;
import com.appswithlove.updraft.manager.ShakeDetectorManager;
import com.appswithlove.updraft.presentation.UpdraftSdkUi;

/**
 * Created by satori on 3/27/18.
 */

public class Updraft {

    public static final String UPDRAFT_TAG = "updraft";

    private static final String NOT_INITIALIZED_MESSAGE = "Must initialize Updraft before using getInstance()";

    private static Updraft instance;
    private AppUpdateManager mAppUpdateManager;
    private ShakeDetectorManager mShakeDetectorManager;
    private CheckFeedbackEnabledManager mCheckFeedbackEnabledManager;
    private ApiWrapper mApiWrapper;

    private Updraft(Application application, Settings settings) {
        mApiWrapper = new ApiWrapper(application, settings);
        CheckUpdateInteractor checkUpdateInteractor = new CheckUpdateInteractor(mApiWrapper);
        UpdraftSdkUi updraftSdkUi = new UpdraftSdkUi(application, settings);
        mAppUpdateManager = new AppUpdateManager(application, checkUpdateInteractor, updraftSdkUi);
        CheckFeedbackEnabledInteractor checkFeedbackEnabledInteractor = new CheckFeedbackEnabledInteractor(mApiWrapper, application);

        mCheckFeedbackEnabledManager = new CheckFeedbackEnabledManager(updraftSdkUi, checkFeedbackEnabledInteractor);

        mShakeDetectorManager = new ShakeDetectorManager(application, updraftSdkUi, settings, mCheckFeedbackEnabledManager);
    }

    public static void initialize(Application application, Settings settings) {
        createUpdraft(application, settings);
    }

    private static synchronized void createUpdraft(Application application, Settings settings) {
        if (instance == null) {
            instance = new Updraft(application, settings);
        }

    }

    private static void checkInitialized() {
        if (instance == null) {
            throw new IllegalStateException(NOT_INITIALIZED_MESSAGE);
        }
    }

    public static Updraft getInstance() {
        checkInitialized();
        return instance;
    }

    public void start() {
        mAppUpdateManager.start();
        mShakeDetectorManager.start();
        mCheckFeedbackEnabledManager.start();
    }

    public ApiWrapper getApiWrapper() {
        return mApiWrapper;
    }

    public ShakeDetectorManager getShakeDetectorManager() {
        return mShakeDetectorManager;
    }
}
