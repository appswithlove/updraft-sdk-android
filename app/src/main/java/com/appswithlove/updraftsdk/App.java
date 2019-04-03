package com.appswithlove.updraftsdk;

import android.app.Application;

import com.appswithlove.updraft.Settings;
import com.appswithlove.updraft.Updraft;

import static com.appswithlove.updraftsdk.Keys.APP_KEY;
import static com.appswithlove.updraftsdk.Keys.SDK_KEY;

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
