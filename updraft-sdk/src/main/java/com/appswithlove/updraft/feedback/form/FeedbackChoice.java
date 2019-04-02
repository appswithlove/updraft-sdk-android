package com.appswithlove.updraft.feedback.form;

public class FeedbackChoice {

    public static final long FEEDBACK_TYPE_NOT_SELECTED = 0;
    public static final long FEEDBACK_TYPE_DESIGN = 1;
    public static final long FEEDBACK_TYPE_FEEDBACK = 2;
    public static final long FEEDBACK_TYPE_BUG = 3;

    private static final String FEEDBACK_TYPE_DESIGN_API_STRING = "design";
    private static final String FEEDBACK_TYPE_FEEDBACK_API_STRING = "feedback";
    private static final String FEEDBACK_TYPE_BUG_API_STRING = "bug";

    private long mId;
    private String mName;
    private boolean mIsHiddenInDropdown;

    public FeedbackChoice(long id, String name, boolean isHiddenInDropdown) {
        mId = id;
        mName = name;
        mIsHiddenInDropdown = isHiddenInDropdown;
    }

    public long getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public boolean isHiddenInDropdown() {
        return mIsHiddenInDropdown;
    }

    public String getApiName() {
        if (mId == FEEDBACK_TYPE_DESIGN) {
            return FEEDBACK_TYPE_DESIGN_API_STRING;
        }
        if (mId == FEEDBACK_TYPE_FEEDBACK) {
            return FEEDBACK_TYPE_FEEDBACK_API_STRING;
        }
        if (mId == FEEDBACK_TYPE_BUG) {
            return FEEDBACK_TYPE_BUG_API_STRING;
        }
        return null;
    }
}
