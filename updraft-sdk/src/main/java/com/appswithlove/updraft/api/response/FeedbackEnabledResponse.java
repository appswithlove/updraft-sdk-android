package com.appswithlove.updraft.api.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FeedbackEnabledResponse {

    @SerializedName("is_feedback_enabled")
    private Boolean mIsFeedbackEnabled;

    @SerializedName("error_code")
    private List<String> mErrorCodes;

    @SerializedName("error_description")
    private List<String> mErrorDescriptions;

    public Boolean getFeedbackEnabled() {
        return mIsFeedbackEnabled;
    }

    public void setFeedbackEnabled(Boolean feedbackEnabled) {
        mIsFeedbackEnabled = feedbackEnabled;
    }

    public List<String> getErrorCodes() {
        return mErrorCodes;
    }

    public void setErrorCodes(List<String> errorCodes) {
        mErrorCodes = errorCodes;
    }

    public List<String> getErrorDescriptions() {
        return mErrorDescriptions;
    }

    public void setErrorDescriptions(List<String> errorDescriptions) {
        mErrorDescriptions = errorDescriptions;
    }
}
