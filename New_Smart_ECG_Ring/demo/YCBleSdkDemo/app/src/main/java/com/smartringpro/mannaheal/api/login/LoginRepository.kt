package com.smartringpro.mannaheal.api.login
import com.smartringpro.mannaheal.api.ApiClient
import retrofit2.Call

class LoginRepository {
    private val loginApi = ApiClient.retrofit.create(LoginApiService::class.java)

    fun login(mobileNumber: String): Call<LoginResponse> {
        return loginApi.login(mobileNumber)
    }
}