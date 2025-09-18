package com.smartringpro.mannaheal.api.ringApi

import retrofit2.Call
import retrofit2.http.GET

interface RingApiService {
    @GET("getAutoSyncConfig")
    fun getAutoSyncConfig(): Call<RingConfigResponse>
}