package com.appswithlove.updraft.api.request;

import com.google.gson.annotations.SerializedName;

/**
 * Created by satori on 3/27/18.
 */

public class GetLastVersionRequest {

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
