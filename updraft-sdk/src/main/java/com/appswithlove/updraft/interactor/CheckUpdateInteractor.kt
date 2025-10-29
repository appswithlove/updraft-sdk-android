package com.appswithlove.updraft.interactor

import com.appswithlove.updraft.api.ApiWrapper

class CheckUpdateInteractor(private val apiWrapper: ApiWrapper) {

    suspend fun checkUpdate(): CheckUpdateResultModel {
        val checkLastVersionResponse = apiWrapper.checkLastVersion()

        val isAutoupdateEnabled = checkLastVersionResponse.isAutoupdateEnabled
        val isNewVersion = checkLastVersionResponse.isNewVersion

        return if (isNewVersion && isAutoupdateEnabled) {
            val getLastVersionResponse = apiWrapper.getLastVersion()
            val url = getLastVersionResponse.updateUrl
            CheckUpdateResultModel(
                showAlert = url != null,
                url = url
            )
        } else {
            CheckUpdateResultModel(showAlert = false)
        }
    }
}
