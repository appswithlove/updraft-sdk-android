package com.appswithlove.updraft.interactor

import com.appswithlove.updraft.api.ApiWrapper
import io.reactivex.Single

class CheckUpdateInteractor(private val apiWrapper: ApiWrapper) {

    fun checkUpdate(): Single<CheckUpdateResultModel> {
        return apiWrapper.checkLastVersion()
            .flatMap { checkLastVersionResponse ->
                val isAutoupdateEnabled = checkLastVersionResponse.isAutoupdateEnabled
                if (checkLastVersionResponse.isNewVersion && isAutoupdateEnabled) {
                    apiWrapper.getLastVersion()
                        .map { getLastVersionResponse ->
                            val url = getLastVersionResponse.updateUrl
                            CheckUpdateResultModel(
                                showAlert = url != null,
                                url = url
                            )
                        }
                } else {
                    Single.just(CheckUpdateResultModel(showAlert = false))
                }
            }
    }
}
