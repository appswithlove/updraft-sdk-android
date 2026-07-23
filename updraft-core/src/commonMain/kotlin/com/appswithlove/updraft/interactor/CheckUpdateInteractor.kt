package com.appswithlove.updraft.interactor

import com.appswithlove.updraft.api.UpdraftApiContract

class CheckUpdateInteractor(private val api: UpdraftApiContract) {

    suspend fun checkUpdate(): CheckUpdateResultModel {
        val check = api.checkLastVersion()
        if (!check.isNewVersion || !check.isAutoupdateEnabled) {
            return CheckUpdateResultModel(showAlert = false)
        }
        val url = api.getLastVersion().updateUrl
        return CheckUpdateResultModel(
            showAlert = url != null,
            url = url,
            version = check.version,
            yourVersion = check.yourVersion,
            createAt = check.createAt,
        )
    }
}
