package com.appswithlove.updraft.feedback.form;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import com.appswithlove.updraft.R;
import com.appswithlove.updraft.feedback.FeedbackRootContainer;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import androidx.fragment.app.Fragment;

public class FeedbackFormFragment extends Fragment implements FeedbackFormContract.View {

    private FeedbackFormPresenter mPresenter;
    private FeedbackRootContainer mFeedbackRootContainer;
    private Spinner mSpinner;
    private EditText mEmailEdit;
    private EditText mDescriptionEdit;
    private View mProgress;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private TextView mProgressTitle;
    private View mProgressCancelButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPresenter = new FeedbackFormPresenter();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FeedbackRootContainer) {
            mFeedbackRootContainer = (FeedbackRootContainer) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFeedbackRootContainer = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_updraft_feedback, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mProgress = view.findViewById(R.id.updraft_feedback_progress_container);
        view.findViewById(R.id.updraft_feedback_previous_button).setOnClickListener(v -> {
            if (mFeedbackRootContainer != null) {
                mFeedbackRootContainer.goPrevious();
            }
        });
        mEmailEdit = view.findViewById(R.id.updraft_feedback_email);
        mDescriptionEdit = view.findViewById(R.id.updraft_feedback_description);
        Button sendButton = view.findViewById(R.id.updraft_feedback_send_button);
        sendButton.setOnClickListener(v -> mPresenter.onSendButtonClicked());
        sendButton.setEnabled(false);
        mSpinner = view.findViewById(R.id.updraft_feedback_type_spinner);
        List<FeedbackChoice> feedbackChoices = new ArrayList<>();
        feedbackChoices.add(new FeedbackChoice(FeedbackChoice.FEEDBACK_TYPE_NOT_SELECTED, getString(R.string.updraft_feedback_type_title), true));
        feedbackChoices.add(new FeedbackChoice(FeedbackChoice.FEEDBACK_TYPE_DESIGN, getString(R.string.updraft_feedback_type_design), false));
        feedbackChoices.add(new FeedbackChoice(FeedbackChoice.FEEDBACK_TYPE_FEEDBACK, getString(R.string.updraft_feedback_type_feedback), false));
        feedbackChoices.add(new FeedbackChoice(FeedbackChoice.FEEDBACK_TYPE_BUG, getString(R.string.updraft_feedback_type_bug), false));
        FeedbackFormTypeSpinnerAdapter adapter = new FeedbackFormTypeSpinnerAdapter(getContext(), feedbackChoices);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                FeedbackChoice feedbackChoice = (FeedbackChoice) parent.getSelectedItem();
                if (!feedbackChoice.isHiddenInDropdown()) {
                    sendButton.setEnabled(true);
                } else {
                    sendButton.setEnabled(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                sendButton.setEnabled(false);
            }
        });
        mProgressBar = view.findViewById(R.id.updraft_feedback_progress_bar);
        mProgressText = view.findViewById(R.id.updraft_feedback_progress_text);
        mProgressCancelButton = view.findViewById(R.id.updraft_feedback_progress_cancel);
        mProgressCancelButton.setOnClickListener(v -> {
            mPresenter.onProgressCancelClicked();
        });
        mProgressTitle = view.findViewById(R.id.updraft_feedback_progress_title);
        mPresenter.attachView(this);
    }

    @Override
    public void onDestroyView() {
        mPresenter.detachView();
        super.onDestroyView();
    }

    @Override
    public FeedbackChoice getSelectedChoice() {
        return (FeedbackChoice) mSpinner.getSelectedItem();
    }

    @Override
    public String getEmail() {
        return mEmailEdit.getText().toString();
    }

    @Override
    public String getDescription() {
        return mDescriptionEdit.getText().toString();
    }

    @Override
    public void showProgress() {
        mProgress.setVisibility(VISIBLE);
    }

    @Override
    public void hideProgress() {
        mProgress.setVisibility(GONE);
    }

    @Override
    public void showSuccessMessage() {
        mProgressTitle.setText(R.string.updraft_feedback_send_success);
        mProgressBar.setVisibility(VISIBLE);
        mProgressCancelButton.setVisibility(GONE);
    }

    @Override
    public void showErrorMessage(Throwable t) {
        mProgressTitle.setText(R.string.updraft_feedback_send_failure_title);
        mProgressBar.setVisibility(GONE);
        mProgressText.setText(R.string.updraft_feedback_send_failure_description);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void updateProgress(double progress) {
        mProgressTitle.setText(R.string.updraft_feedback_send_inProgress);
        mProgressBar.setVisibility(VISIBLE);
        mProgressBar.setProgress((int) (progress * 100));
        mProgressText.setText(Integer.toString((int) (progress * 100)) + "%");
    }

    @Override
    public void closeFeedback() {
        getActivity().finish();
    }
}
