package com.smartringpro.mannaheal.api.userHealthData

//import com.vanzoo.ble.bean.SleepBean
import com.smartringpro.mannaheal.api.UserDataApiClient
import retrofit2.Call

class UserHealthDataRepository {
    private val healthDataApi = UserDataApiClient.retrofit.create(UserHealthDataApiService::class.java)

    fun saveHealthData(userId: Int, type: String, value: String, time: Long): Call<AddUserHealthDataResponse> {
        return healthDataApi.saveHealthData(userId, type, value, time)
    }
    fun saveHealthDataBatch(userId: Int, type: String, values: List<RingValueEntry>): Call<AddUserHealthDataResponse> {
        val request = CreateRingValuesRequest(userId, type, values)
        return healthDataApi.saveHealthDataBatch(request)
    }
    fun getHealthData(userId: Int, type: String): Call<GetUserHealthDataResponse> {
        return healthDataApi.getHeartRateData(userId, type)
    }
    fun getHealthDataByDay(userId: Int, type: String): Call<GetUserHealthDataByDayResponse> {
        return healthDataApi.getHealthRateDatabyDay(userId, type)
    }
//    fun saveSleepData(userId: Int, sleepData: SleepBean): Call<Void> {
//        return healthDataApi.saveSleepData(userId, sleepData)
//    }
    fun getSleepData(userId: Int): Call<SleepResponse> {
        return healthDataApi.getSleepData(userId)
    }
    fun getSleepDataByDateWise(userId: Int): Call<SleepResponseByDateWise> {
        return healthDataApi.getSleepDataByDateWise(userId)
    }
    fun saveUserAppDetail(deviceStatus: DeviceStatusRequest): Call<AddUserAppDetailResponse> {
        return healthDataApi.saveUserAppDetail(deviceStatus)
    }
}