package com.smartringpro.mannaheal.api.login

import com.smartringpro.mannaheal.api.ApiConstants
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface LoginApiService {
    @FormUrlEncoded
    @POST(ApiConstants.LOGIN_ENDPOINT)
    fun login(@Field("mobile_number") mobileNumber: String): Call<LoginResponse>
}