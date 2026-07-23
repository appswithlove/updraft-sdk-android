package com.appswithlove.updraft.interactor

data class CheckUpdateResultModel(
    val showAlert: Boolean,
    val url: String? = null,
    val version: String? = null,
    val yourVersion: String? = null,
    val createAt: String? = null,
)
