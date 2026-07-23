package com.appswithlove.updraft.android

import android.content.Context
import androidx.startup.Initializer
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.UpdraftEvent
import com.appswithlove.updraft.platform.UpdraftContext
import com.appswithlove.updraft.platform.UpdraftCoreInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object UpdraftAndroid {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var started = false

    fun autoWire() {
        if (started) return
        started = true
        scope.launch {
            // Wait until the host app calls Updraft.start()
            while (!runCatching { Updraft.settings }.isSuccess) delay(100)
            Updraft.events.collect { event ->
                val context = UpdraftContext.application
                when (event) {
                    is UpdraftEvent.UpdateAvailable,
                    UpdraftEvent.ShowFeedbackHint,
                    UpdraftEvent.FeedbackDisabled,
                    -> UpdraftOverlayActivity.launch(context, event)

                    UpdraftEvent.FeedbackRequested ->
                        UpdraftFeedbackActivity.launch(context, Updraft.takePendingScreenshot())

                    UpdraftEvent.CloseFeedback, is UpdraftEvent.Error -> Unit
                }
            }
        }
    }
}

class UpdraftSdkInitializer : Initializer<UpdraftAndroid> {
    override fun create(context: Context): UpdraftAndroid {
        UpdraftAndroid.autoWire()
        return UpdraftAndroid
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(UpdraftCoreInitializer::class.java)
}
