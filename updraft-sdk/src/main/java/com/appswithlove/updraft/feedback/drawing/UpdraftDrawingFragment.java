package com.appswithlove.updraft.feedback.drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.core.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.appswithlove.updraft.R;
import com.appswithlove.updraft.feedback.FeedbackActivity;
import com.appswithlove.updraft.feedback.FeedbackRootContainer;
import com.rm.freedrawview.FreeDrawView;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import static android.view.View.GONE;

public class UpdraftDrawingFragment extends Fragment {

    private static final String FILENAME_ARG = "filename";

    private FeedbackRootContainer mFeedbackRootContainer;
    private Bitmap mCurrentBitmap;
    private ImageView mScreenShotBitmapHolder;
    private FreeDrawView mFreeDrawView;
    private String mFileName;

    public static UpdraftDrawingFragment getInstance(String fileName) {
        Bundle args = new Bundle();
        args.putString(FILENAME_ARG, fileName);
        UpdraftDrawingFragment updraftDrawingFragment = new UpdraftDrawingFragment();
        updraftDrawingFragment.setArguments(args);
        return updraftDrawingFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFileName = getArguments().getString(FILENAME_ARG);
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        if (activity instanceof FeedbackRootContainer) {
            mFeedbackRootContainer = (FeedbackRootContainer) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_updraft_drawing, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mScreenShotBitmapHolder = view.findViewById(R.id.updraft_screenshot_bitmap_holder);
        mFreeDrawView = view.findViewById(R.id.updraft_drawing_view);
        mFreeDrawView.setPaintWidthPx(6f);
        RadioGroup colorSelectRadioGroup = view.findViewById(R.id.updraft_color_select_radiogroup);
        colorSelectRadioGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            int id = radioGroup.getCheckedRadioButtonId();
            if (id == R.id.updraft_color_button_black) {
                mFreeDrawView.setPaintColor(Color.BLACK);
            } else if (id == R.id.updraft_color_button_white) {
                mFreeDrawView.setPaintColor(Color.WHITE);
            } else if (id == R.id.updraft_color_button_yellow) {
                mFreeDrawView.setPaintColor(ResourcesCompat.getColor(getResources(), R.color.updraft_yellow, null));
            } else if (id == R.id.updraft_color_button_red) {
                mFreeDrawView.setPaintColor(ResourcesCompat.getColor(getResources(), R.color.updraft_red, null));
            }
        });
        view.findViewById(R.id.updraft_color_select_reset_button).setOnClickListener(v -> {
            mFreeDrawView.undoLast();
        });
        try {
            FileInputStream is = getContext().openFileInput(mFileName);
            mCurrentBitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateBitmap();
        view.findViewById(R.id.updraft_feedback_next_button).setOnClickListener(v -> {
            mFreeDrawView.getDrawScreenshot(new FreeDrawView.DrawCreatorListener() {
                @Override
                public void onDrawCreated(Bitmap draw) {
                    saveBitmap(draw);
                }

                @Override
                public void onDrawCreationError() {
                    saveBitmap(null);
                }
            });
        });
        View drawSomethingHereContainer = view.findViewById(R.id.updraft_draw_here_container);
        drawSomethingHereContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                drawSomethingHereContainer.setVisibility(GONE);
                return false;
            }
        });
    }

    private void updateBitmap() {
        Handler handler = new Handler();
        handler.post(() -> {
            mScreenShotBitmapHolder.setImageBitmap(mCurrentBitmap);
        });
    }

    private void saveBitmap(Bitmap drawnBitmap) {
        Bitmap bmOverlay = Bitmap.createBitmap(
                mCurrentBitmap.getWidth(),
                mCurrentBitmap.getHeight(),
                mCurrentBitmap.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(mCurrentBitmap, new Matrix(), null);
        if (drawnBitmap != null) {
            canvas.drawBitmap(drawnBitmap, new Matrix(), null);
        }
        try {
            FileOutputStream stream = getContext().openFileOutput(FeedbackActivity.SAVED_SCREENSHOT, Context.MODE_PRIVATE);
            bmOverlay.compress(Bitmap.CompressFormat.PNG, 100, stream);

            //Cleanup
            stream.close();
            bmOverlay.recycle();
            if (mFeedbackRootContainer != null) {
                mFeedbackRootContainer.goNext();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), R.string.GENERAL_ERROR, Toast.LENGTH_SHORT).show();
        }
    }
}

