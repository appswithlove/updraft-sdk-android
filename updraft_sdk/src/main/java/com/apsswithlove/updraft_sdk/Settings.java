package com.apsswithlove.updraft_sdk;

/**
 * Created by satori on 3/27/18.
 */

public class Settings {

    public static final String BASE_URL_STAGING = "https://u2.mqd.me/api/";
    public static final String BASE_URL_PROD = "https://getupdraft.com/api/";

    public static final int LOG_LEVEL_NONE = 0;
    public static final int LOG_LEVEL_ERROR = 1;
    public static final int LOG_LEVEL_DEBUG = 2;

    private String mSdkKey;
    private String mAppKey;
    private boolean mIsStoreRelease;
    private String mBaseUrl = BASE_URL_PROD;
    private int mLogLevel = LOG_LEVEL_ERROR;
    private boolean mShowStartAlert;

    public String getSdkKey() {
        return mSdkKey;
    }

    public void setSdkKey(String sdkKey) {
        mSdkKey = sdkKey;
    }

    public String getAppKey() {
        return mAppKey;
    }

    public void setAppKey(String appKey) {
        mAppKey = appKey;
    }

    public boolean isStoreRelease() {
        return mIsStoreRelease;
    }

    public void setStoreRelease(boolean storeRelease) {
        mIsStoreRelease = storeRelease;
    }

    public String getBaseUrl() {
        return mBaseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        mBaseUrl = baseUrl;
    }

    public int getLogLevel() {
        return mLogLevel;
    }

    public void setLogLevel(int logLevel) {
        mLogLevel = logLevel;
    }

    public boolean getShowStartAlert() {
        return mShowStartAlert;
    }

    public void setShowStartAlert(boolean showAlert) {
        mShowStartAlert = showAlert;
    }

    public boolean shouldShowErrors() {
        return mLogLevel == LOG_LEVEL_DEBUG || mLogLevel == LOG_LEVEL_ERROR;
    }
}
