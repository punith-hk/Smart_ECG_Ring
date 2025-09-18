package com.smartringpro.mannaheal.api.userHealthData

import com.google.gson.annotations.SerializedName

data class AddUserHealthDataResponse(
    val message: String,
    val data: AddHealthData
) {
    data class AddHealthData(
        val user_id: String,
        val type: String,
        val value: String,
        val updated_at: String,
        val created_at: String,
        val id: Int
    )
}

data class AddUserAppDetailResponse(
    val user_id: String
) {
}
data class DeviceStatusRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("firmware_version") val firmwareVersion: String,
    @SerializedName("location") val location: String,
    @SerializedName("bluetooth") val bluetooth: String,
    @SerializedName("ring") val ring: String,
    @SerializedName("battery") val battery: String,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("version") val version: String,
    @SerializedName("status") val status: String,
    @SerializedName("os") val os: Int = 1
)

data class GetUserHealthDataResponse(
    val message: String,
    val data: List<GetHealthData>
) {
    data class GetHealthData(
        val user_id: String,
        val type: String,
        var value: String,
        val updated_at: String,
        val created_at: String,
        val id: Int,
        val timestamp: String?
    )
}

data class GetUserHealthDataByDayResponse(
    val message: String,
    val data: List<GetHealthData>
) {
    data class GetHealthData(
        val vDate: String,
        var value: Double,
        var diastolicValue: Double
    )
}

data class SleepResponse(
    val message: String,
    val data: List<SleepBeanResponse>
)

data class SleepBeanResponse(
    val id: Int,
    val user_id: Int,
    val statisticTime: Long,
    val startSleepHour: Int,
    val startSleepMinute: Int,
    val endSleepHour: Int,
    val endSleepMinute: Int,
    val totalTimes: Int,
    val deepSleepTimes: Int,
    val lightSleepTimes: Int,
    val wakeupTimes: Int,
    val created_at: String,
    val updated_at: String,
    val sleep_details: List<SleepDetail>
)

data class SleepDetail(
    val id: Int,
    val startTime: Long,
    val endTime: Long,
    val sleepType: Int,
    val created_at: String,
    val updated_at: String,
    val sleep_id: Int
)

data class SleepResponseByDateWise(
    val message: String,
    val data: List<SleepDataByDateWise>
)

data class SleepDataByDateWise(
    val sleepType: Int,
    val weekDay: String,
    val minute_difference: String
)

data class RingValueEntry(
    val value: Any,
    val timestamp: Long
)

data class CreateRingValuesRequest(
    val user_id: Int,
    val type: String,
    val values: List<RingValueEntry>
)


