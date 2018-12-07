package com.apsswithlove.updraft_sdk.manager;

import android.app.Application;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import com.apsswithlove.updraft_sdk.Settings;
import com.apsswithlove.updraft_sdk.presentation.UpdraftSdkUi;

import static android.content.Context.SENSOR_SERVICE;
import static com.apsswithlove.updraft_sdk.Updraft.UPDRAFT_TAG;

public class ShakeDetectorManager implements LifecycleObserver, SensorEventListener {

    public interface ShakeDetectorListener {

        void onShakeDetected();
    }

    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7F;
    private static final int SHAKE_SLOP_TIME_MS = 500;
    private static final int SHAKE_COUNT_RESET_TIME_MS = 3000;

    private SensorManager mSensorManager;
    private UpdraftSdkUi mUpdraftSdkUi;
    private Settings mSettings;
    private Sensor mAccelerometer;
    private ShakeDetectorListener mShakeDetectorListener;

    private long mShakeTimestamp;
    private int mShakeCount;
    private boolean mShouldListen = true;

    public ShakeDetectorManager(Application application,
                                UpdraftSdkUi updraftSdkUi,
                                Settings settings,
                                ShakeDetectorListener shakeDetectorListener) {
        mSensorManager = (SensorManager) application.getSystemService(SENSOR_SERVICE);
        mUpdraftSdkUi = updraftSdkUi;
        mSettings = settings;
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetectorListener = shakeDetectorListener;

    }

    public void start() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        mSensorManager.registerListener(this,
                mAccelerometer,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        // gForce will be close to 1 when there is no movement.
        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (mSettings.getLogLevel() == Settings.LOG_LEVEL_DEBUG) {
            Log.d(UPDRAFT_TAG, "sensor change with gForce = " + gForce);
        }

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            final long now = System.currentTimeMillis();
            // ignore shake events too close to each other (500ms)
            if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return;
            }

            // reset the shake count after 3 seconds of no shakes
            if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                mShakeCount = 0;
            }

            mShakeTimestamp = now;
            mShakeCount++;

            onShaked();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void onShaked() {
        if (mShouldListen) {
            if (mSettings.getLogLevel() == Settings.LOG_LEVEL_DEBUG) {
                Log.d(UPDRAFT_TAG, "shake event detected");
            }
            if (mShakeDetectorListener != null) {
                mShakeDetectorListener.onShakeDetected();
            }
            stopListening();
        }
    }

    public void startListening() {
        mShouldListen = true;
    }

    public void stopListening() {
        mShouldListen = false;
    }
}
