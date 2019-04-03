package com.appswithlove.updraft.interactor;

import com.appswithlove.updraft.api.ApiWrapper;
import io.reactivex.Single;

/**
 * Created by satori on 3/27/18.
 */

public class CheckUpdateInteractor {

    private ApiWrapper mApiWrapper;

    public CheckUpdateInteractor(ApiWrapper apiWrapper) {
        mApiWrapper = apiWrapper;
    }

    public Single<CheckUpdateResultModel> checkUpdate() {
        return mApiWrapper
                .checkLastVersion()
                .flatMap(checkLastVersionResponse -> {
                    boolean isAutoupdateEnabled = checkLastVersionResponse.isAutoupdateEnabled();
                    if (checkLastVersionResponse.getIsNewVersion() && isAutoupdateEnabled) {
                        return mApiWrapper
                                .getLastVersion()
                                .map(getLastVersionResponse -> {
                                    String url = getLastVersionResponse.getUpdateUrl();
                                    CheckUpdateResultModel result = new CheckUpdateResultModel();
                                    result.setShowAlert(url != null);
                                    result.setUrl(url);
                                    return result;
                                });
                    }
                    CheckUpdateResultModel result = new CheckUpdateResultModel();
                    result.setShowAlert(false);
                    return Single.just(result);
                });
    }

}
