package com.smartringpro.mannaheal.helper

import android.content.Context
import android.util.Log
import com.smartringpro.mannaheal.api.userHealthData.AddUserHealthDataResponse
import com.smartringpro.mannaheal.api.userHealthData.RingValueEntry
import com.smartringpro.mannaheal.api.userHealthData.UserHealthDataRepository
import com.smartringpro.mannaheal.util.ConnectionPreferences
import com.yucheng.ycbtsdk.Constants
import com.yucheng.ycbtsdk.Constants.BLEState
import com.yucheng.ycbtsdk.YCBTClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AutoSyncHelper(private var applicationContext: Context) {

    private var isBleConnected = YCBTClient.connectState() == BLEState.ReadWriteOK
    private val repository = UserHealthDataRepository()
    private var userId: Int
    private var tempUnit: String
    private val isRingDataHandledMap = mutableMapOf<String, Boolean>()

    init {
        val sharedPreferences =
            applicationContext.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        this.userId = sharedPreferences.getInt("id", -1)

        val sharedUserPref =
            applicationContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        tempUnit =
            sharedUserPref.getString("temperature_unit", "Celsius degrees") ?: "Celsius degrees"
    }

    fun heartDataSync() {
        syncHeartRateData()
    }

    fun otherVitalDataSync() {
        syncBloodPressureData()
    }

    private fun sendVitalListToApi(
        type: String,
        values: List<RingValueEntry>,
        onSuccess: (() -> Unit)? = null
    ) {
        if (values.isNotEmpty()) {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", java.util.Locale.getDefault())

            val entriesLog = values.joinToString(
                separator = "\n",
                prefix = "\n",
                postfix = "\n"
            ) { entry ->
                val formattedTime = dateFormat.format(java.util.Date(entry.timestamp * 1000))
                " → $type=${entry.value} at $formattedTime"
            }
            Log.i("AutoSyncHelper", "$type - Uploading ${values.size} entries:$entriesLog")
        } else {
            Log.i("AutoSyncHelper", "$type - No entries to upload")
        }

        repository.saveHealthDataBatch(userId, type, values).enqueue(object :
            Callback<AddUserHealthDataResponse> {
            override fun onResponse(
                call: Call<AddUserHealthDataResponse>,
                response: Response<AddUserHealthDataResponse>
            ) {
                if (response.isSuccessful) {
                    Log.i("AutoSyncHelper", "$type uploaded successfully")
                    onSuccess?.invoke()
                } else {
                    Log.e("AutoSyncHelper", "$type upload failed: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<AddUserHealthDataResponse>, t: Throwable) {
                Log.e("AutoSyncHelper", "$type upload failed: ${t.message}")
            }
        })
    }

    private fun syncHeartRateData() {
        if (!isBleConnected) {
            Log.w("AutoSyncHelper", "HeartRate - Device not connected, skipping sync")
            return
        }

        // 0x0302 = heart rate history (replace with your constant if defined in SDK)
        YCBTClient.healthHistoryData(
            Constants.DATATYPE.Health_HistoryHeart,
            object : com.yucheng.ycbtsdk.response.BleDataResponse {
            override fun onDataResponse(
                responseCode: Int,
                ratio: Float,
                resultMap: HashMap<*, *>?
            ) {
                if (responseCode != 0 || resultMap == null) {
                    Log.w("AutoSyncHelper", "HeartRate - Error fetching data, code=$responseCode")
                    return
                }

                val dataList = resultMap["data"] as? List<HashMap<String, Any>>
                if (dataList.isNullOrEmpty()) {
                    Log.i("AutoSyncHelper", "HeartRate - No data returned")
                    return
                }

                val ringEntries = mutableListOf<RingValueEntry>()
                dataList.forEach { item ->
                    val value = item["heartValue"] as? Int
                    val timestamp = item["heartStartTime"] as? Long
                    if (value != null && timestamp != null) {
                        ringEntries.add(RingValueEntry(value, timestamp / 1000))
                    }
                }

                if (ringEntries.isNotEmpty()) {
                    sendVitalListToApi("heart_rate", ringEntries) {
                        // Save latest entry locally after successful sync
                        val latest = ringEntries.maxByOrNull { it.timestamp }
                        latest?.let {
                            ConnectionPreferences.setLastVitalTime(
                                applicationContext,
                                "heart_rate",
                                it.timestamp,
                                it.value.toString()
                            )
                        }
                    }
                }
            }
        })
    }

    private fun syncBloodPressureData() {
        if (!isBleConnected) {
            Log.w("AutoSyncHelper", "BloodPressure - Device not connected, skipping sync")
            return
        }

        // 0x0304 = blood pressure history (replace with your constant if defined in SDK)
        YCBTClient.healthHistoryData(
            Constants.DATATYPE.Health_HistoryBlood,
            object : com.yucheng.ycbtsdk.response.BleDataResponse {
            override fun onDataResponse(
                responseCode: Int,
                ratio: Float,
                resultMap: HashMap<*, *>?
            ) {
                if (responseCode != 0 || resultMap == null) {
                    Log.w("AutoSyncHelper", "BloodPressure - Error fetching data, code=$responseCode")
                    return
                }

                val dataList = resultMap["data"] as? List<HashMap<String, Any>>
                if (dataList.isNullOrEmpty()) {
                    Log.i("AutoSyncHelper", "BloodPressure - No data returned")
                    return
                }

                val ringEntries = mutableListOf<RingValueEntry>()
                dataList.forEach { item ->
                    val sbp = item["bloodSBP"] as? Int
                    val dbp = item["bloodDBP"] as? Int
                    val timestamp = item["bloodStartTime"] as? Long
                    if (sbp != null && dbp != null && timestamp != null) {
                        ringEntries.add(RingValueEntry("$sbp/$dbp", timestamp / 1000))
                    }
                }

                if (ringEntries.isNotEmpty()) {
                    sendVitalListToApi("blood_pressure", ringEntries) {
                        // Save latest entry locally after successful sync
                        val latest = ringEntries.maxByOrNull { it.timestamp }
                        latest?.let {
                            ConnectionPreferences.setLastVitalTime(
                                applicationContext,
                                "blood_pressure",
                                it.timestamp,
                                it.value.toString()
                            )
                        }
                    }
                }
            }
        })
    }
}