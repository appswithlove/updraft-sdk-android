package com.appswithlove.updraft.api.request;

import com.google.gson.annotations.SerializedName;

public class FeedbackEnabledRequest {

    @SerializedName("sdk_key")
    private String mSdkKey;

    @SerializedName("app_key")

    private String mAppKey;

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

}
