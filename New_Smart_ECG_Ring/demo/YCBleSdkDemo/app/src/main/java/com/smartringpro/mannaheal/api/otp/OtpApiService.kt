package com.smartringpro.mannaheal.api.otp
import com.smartringpro.mannaheal.api.ApiConstants
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OtpApiService {
    @FormUrlEncoded
    @POST(ApiConstants.VALIDATE_OTP_ENDPOINT)
    fun validateOtp(
        @Field("user_id") userId: Int?,
        @Field("otp") otp: String
    ): Call<OtpResponse>

}