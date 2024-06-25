package com.appswithlove.updraft

import android.app.Application
import com.appswithlove.updraft.api.ApiWrapper
import com.appswithlove.updraft.interactor.CheckFeedbackEnabledInteractor
import com.appswithlove.updraft.interactor.CheckUpdateInteractor
import com.appswithlove.updraft.manager.AppUpdateManager
import com.appswithlove.updraft.manager.CheckFeedbackEnabledManager
import com.appswithlove.updraft.manager.CurrentActivityManger
import com.appswithlove.updraft.manager.ShakeDetectorManager
import com.appswithlove.updraft.presentation.DefaultScreenshotProvider
import com.appswithlove.updraft.presentation.ScreenshotProvider
import com.appswithlove.updraft.presentation.UpdraftSdkUi

/**
 * Created by satori on 3/27/18.
 */
class Updraft private constructor(
    application: Application,
    private val settings: Settings,
    screenshotProvider: ScreenshotProvider,
) {
    private val mAppUpdateManager: AppUpdateManager
    val shakeDetectorManager: ShakeDetectorManager
    private val mCheckFeedbackEnabledManager: CheckFeedbackEnabledManager
    val apiWrapper: ApiWrapper = ApiWrapper(application, settings)

    fun start() {
        mAppUpdateManager.start()
        if (settings.feedbackEnabled) {
            shakeDetectorManager.start()
        }
        mCheckFeedbackEnabledManager.start()
    }

    init {
        val checkUpdateInteractor = CheckUpdateInteractor(apiWrapper)
        val updraftSdkUi = UpdraftSdkUi(CurrentActivityManger.INSTANCE, settings, screenshotProvider)
        mAppUpdateManager = AppUpdateManager(checkUpdateInteractor, updraftSdkUi)
        val checkFeedbackEnabledInteractor = CheckFeedbackEnabledInteractor(
            apiWrapper, application
        )
        mCheckFeedbackEnabledManager =
            CheckFeedbackEnabledManager(updraftSdkUi, checkFeedbackEnabledInteractor)
        shakeDetectorManager =
            ShakeDetectorManager(application, updraftSdkUi, settings, mCheckFeedbackEnabledManager)
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
        private fun createUpdraft(application: Application, settings: Settings, screenshotProvider: ScreenshotProvider = DefaultScreenshotProvider()) {
            if (instance == null) {
                instance = Updraft(application, settings, screenshotProvider)
            }
        }

        private fun checkInitialized() {
            checkNotNull(instance) { NOT_INITIALIZED_MESSAGE }
        }

        @JvmStatic
        fun getInstance(): Updraft? {
            checkInitialized()
            return instance
        }
    }
}