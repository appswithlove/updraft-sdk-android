package com.appswithlove.updraft.api

import com.appswithlove.updraft.api.request.CheckLastVersionRequest
import com.appswithlove.updraft.api.request.FeedbackEnabledRequest
import com.appswithlove.updraft.api.request.GetLastVersionRequest
import com.appswithlove.updraft.api.response.CheckLastVersionResponse
import com.appswithlove.updraft.api.response.FeedbackEnabledResponse
import com.appswithlove.updraft.api.response.FeedbackMobileResponse
import com.appswithlove.updraft.api.response.GetLastVersionResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PartMap

interface UpdraftService {

    @POST("check_last_version/")
    suspend fun checkLastVersion(
        @Body checkLastVersionRequest: CheckLastVersionRequest,
    ): CheckLastVersionResponse

    @POST("get_last_version/")
    suspend fun getLastVersion(
        @Body getLastVersionRequest: GetLastVersionRequest,
    ): GetLastVersionResponse

    @Multipart
    @POST("feedback-mobile/")
    suspend fun feedbackMobile(
        @PartMap map: Map<String, @JvmSuppressWildcards RequestBody>,
    ): FeedbackMobileResponse

    @POST("feedback-mobile-enabled/")
    suspend fun isFeedbackEnabled(
        @Body feedbackEnabledRequest: FeedbackEnabledRequest,
    ): FeedbackEnabledResponse
}
