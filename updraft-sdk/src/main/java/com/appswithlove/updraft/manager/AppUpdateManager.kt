package com.appswithlove.updraft.manager

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.appswithlove.updraft.interactor.CheckUpdateInteractor
import com.appswithlove.updraft.presentation.UpdraftSdkUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Created by satori on 3/27/18.
 */
class AppUpdateManager(
    private val checkUpdateInteractor: CheckUpdateInteractor,
    private val updraftSdkUi: UpdraftSdkUi
) : DefaultLifecycleObserver, UpdraftSdkUi.Listener {

    private var checkUpdateJob: Job? = null

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        updraftSdkUi.showFeedbackAlert()
        updraftSdkUi.setListener(this)

        checkUpdateJob?.cancel()
        checkUpdateJob = owner.lifecycleScope.launch {
            try {
                val result = checkUpdateInteractor.checkUpdate()
                val url = result.url
                if (result.showAlert && url != null) {
                    updraftSdkUi.showUpdateAvailableAlert(url)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        updraftSdkUi.setListener(null)
        checkUpdateJob?.cancel()
    }

    override fun onOkClicked(url: String) {
        updraftSdkUi.openUrl(url)
    }
}
