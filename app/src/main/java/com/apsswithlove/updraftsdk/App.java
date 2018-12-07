package com.apsswithlove.updraftsdk;

import android.app.Application;
import com.apsswithlove.updraft_sdk.Settings;
import com.apsswithlove.updraft_sdk.Updraft;

import static com.apsswithlove.updraftsdk.Keys.APP_KEY;
import static com.apsswithlove.updraftsdk.Keys.SDK_KEY;

/**
 * Created by satori on 3/27/18.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Settings settings = new Settings();
        settings.setAppKey(APP_KEY);
        settings.setSdkKey(SDK_KEY);
        settings.setStoreRelease(false);
        settings.setLogLevel(Settings.LOG_LEVEL_DEBUG);
        settings.setShowStartAlert(false);
        Updraft.initialize(this, settings);
        Updraft.getInstance().start();
    }

}
