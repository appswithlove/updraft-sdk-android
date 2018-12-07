package com.apsswithlove.updraft_sdk.interactor;

import android.content.Context;
import android.content.SharedPreferences;
import com.apsswithlove.updraft_sdk.api.ApiWrapper;
import io.reactivex.Single;

public class CheckFeedbackEnabledInteractor {

    private static final String FEEDBACK_ENABLED_STORAGE = "feedback_enabled_storage";

    private static final String IS_FEEDBACK_ENABLED_PROPERTY = "is_feedback_enabled_property";

    private ApiWrapper mApiWrapper;
    private SharedPreferences mSharedPreferences;

    public CheckFeedbackEnabledInteractor(ApiWrapper apiWrapper, Context context) {
        mApiWrapper = apiWrapper;
        mSharedPreferences = context.getSharedPreferences(FEEDBACK_ENABLED_STORAGE, Context.MODE_PRIVATE);

    }

    public Single<CheckFeedbackResultModel> run() {
        return mApiWrapper.isFeedbackEnabled()
                .map(isEnabled -> {
                    boolean previouslyEnabled = mSharedPreferences.getBoolean(IS_FEEDBACK_ENABLED_PROPERTY, false);
                    boolean showAlert = true;
                    if (!isEnabled && !previouslyEnabled) {
                        showAlert = false;
                    }
                    if (isEnabled && previouslyEnabled) {
                        showAlert = false;
                    }
                    int alertType = CheckFeedbackResultModel.ALERT_TYPE_HOW_TO_GIVE_FEEDBACK;
                    if (!isEnabled && previouslyEnabled) {
                        alertType = CheckFeedbackResultModel.ALERT_TYPE_FEEDBACK_DISABLED;
                    }
                    mSharedPreferences.edit().putBoolean(IS_FEEDBACK_ENABLED_PROPERTY, isEnabled).apply();
                    return new CheckFeedbackResultModel(showAlert, alertType, isEnabled);
                });
    }
}
