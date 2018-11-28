package com.apsswithlove.updraft_sdk.feedback.form;

public interface FeedbackFormContract {

    interface View {

        FeedbackChoice getSelectedChoice();

        String getEmail();

        String getDescription();

        void showProgress();

        void hideProgress();

        void showSuccessMessage();

        void showErrorMessage(Throwable t);

        void updateProgress(double progress);

        void closeFeedback();
    }
}
