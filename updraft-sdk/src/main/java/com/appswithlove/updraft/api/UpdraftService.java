package com.appswithlove.updraft.api;

import com.appswithlove.updraft.api.request.CheckLastVersionRequest;
import com.appswithlove.updraft.api.request.FeedbackEnabledRequest;
import com.appswithlove.updraft.api.request.GetLastVersionRequest;
import com.appswithlove.updraft.api.response.CheckLastVersionResponse;
import com.appswithlove.updraft.api.response.FeedbackEnabledResponse;
import com.appswithlove.updraft.api.response.FeedbackMobileResponse;
import com.appswithlove.updraft.api.response.GetLastVersionResponse;
import io.reactivex.Single;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PartMap;

import java.util.Map;

/**
 * Created by satori on 3/27/18.
 */

public interface UpdraftService {

    @POST("check_last_version/")
    Single<CheckLastVersionResponse> checkLastVersion(@Body CheckLastVersionRequest checkLastVersionRequest);

    @POST("get_last_version/")
    Single<GetLastVersionResponse> getLastVersion(@Body GetLastVersionRequest getLastVersionRequest);

    @Multipart
    @POST("feedback-mobile/")
    Single<FeedbackMobileResponse> feedbackMobile(@PartMap Map<String, RequestBody> map);

    @POST("feedback-mobile-enabled/")
    Single<FeedbackEnabledResponse> isFeedbackEnabled(@Body FeedbackEnabledRequest feedbackEnabledRequest);
}
