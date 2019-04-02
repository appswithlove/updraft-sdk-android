package com.appswithlove.updraft.api.response;

import com.google.gson.annotations.SerializedName;

public class FeedbackMobileResponse {

    @SerializedName("image")
    private String mImage;

    @SerializedName("timestamp")
    private String mTimestamp;

    @SerializedName("tag")
    private String mTag;

    @SerializedName("description")
    private String mDescription;

    @SerializedName("email")
    private String mEmail;

    @SerializedName("build_version")
    private String mBuildVersion;

    @SerializedName("device_name")
    private String mDeviceName;

    @SerializedName("device_uuid")
    private String mDeviceUuid;

    @SerializedName("navigation_stack")
    private String mNavigationStack;

    public String getImage() {
        return mImage;
    }

    public void setImage(String image) {
        mImage = image;
    }

    public String getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(String timestamp) {
        mTimestamp = timestamp;
    }

    public String getTag() {
        return mTag;
    }

    public void setTag(String tag) {
        mTag = tag;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    public String getBuildVersion() {
        return mBuildVersion;
    }

    public void setBuildVersion(String buildVersion) {
        mBuildVersion = buildVersion;
    }

    public String getDeviceName() {
        return mDeviceName;
    }

    public void setDeviceName(String deviceName) {
        mDeviceName = deviceName;
    }

    public String getDeviceUuid() {
        return mDeviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        mDeviceUuid = deviceUuid;
    }

    public String getNavigationStack() {
        return mNavigationStack;
    }

    public void setNavigationStack(String navigationStack) {
        mNavigationStack = navigationStack;
    }
}
