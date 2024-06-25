package com.appswithlove.updraft.presentation

import android.app.Activity
import android.graphics.Bitmap

class DefaultScreenshotProvider : ScreenshotProvider {
    override fun getBitmap(activity: Activity): Bitmap? {
        val rootView = activity.window.decorView.rootView
        if (rootView != null) {
            rootView.isDrawingCacheEnabled = true
            val drawingCache = rootView.drawingCache
            if (drawingCache == null || drawingCache.width == 0) {
                rootView.isDrawingCacheEnabled = false
                return null
            }
            val bitmap = Bitmap.createBitmap(rootView.drawingCache)
            rootView.isDrawingCacheEnabled = false
            return bitmap
        }
        return null
    }
}