package com.appswithlove.updraft.interactor;

/**
 * Created by satori on 3/27/18.
 */

public class CheckUpdateResultModel {

    private boolean mShowAlert;
    private String mUrl;

    public boolean isShowAlert() {
        return mShowAlert;
    }

    public void setShowAlert(boolean showAlert) {
        mShowAlert = showAlert;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }
}
