package com.smartringpro.mannaheal.api.ringApi

import com.smartringpro.mannaheal.api.UserDataApiClient
import retrofit2.Call

class RingApiRepository {
    private val spApi = UserDataApiClient.retrofit.create(RingApiService::class.java)

    fun getAutoSyncConfig(): Call<RingConfigResponse> {
        return spApi.getAutoSyncConfig()
    }
}