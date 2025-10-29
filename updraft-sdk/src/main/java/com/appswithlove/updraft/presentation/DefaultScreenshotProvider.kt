package com.appswithlove.updraft.presentation

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.createBitmap

class DefaultScreenshotProvider : ScreenshotProvider {
    override fun getBitmap(activity: Activity): Bitmap? {
        val rootView = activity.window.decorView.rootView ?: return null

        val width = rootView.width
        val height = rootView.height
        if (width == 0 || height == 0) return null

        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        rootView.draw(canvas)

        return bitmap
    }
}
