package com.apsswithlove.updraftsdk;

import android.app.Application;
import com.apsswithlove.updraft_sdk.Settings;
import com.apsswithlove.updraft_sdk.Updraft;

/**
 * Created by satori on 3/27/18.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Settings settings = new Settings();
        settings.setAppKey("7ae57391493b44bdbe6c5d18cd6f8fef");
        settings.setSdkKey("30cf32ded8f64bbfb0bb4db0bc719316 ");
        settings.setStoreRelease(false);
        settings.setBaseUrl(Settings.BASE_URL_STAGING);
        settings.setLogLevel(Settings.LOG_LEVEL_ERROR);
        settings.setShowStartAlert(false);
        Updraft.initialize(this, settings);
        Updraft.getInstance().start();
    }

}
