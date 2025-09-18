package com.smartringpro.mannaheal.api.register

import com.smartringpro.mannaheal.api.ApiClient
import retrofit2.Call

class RegisterRepository {
    private val registerApi = ApiClient.retrofit.create(RegisterApiService::class.java)

    fun register(mobileNumber: String, name: String): Call<RegisterResponse> {
        return registerApi.register(mobileNumber, name)
    }

    fun sendFcmToken(userId: String, fcmToken: String): Call<FcmTokenResponse> {
        val payload = FcmTokenRequest(user_id = userId, fcm_token = fcmToken)
        return registerApi.sendFcmToken(payload)
    }
}