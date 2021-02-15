package com.appswithlove.updraft

import android.app.Application
import com.appswithlove.updraft.api.ApiWrapper
import com.appswithlove.updraft.interactor.CheckFeedbackEnabledInteractor
import com.appswithlove.updraft.interactor.CheckUpdateInteractor
import com.appswithlove.updraft.manager.AppUpdateManager
import com.appswithlove.updraft.manager.CheckFeedbackEnabledManager
import com.appswithlove.updraft.manager.ShakeDetectorManager
import com.appswithlove.updraft.presentation.UpdraftSdkUi

/**
 * Created by satori on 3/27/18.
 */
class Updraft private constructor(application: Application, settings: Settings) {
    private val mAppUpdateManager: AppUpdateManager
    val shakeDetectorManager: ShakeDetectorManager
    private val mCheckFeedbackEnabledManager: CheckFeedbackEnabledManager
    val apiWrapper: ApiWrapper = ApiWrapper(application, settings)

    fun start() {
        mAppUpdateManager.start()
        shakeDetectorManager.start()
        mCheckFeedbackEnabledManager.start()
    }

    companion object {
        const val UPDRAFT_TAG = "updraft"
        private const val NOT_INITIALIZED_MESSAGE =
            "Must initialize Updraft before using getInstance()"
        private var instance: Updraft? = null
        fun initialize(application: Application, settings: Settings) {
            createUpdraft(application, settings)
        }

        @Synchronized
        private fun createUpdraft(application: Application, settings: Settings) {
            if (instance == null) {
                instance = Updraft(application, settings)
            }
        }

        private fun checkInitialized() {
            checkNotNull(instance) { NOT_INITIALIZED_MESSAGE }
        }

        @kotlin.jvm.JvmStatic
        fun getInstance(): Updraft {
            checkInitialized()
            return instance!!
        }
    }

    init {
        val checkUpdateInteractor = CheckUpdateInteractor(apiWrapper)
        val updraftSdkUi = UpdraftSdkUi(application, settings)
        mAppUpdateManager = AppUpdateManager(application, checkUpdateInteractor, updraftSdkUi)
        val checkFeedbackEnabledInteractor = CheckFeedbackEnabledInteractor(
            apiWrapper, application
        )
        mCheckFeedbackEnabledManager =
            CheckFeedbackEnabledManager(updraftSdkUi, checkFeedbackEnabledInteractor)
        shakeDetectorManager =
            ShakeDetectorManager(application, settings, mCheckFeedbackEnabledManager)
    }
}
