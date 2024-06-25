package com.appswithlove.updraft.presentation

import android.app.Activity
import android.graphics.Bitmap

interface ScreenshotProvider {
    fun getBitmap(activity: Activity): Bitmap?
}