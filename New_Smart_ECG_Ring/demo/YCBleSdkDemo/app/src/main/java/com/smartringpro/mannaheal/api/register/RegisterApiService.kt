package com.smartringpro.mannaheal.api.register

import com.smartringpro.mannaheal.api.ApiConstants
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Part

interface RegisterApiService {
    @FormUrlEncoded
    @POST(ApiConstants.REGISTER_ENDPOINT)
    fun register(
        @Field("mobile_number") mobileNumber: String,
        @Field("name") name: String
    ): Call<RegisterResponse>

    @POST(ApiConstants.FCM_TOKEN)
    fun sendFcmToken(
        @Body payload: FcmTokenRequest
    ): Call<FcmTokenResponse>
}