package com.apsswithlove.updraft_sdk.api.response;

import com.google.gson.annotations.SerializedName;

/**
 * Created by satori on 3/27/18.
 */

public class CheckLastVersionResponse {

    @SerializedName("create_at")
    private String mCreteAt;

    @SerializedName("whats_new")
    private String mWhatsNew;

    @SerializedName("version")
    private String mVersion;

    @SerializedName("your_version")
    private String mYourVersion;

    @SerializedName("is_new_version")
    private boolean mIsNewVersion;

    @SerializedName("is_autoupdate_enabled")
    private boolean mIsAutoupdateEnabled;

    public String getCreteAt() {
        return mCreteAt;
    }

    public String getWhatsNew() {
        return mWhatsNew;
    }

    public String getVersion() {
        return mVersion;
    }

    public String getYourVersion() {
        return mYourVersion;
    }

    public boolean getIsNewVersion() {
        return mIsNewVersion;
    }

    public boolean isAutoupdateEnabled() {
        return mIsAutoupdateEnabled;
    }
}
