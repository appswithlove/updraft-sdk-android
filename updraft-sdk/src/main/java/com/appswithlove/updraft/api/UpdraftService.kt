package com.appswithlove.updraft.api

import com.appswithlove.updraft.api.request.CheckLastVersionRequest
import com.appswithlove.updraft.api.request.FeedbackEnabledRequest
import com.appswithlove.updraft.api.request.GetLastVersionRequest
import com.appswithlove.updraft.api.response.CheckLastVersionResponse
import com.appswithlove.updraft.api.response.FeedbackEnabledResponse
import com.appswithlove.updraft.api.response.FeedbackMobileResponse
import com.appswithlove.updraft.api.response.GetLastVersionResponse
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PartMap

interface UpdraftService {

    @POST("check_last_version/")
    fun checkLastVersion(
        @Body checkLastVersionRequest: CheckLastVersionRequest,
    ): Single<CheckLastVersionResponse>

    @POST("get_last_version/")
    fun getLastVersion(
        @Body getLastVersionRequest: GetLastVersionRequest,
    ): Single<GetLastVersionResponse>

    @Multipart
    @POST("feedback-mobile/")
    fun feedbackMobile(
        @PartMap map: Map<String, @JvmSuppressWildcards RequestBody>,
    ): Single<FeedbackMobileResponse>

    @POST("feedback-mobile-enabled/")
    fun isFeedbackEnabled(
        @Body feedbackEnabledRequest: FeedbackEnabledRequest,
    ): Single<FeedbackEnabledResponse>
}
