package com.apsswithlove.updraft_sdk.api;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import com.apsswithlove.updraft_sdk.BuildConfig;
import com.apsswithlove.updraft_sdk.Settings;
import com.apsswithlove.updraft_sdk.api.request.CheckLastVersionRequest;
import com.apsswithlove.updraft_sdk.api.request.FeedbackMobileRequest;
import com.apsswithlove.updraft_sdk.api.request.GetLastVersionRequest;
import com.apsswithlove.updraft_sdk.api.response.CheckLastVersionResponse;
import com.apsswithlove.updraft_sdk.api.response.FeedbackMobileResponse;
import com.apsswithlove.updraft_sdk.api.response.GetLastVersionResponse;
import com.apsswithlove.updraft_sdk.feedback.form.FeedbackChoice;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by satori on 3/27/18.
 */

public class ApiWrapper {

    private UpdraftService mUpdraftService;
    private Context mContext;
    private Settings mSettings;

    public ApiWrapper(Context context, Settings settings) {
        mContext = context;
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        if (settings.getLogLevel() == Settings.LOG_LEVEL_DEBUG) {
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        }
        if (settings.getLogLevel() == Settings.LOG_LEVEL_NONE || settings.getLogLevel() == Settings.LOG_LEVEL_ERROR) {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        httpClientBuilder.addInterceptor(logging);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(settings.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(httpClientBuilder.build())
                .build();
        mUpdraftService = retrofit.
                create(UpdraftService.class);
        mSettings = settings;
    }

    public Single<GetLastVersionResponse> getLastVersion() {
        return Single.defer(() -> {
            GetLastVersionRequest getLastVersionRequest = new GetLastVersionRequest();
            getLastVersionRequest.setSdkKey(mSettings.getSdkKey());
            getLastVersionRequest.setAppKey(mSettings.getAppKey());
            return mUpdraftService.getLastVersion(getLastVersionRequest);
        });
    }

    public Single<CheckLastVersionResponse> checkLastVersion() {
        return Single.defer(() -> {
            CheckLastVersionRequest checkLastVersionRequest = new CheckLastVersionRequest();
            checkLastVersionRequest.setAppKey(mSettings.getAppKey());
            checkLastVersionRequest.setSdkKey(mSettings.getSdkKey());
            int versionCode = -1;
            try {
                PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                versionCode = packageInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (versionCode == -1) {
                return Single.error(new IllegalStateException("Version code is invalid"));
            }
            checkLastVersionRequest.setVersion(Integer.toString(versionCode));
            return mUpdraftService.checkLastVersion(checkLastVersionRequest);
        });
    }

    public Observable<Double> sendMobileFeedback(FeedbackChoice feedbackChoice, String description, String email, String fileName) {

        return Observable.defer(() -> {
            FeedbackMobileRequest feedbackMobileRequest = new FeedbackMobileRequest();
            feedbackMobileRequest.setAppKey(mSettings.getAppKey());
            feedbackMobileRequest.setSdkKey(mSettings.getSdkKey());
            feedbackMobileRequest.setTag(feedbackChoice.getApiName());
            feedbackMobileRequest.setImage(fileName);
            feedbackMobileRequest.setDescription(description);
            feedbackMobileRequest.setEmail(email);
            PackageManager m = mContext.getPackageManager();
            String appVersion = "";
            try {
                appVersion = m.getPackageInfo(mContext.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            feedbackMobileRequest.setBuildVersion(appVersion);
            feedbackMobileRequest.setSystemVersion(Build.VERSION.RELEASE);
            feedbackMobileRequest.setDeviceName(Build.MODEL);
            String androidId = android.provider.Settings.Secure.getString(mContext.getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);
            feedbackMobileRequest.setDeviceUuid(androidId);
            return Observable.create(new ObservableOnSubscribe<Double>() {
                @Override
                public void subscribe(ObservableEmitter<Double> emitter) throws Exception {
                    try {
                        FeedbackMobileResponse response = mUpdraftService.feedbackMobile(mapMultipart(feedbackMobileRequest, (bytesWritten, contentLength) -> {
                            double progress = (1.0 * bytesWritten) / contentLength;
                            emitter.onNext(progress);
                        })).blockingGet();
                    } catch (Throwable t) {
                        if (mSettings.shouldShowErrors()) {
                            t.printStackTrace();
                        }
                        emitter.onError(t);
                    }
                    emitter.onComplete();
                }
            });
        });
    }

    private Map<String, RequestBody> mapMultipart(FeedbackMobileRequest request, CountingRequestBody.Listener listener) {
        Map<String, RequestBody> map = new HashMap<>();
        File imageFile = new File(mContext.getFilesDir(), request.getImage());
        RequestBody imageRequestBody = new CountingRequestBody(RequestBody.create(MediaType.parse("image/png"), imageFile), listener);
        MediaType plainText = MediaType.parse("text/plain");
        map.put("image\"; filename=\"" + imageFile.getName(), imageRequestBody);
        if (request.getAppKey() != null) {
            map.put("app_key", RequestBody.create(plainText, request.getAppKey()));
        }
        if (request.getSdkKey() != null) {
            map.put("sdk_key", RequestBody.create(plainText, request.getSdkKey()));
        }
        if (request.getTag() != null) {
            map.put("tag", RequestBody.create(plainText, request.getTag()));
        }
        if (request.getDescription() != null) {
            map.put("description", RequestBody.create(plainText, request.getDescription()));
        }
        if (request.getEmail() != null) {
            map.put("email", RequestBody.create(plainText, request.getEmail()));
        }
        if (request.getBuildVersion() != null) {
            map.put("build_version", RequestBody.create(plainText, request.getBuildVersion()));
        }
        if (request.getSystemVersion() != null) {
            map.put("system_version", RequestBody.create(plainText, request.getSystemVersion()));
        }
        if (request.getDeviceName() != null) {
            map.put("device_name", RequestBody.create(plainText, request.getDeviceName()));
        }
        if (request.getDeviceUuid() != null) {
            map.put("device_uuid", RequestBody.create(plainText, request.getDeviceUuid()));
        }
        if (request.getNavigationStack() != null) {
            map.put("navigation_stack", RequestBody.create(plainText, request.getNavigationStack()));
        }
        return map;
    }

}
