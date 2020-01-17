package com.appswithlove.updraft.presentation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.appswithlove.updraft.BuildConfig;
import com.appswithlove.updraft.R;
import com.appswithlove.updraft.Settings;
import com.appswithlove.updraft.Updraft;
import com.appswithlove.updraft.feedback.FeedbackActivity;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by satori on 3/27/18.
 */

public class UpdraftSdkUi implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "UpdraftSdkUi";
    private static final String FILENAME = "UPDRAFT_SCREENSHOT.png";

    private Activity mCurrentActivity;
    private Settings mSettings;

    private boolean mShowDialogPending = false;
    private String mPendingUrl;
    private Listener mListener;
    private boolean mStartAlertShown = false;
    private boolean mShowStartAlertDialogPending = false;

    private boolean mShowHowToFeedbackDialogPending = false;
    private boolean mShowFeedbackDisabledDialogPending = false;

    private boolean mIsCurrentlyShowingFeedback = false;

    public interface Listener {

        void onOkClicked(String url);
    }

    public UpdraftSdkUi(Application application, Settings settings) {
        mSettings = settings;
        application.registerActivityLifecycleCallbacks(this);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void showStartAlert() {
        if (!mSettings.getShowStartAlert()) {
            return;
        }
        if (!mStartAlertShown) {
            if (mCurrentActivity == null) {
                mShowStartAlertDialogPending = true;
                return;
            }
            mShowStartAlertDialogPending = false;
            AlertDialog.Builder builder = new AlertDialog.Builder(mCurrentActivity);
            builder.setTitle(R.string.updraft_feedback_alert_howToGiveFeedback_title);
            builder.setMessage(R.string.updraft_feedback_alert_howToGiveFeedback_message);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.updraft_alert_button_ok, (dialog, which) -> dialog.dismiss());
            builder.show();
            mStartAlertShown = true;
        }
    }

    public void showAlert(String url) {
        if (mCurrentActivity == null) {
            mShowDialogPending = true;
            mPendingUrl = url;
            return;
        }
        mShowDialogPending = false;
        mPendingUrl = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(mCurrentActivity);
        builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
            if (mListener != null) {
                mListener.onOkClicked(url);
            }
        });
        builder.setNegativeButton(R.string.updraft_alert_button_cancel, (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        builder.setTitle(R.string.updraft_autoUpdate_alert_newVersionAvailable_title);
        builder.setMessage(R.string.updraft_autoUpdate_alert_newVersionAvailable_message);
        builder.show();
    }

    public void showFeedbackDisabledAlert() {
        if (mCurrentActivity == null) {
            mShowFeedbackDisabledDialogPending = true;
            return;
        }
        mShowFeedbackDisabledDialogPending = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(mCurrentActivity);
        builder.setNegativeButton(R.string.updraft_alert_button_cancel, (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        builder.setTitle(R.string.updraft_feedback_alert_feedbackDisabled_title);
        builder.setMessage(R.string.updraft_feedback_alert_feedbackDisabled_message);
        builder.show();
    }

    public void showHowToGiveFeedbackAlert() {
        if (mCurrentActivity == null) {
            mShowHowToFeedbackDialogPending = true;
            return;
        }
        mShowHowToFeedbackDialogPending = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(mCurrentActivity);
        builder.setTitle(R.string.updraft_feedback_alert_howToGiveFeedback_title);
        builder.setMessage(R.string.updraft_feedback_alert_howToGiveFeedback_message);
        builder.setPositiveButton(R.string.updraft_alert_button_ok, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public void openUrl(String url) {
        Uri uri = Uri.parse(url);
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        if (mCurrentActivity != null) {
            mCurrentActivity.startActivity(i);
        }
    }

    public void showFeedback() {
        boolean activityStarted = false;
        if (mCurrentActivity != null) {
            View v1 = mCurrentActivity.getWindow().getDecorView().getRootView();
            if (v1 != null) {
                v1.setDrawingCacheEnabled(true);
                Bitmap drawingCache = v1.getDrawingCache();
                if (drawingCache == null || drawingCache.getWidth() == 0) {
                    v1.setDrawingCacheEnabled(false);
                    return;
                }
                Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
                v1.setDrawingCacheEnabled(false);
                try {
                    saveBitmap(bitmap);
                    Intent intent = FeedbackActivity.getIntent(mCurrentActivity, FILENAME);
                    mCurrentActivity.startActivity(intent);
                    mCurrentActivity.overridePendingTransition(0, 0);
                    activityStarted = true;
                } catch (IOException e) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace();
                    }
                    Toast.makeText(mCurrentActivity, R.string.GENERAL_ERROR, Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (!activityStarted) {
            Updraft.getInstance().getShakeDetectorManager().startListening();
        }
    }

    public boolean isCurrentlyShowingFeedback() {
        return mIsCurrentlyShowingFeedback;
    }

    private void saveBitmap(Bitmap bitmap) throws IOException {
        //Write file
        FileOutputStream stream = mCurrentActivity.openFileOutput(FILENAME, Context.MODE_PRIVATE);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

        //Cleanup
        stream.close();
        bitmap.recycle();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        mCurrentActivity = activity;
        if (mShowStartAlertDialogPending) {
            showStartAlert();
        }
        if (mShowDialogPending) {
            showAlert(mPendingUrl);
        }
        if (mShowHowToFeedbackDialogPending) {
            showHowToGiveFeedbackAlert();
        }
        if (mShowFeedbackDisabledDialogPending) {
            showFeedbackDisabledAlert();
        }
        if (activity instanceof FeedbackActivity) {
            mIsCurrentlyShowingFeedback = true;
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (activity instanceof FeedbackActivity) {
            mIsCurrentlyShowingFeedback = false;
        }
        mCurrentActivity = null;
    }

    public void closeFeedback() {
        if (mCurrentActivity instanceof FeedbackActivity && !mCurrentActivity.isFinishing()) {
            mCurrentActivity.finish();
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
