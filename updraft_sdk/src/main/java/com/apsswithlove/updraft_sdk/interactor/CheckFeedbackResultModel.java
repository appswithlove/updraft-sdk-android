package com.apsswithlove.updraft_sdk.interactor;

public class CheckFeedbackResultModel {

    public static final int ALERT_TYPE_HOW_TO_GIVE_FEEDBACK = 0;
    public static final int ALERT_TYPE_FEEDBACK_DISABLED = 1;

    private boolean mShowAlert;
    private int mAlertType;
    private boolean mIsFeedbackEnabled;

    public CheckFeedbackResultModel(boolean showAlert, int alertType, boolean isFeedbackEnabled) {
        mShowAlert = showAlert;
        mAlertType = alertType;
        mIsFeedbackEnabled = isFeedbackEnabled;
    }

    public boolean isShowAlert() {
        return mShowAlert;
    }

    public int getAlertType() {
        return mAlertType;
    }

    public boolean isFeedbackEnabled() {
        return mIsFeedbackEnabled;
    }
}
