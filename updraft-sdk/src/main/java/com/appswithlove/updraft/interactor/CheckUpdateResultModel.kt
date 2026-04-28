package com.appswithlove.updraft.interactor

data class CheckUpdateResultModel(
    var showAlert: Boolean = false,
    var url: String? = null,
    var version: String? = null,
    var yourVersion: String? = null,
    var createAt: String? = null,
)
