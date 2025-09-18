package com.smartringpro.mannaheal.api.userHealthData

//import com.vanzoo.ble.bean.SleepBean
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface UserHealthDataApiService {
    @POST("CreateRingValue")
    fun saveHealthData(
        @Query("user_id") userId: Int,
        @Query("type") type: String,
        @Query("value") value: String,
        @Query("timestamp") timestamp: Long
    ): Call<AddUserHealthDataResponse>

    @POST("CreateRingValues")
    fun saveHealthDataBatch(
        @Body request: CreateRingValuesRequest
    ): Call<AddUserHealthDataResponse>

    @GET("getRingDataByType")
    fun getHeartRateData(
        @Query("user_id") userId: Int,
        @Query("type") type: String
    ): Call<GetUserHealthDataResponse>

    @GET("getRingDataByDay")
    fun getHealthRateDatabyDay(
        @Query("user_id") userId: Int,
        @Query("type") type: String
    ): Call<GetUserHealthDataByDayResponse>

    @GET("getSleepData")
    fun getSleepData(
        @Query("user_id") userId: Int
    ): Call<SleepResponse>

    @GET("getSleepDayWise")
    fun getSleepDataByDateWise(
        @Query("user_id") userId: Int
    ): Call<SleepResponseByDateWise>


    @POST("CreateSleepData")
    fun saveSleepData(
        @Query("user_id") userId: Int,
//        @Body sleepBean: SleepBean
    ): Call<Void>


    @POST("user-app-details")
    fun saveUserAppDetail(@Body request: DeviceStatusRequest): Call<AddUserAppDetailResponse>
}
