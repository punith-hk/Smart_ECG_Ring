package com.smartringpro.mannaheal.api.linkedAccountData

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LinkedAccountApiService {
    @GET("getLastRingData")
    fun getLastRingData(@Query("user_id") userId: Int): Call<LastRingDataResponse>

    @GET("caretaker/{user_id}")
    fun getLinkedAccountData(@Path("user_id") userId: Int): Call<List<LinkedAccountInfo>>

    @POST("caretaker/request")
    fun addLinkedAccountData(
        @Body body: CaretakerRequestBody
    ): Call<AddLinkedAccountResponse>

    @POST("caretaker/verify")
    fun verifyCaretakerOtp(
        @Body request: CaretakerVerifyOtpRequestBody
    ): Call<CaretakerVerifyOtpResponse>

}