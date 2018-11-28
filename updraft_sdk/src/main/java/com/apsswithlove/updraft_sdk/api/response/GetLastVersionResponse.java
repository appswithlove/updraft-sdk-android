package com.apsswithlove.updraft_sdk.api.response;

import com.google.gson.annotations.SerializedName;

/**
 * Created by satori on 3/27/18.
 */

public class GetLastVersionResponse {

    @SerializedName("update_url")
    private String mUpdateUrl;

    public String getUpdateUrl() {
        return mUpdateUrl;
    }
}
