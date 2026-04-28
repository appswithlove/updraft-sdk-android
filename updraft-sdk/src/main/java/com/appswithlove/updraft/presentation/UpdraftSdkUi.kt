package com.appswithlove.updraft.presentation

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.text.format.DateUtils
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.appswithlove.updraft.BuildConfig
import com.appswithlove.updraft.R
import com.appswithlove.updraft.Settings
import com.appswithlove.updraft.Updraft.Companion.getInstance
import com.appswithlove.updraft.feedback.FeedbackActivity
import com.appswithlove.updraft.manager.CurrentActivityManger
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.R as MaterialR
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private var mPendingVersion: String? = null
    private var mPendingYourVersion: String? = null
    private var mPendingCreateAt: String? = null
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
            val activity = currentActivity
            if (activity == null) {
                mShowStartAlertDialogPending = true
                return
            }
            mShowStartAlertDialogPending = false
            val builder = MaterialAlertDialogBuilder(activity.materialDialogContext(), R.style.Updraft_AlertDialog)
            builder.setTitle(R.string.updraft_feedbackDialog_title)
            builder.setMessage(R.string.updraft_feedbackDialog_description)
            builder.setCancelable(true)
            builder.setPositiveButton(R.string.updraft_button_ok) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
            builder.show().boldTitle()
            mFeedbackAlertShown = true
        }
    }

    fun showUpdateAvailableAlert(
        url: String,
        version: String? = null,
        yourVersion: String? = null,
        createAt: String? = null,
    ) {
        val activity = currentActivity
        if (activity == null) {
            mShowDialogPending = true
            mPendingUrl = url
            mPendingVersion = version
            mPendingYourVersion = yourVersion
            mPendingCreateAt = createAt
            return
        }

        if (mUpdateAlertShown) {
            return
        }

        mShowDialogPending = false
        mPendingUrl = null
        mPendingVersion = null
        mPendingYourVersion = null
        mPendingCreateAt = null

        val title = if (!version.isNullOrBlank()) {
            activity.getString(R.string.updraft_updateAvailable_titleWithVersion, version)
        } else {
            activity.getString(R.string.updraft_updateAvailable_title)
        }
        val message = buildUpdateMessage(activity, yourVersion, createAt)

        val builder = MaterialAlertDialogBuilder(activity.materialDialogContext(), R.style.Updraft_AlertDialog)
        builder.setPositiveButton(R.string.updraft_updateAvailable_openButton) { dialog: DialogInterface?, id: Int ->
            if (mListener != null) {
                mListener?.onOkClicked(url)
            }
        }
        builder.setNegativeButton(R.string.updraft_updateAvailable_laterButton) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        builder.setCancelable(false)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.show().boldTitle()
        mUpdateAlertShown = true
    }

    private fun buildUpdateMessage(
        context: Context,
        yourVersion: String?,
        createAt: String?,
    ): String {
        val age = formatRelativeAge(context, createAt)
        val hasVersion = !yourVersion.isNullOrBlank()
        return when {
            age != null && hasVersion ->
                context.getString(R.string.updraft_updateAvailable_descriptionFull, age, yourVersion)
            age != null ->
                context.getString(R.string.updraft_updateAvailable_releasedRelative, age)
            hasVersion ->
                context.getString(R.string.updraft_updateAvailable_yourVersion, yourVersion)
            else ->
                context.getString(R.string.updraft_updateAvailable_description)
        }
    }

    private fun formatRelativeAge(context: Context, createAt: String?): CharSequence? {
        if (createAt.isNullOrBlank()) return null
        val date = parseCreateAt(createAt) ?: return null
        val now = System.currentTimeMillis()
        val days = (now - date.time) / DateUtils.DAY_IN_MILLIS
        return when {
            now - date.time < DateUtils.MINUTE_IN_MILLIS ->
                context.getString(R.string.updraft_relative_justNow)
            days < 7L -> DateUtils.getRelativeTimeSpanString(
                date.time, now, DateUtils.MINUTE_IN_MILLIS,
            )
            days < 30L -> {
                val weeks = (days / 7L).toInt()
                context.resources.getQuantityString(R.plurals.updraft_relative_weeksAgo, weeks, weeks)
            }
            days < 365L -> {
                val months = (days / 30L).toInt()
                context.resources.getQuantityString(R.plurals.updraft_relative_monthsAgo, months, months)
            }
            else -> DateUtils.getRelativeTimeSpanString(date.time, now, DateUtils.DAY_IN_MILLIS)
        }
    }

    private fun parseCreateAt(createAt: String): Date? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
        )
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                return format.parse(createAt) ?: continue
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return null
    }

    fun showFeedbackDisabledAlert() {
        val activity = currentActivity
        if (activity == null) {
            mShowFeedbackDisabledDialogPending = true
            return
        }
        mShowFeedbackDisabledDialogPending = false
        val builder = MaterialAlertDialogBuilder(activity.materialDialogContext(), R.style.Updraft_AlertDialog)
        builder.setNegativeButton(R.string.updraft_button_cancel) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        builder.setCancelable(false)
        builder.setTitle(R.string.updraft_feedbackDisabled_title)
        builder.setMessage(R.string.updraft_feedbackDisabled_description)
        builder.show().boldTitle()
    }

    fun showHowToGiveFeedbackAlert() {
        if (!mSettings.showFeedbackAlert) {
            return
        }
        val activity = currentActivity
        if (activity == null) {
            mShowHowToFeedbackDialogPending = true
            return
        }
        mShowHowToFeedbackDialogPending = false
        val builder = MaterialAlertDialogBuilder(activity.materialDialogContext(), R.style.Updraft_AlertDialog)
        builder.setTitle(R.string.updraft_feedbackDialog_title)
        builder.setMessage(R.string.updraft_feedbackDialog_description)
        builder.setPositiveButton(R.string.updraft_button_ok) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        builder.show().boldTitle()
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
            showUpdateAvailableAlert(
                url = mPendingUrl.orEmpty(),
                version = mPendingVersion,
                yourVersion = mPendingYourVersion,
                createAt = mPendingCreateAt,
            )
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

private fun androidx.appcompat.app.AlertDialog.boldTitle() {
    findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.apply {
        typeface = Typeface.create(typeface, Typeface.BOLD)
    }
}

private fun Activity.materialDialogContext(): Context =
    ContextThemeWrapper(this, MaterialR.style.Theme_Material3_Light)

