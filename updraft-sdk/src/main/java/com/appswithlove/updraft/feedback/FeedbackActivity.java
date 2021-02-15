package com.appswithlove.updraft.feedback;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.appswithlove.updraft.R;
import com.appswithlove.updraft.Updraft;
import com.appswithlove.updraft.feedback.drawing.UpdraftDrawingFragment;
import com.appswithlove.updraft.feedback.form.FeedbackFormFragment;

public class FeedbackActivity extends AppCompatActivity implements FeedbackRootContainer {

    public static final String SAVED_SCREENSHOT = "UPDRAFT_SAVED_SCREENSHOT.png";

    private static final String FILENAME_ARG = "filename";

    public static Intent getIntent(Context context, @NonNull String filename) {
        Intent intent = new Intent(context, FeedbackActivity.class);
        intent.putExtra(FILENAME_ARG, filename);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback_activity);
        if (savedInstanceState == null) {
            String filename = getIntent().getStringExtra(FILENAME_ARG);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.updraft_feedback_root_container, UpdraftDrawingFragment.getInstance(filename))
                    .commit();
        }
        findViewById(R.id.updraft_close_button).setOnClickListener(v -> {
            finish();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Updraft.getInstance().getShakeDetectorManager().stopListening();
    }

    @Override
    public void onStop() {
        Updraft.getInstance().getShakeDetectorManager().startListening();
        super.onStop();
    }

    @Override
    public void goNext() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.updraft_feedback_root_container, new FeedbackFormFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void goPrevious() {
        getSupportFragmentManager().popBackStack();
    }
}
