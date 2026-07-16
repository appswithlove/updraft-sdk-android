package com.appswithlove.updraft.platform

import android.app.Application
import android.content.Context
import androidx.startup.Initializer

class UpdraftCoreInitializer : Initializer<UpdraftContext> {
    override fun create(context: Context): UpdraftContext {
        UpdraftContext.application = context.applicationContext as Application
        return UpdraftContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
