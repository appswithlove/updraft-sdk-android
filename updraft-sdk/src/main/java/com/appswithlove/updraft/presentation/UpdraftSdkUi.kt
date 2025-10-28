package com.appswithlove.updraft.presentation

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import com.appswithlove.updraft.BuildConfig
import com.appswithlove.updraft.R
import com.appswithlove.updraft.Settings
import com.appswithlove.updraft.Updraft.Companion.getInstance
import com.appswithlove.updraft.feedback.FeedbackActivity
import com.appswithlove.updraft.manager.CurrentActivityManger
import java.io.IOException
import androidx.core.net.toUri

/**
 * Created by satori on 3/27/18.
 */
class UpdraftSdkUi(
    currentActivityManger: CurrentActivityManger,
    private val mSettings: Settings,
    private val screenshotProvider: ScreenshotProvider,
) : CurrentActivityManger.CurrentActivityListener {
    private var currentActivity: Activity? = null
    private var mShowDialogPending = false
    private var mPendingUrl: String? = null
    private var mListener: Listener? = null
    private var mFeedbackAlertShown = false
    var mUpdateAlertShown = false
        private set
    private var mShowStartAlertDialogPending = false
    private var mShowHowToFeedbackDialogPending = false
    private var mShowFeedbackDisabledDialogPending = false
    var isCurrentlyShowingFeedback = false
        private set

    init {
        currentActivityManger.addListener(this)
    }

    fun setListener(listener: Listener?) {
        mListener = listener
    }

    fun showFeedbackAlert() {
        if (!mSettings.showFeedbackAlert || !mSettings.feedbackEnabled) {
            return
        }
        if (!mFeedbackAlertShown) {
            if (currentActivity == null) {
                mShowStartAlertDialogPending = true
                return
            }
            mShowStartAlertDialogPending = false
            val builder = AlertDialog.Builder(currentActivity)
            builder.setTitle(R.string.updraft_feedbackDialog_title)
            builder.setMessage(R.string.updraft_feedbackDialog_description)
            builder.setCancelable(true)
            builder.setPositiveButton(R.string.updraft_button_ok) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
            builder.show()
            mFeedbackAlertShown = true
        }
    }

    fun showUpdateAvailableAlert(url: String) {
        if (currentActivity == null) {
            mShowDialogPending = true
            mPendingUrl = url
            return
        }

        if (mUpdateAlertShown) {
            return
        }

        mShowDialogPending = false
        mPendingUrl = null
        val builder = AlertDialog.Builder(currentActivity)
        builder.setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, id: Int ->
            if (mListener != null) {
                mListener?.onOkClicked(url)
            }
        }
        builder.setNegativeButton(R.string.updraft_button_cancel) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        builder.setCancelable(false)
        builder.setTitle(R.string.updraft_updateAvailable_title)
        builder.setMessage(R.string.updraft_updateAvailable_description)
        builder.show()
        mUpdateAlertShown = true
    }

    fun showFeedbackDisabledAlert() {
        if (currentActivity == null) {
            mShowFeedbackDisabledDialogPending = true
            return
        }
        mShowFeedbackDisabledDialogPending = false
        val builder = AlertDialog.Builder(currentActivity)
        builder.setNegativeButton(R.string.updraft_button_cancel) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        builder.setCancelable(false)
        builder.setTitle(R.string.updraft_feedbackDisabled_title)
        builder.setMessage(R.string.updraft_feedbackDisabled_description)
        builder.show()
    }

    fun showHowToGiveFeedbackAlert() {
        if (currentActivity == null) {
            mShowHowToFeedbackDialogPending = true
            return
        }
        mShowHowToFeedbackDialogPending = false
        val builder = AlertDialog.Builder(currentActivity)
        builder.setTitle(R.string.updraft_feedbackDialog_title)
        builder.setMessage(R.string.updraft_feedbackDialog_description)
        builder.setPositiveButton(R.string.updraft_button_ok) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        builder.show()
    }

    fun openUrl(url: String?) {
        val uri = url?.toUri()
        val i = Intent(Intent.ACTION_VIEW, uri)
        if (currentActivity != null) {
            currentActivity!!.startActivity(i)
        }
    }

    fun showFeedback() {
        var activityStarted = false
        val activity = currentActivity
        if (activity != null) {
            val bitmap = screenshotProvider.getBitmap(activity)
            if (bitmap != null) {
                try {
                    saveBitmap(bitmap)
                    val intent = FeedbackActivity.getIntent(activity, FILENAME)
                    activity.startActivity(intent)
                    activity.overridePendingTransition(0, 0)
                    activityStarted = true
                } catch (e: IOException) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace()
                    }
                    Toast.makeText(activity, R.string.updraft_global_error, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        if (!activityStarted) {
            getInstance()!!.shakeDetectorManager.startListening()
        }
    }

    @Throws(IOException::class)
    private fun saveBitmap(bitmap: Bitmap) {
        //Write file
        val stream = currentActivity!!.openFileOutput(FILENAME, Context.MODE_PRIVATE)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

        //Cleanup
        stream.close()
        bitmap.recycle()
    }


    fun closeFeedback() {
        currentActivity.apply {
            if (this is FeedbackActivity && !this.isFinishing) {
                this.finish()
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        if (mShowStartAlertDialogPending) {
            showFeedbackAlert()
        }
        if (mShowDialogPending) {
            showUpdateAvailableAlert(mPendingUrl.orEmpty())
        }
        if (mShowHowToFeedbackDialogPending) {
            showHowToGiveFeedbackAlert()
        }
        if (mShowFeedbackDisabledDialogPending) {
            showFeedbackDisabledAlert()
        }
        if (activity is FeedbackActivity) {
            isCurrentlyShowingFeedback = true
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity is FeedbackActivity) {
            isCurrentlyShowingFeedback = false
        }
        currentActivity = null
    }

    companion object {
        private const val TAG = "UpdraftSdkUi"
        private const val FILENAME = "UPDRAFT_SCREENSHOT.png"
    }

    interface Listener {
        fun onOkClicked(url: String)
    }
}