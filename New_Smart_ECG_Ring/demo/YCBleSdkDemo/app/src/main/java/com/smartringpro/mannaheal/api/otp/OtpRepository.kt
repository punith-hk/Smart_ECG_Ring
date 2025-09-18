package com.smartringpro.mannaheal.api.otp
import com.smartringpro.mannaheal.api.ApiClient
import retrofit2.Call

class OtpRepository {
    private val otpApi = ApiClient.retrofit.create(OtpApiService::class.java)

    fun validateOtp(userId: Int?, otp: String): Call<OtpResponse> {
        return otpApi.validateOtp(userId, otp)
    }
}