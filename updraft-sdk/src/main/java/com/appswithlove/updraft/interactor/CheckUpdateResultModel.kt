package com.appswithlove.updraft.interactor

data class CheckUpdateResultModel(
    var showAlert: Boolean = false,
    var url: String? = null,
)
