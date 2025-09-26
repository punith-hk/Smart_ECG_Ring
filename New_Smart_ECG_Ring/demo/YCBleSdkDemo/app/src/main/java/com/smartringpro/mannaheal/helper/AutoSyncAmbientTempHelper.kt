package com.smartringpro.mannaheal.helper

import android.content.Context
import android.util.Log
import com.smartringpro.mannaheal.api.userHealthData.AddUserHealthDataResponse
import com.smartringpro.mannaheal.api.userHealthData.RingValueEntry
import com.smartringpro.mannaheal.api.userHealthData.UserHealthDataRepository
import com.yucheng.ycbtsdk.Constants.BLEState
import com.yucheng.ycbtsdk.YCBTClient
import org.threeten.bp.Instant
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode

class AutoSyncAmbientTempHelper(private var applicationContext: Context) {

    private val isBleConnected = YCBTClient.connectState() == BLEState.ReadWriteOK
    private val repository = UserHealthDataRepository()
    private var userId: Int
    private var tempUnit: String

    init {
        val sharedPreferences = applicationContext.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        this.userId = sharedPreferences.getInt("id", -1)

        val sharedUserPref = applicationContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        tempUnit = sharedUserPref.getString("temperature_unit", "Celsius degrees") ?: "Celsius degrees"
    }

    fun checkAmbientTemp() {
        var checkTemp = true

//        if (data.extData != 0) {
//            val aTemp = data.extData
//
//            // Send to API
//            sendHealthDataToApi(
//                "ambient_temperature",
//                aTemp.toString(),
//                Instant.now().epochSecond
//            )
//
//            checkTemp = false
//        }
    }

    private fun applyTemperatureUnit(data: Float): String {
        if (data == 0f) return "0"
        val convertedData = if (tempUnit == "Fahrenheit") {
            if (data < 50f) celsiusToFahrenheit(data) else data
        } else {
            if (data > 50f) fahrenheitToCelisius(data) else data
        }
        return convertedData.toString()
    }

    private fun fahrenheitToCelisius(data: Float): Float {
        return ((data - 32) * 5 / 9).toBigDecimal().setScale(1, RoundingMode.HALF_EVEN).toFloat()
    }

    private fun celsiusToFahrenheit(celsius: Float): Float {
        return (celsius * 9 / 5 + 32).toBigDecimal().setScale(1, RoundingMode.HALF_EVEN).toFloat()
    }

    private fun sendHealthDataToApi(type: String, value: String, timestamp: Long) {
        val vitalEntry = RingValueEntry(
            value = value,
            timestamp = timestamp
        )
        sendVitalListToApi(type, listOf(vitalEntry))
    }

    private fun sendVitalListToApi(type: String, values: List<RingValueEntry>) {
        repository.saveHealthDataBatch(userId, type, values).enqueue(object :
            Callback<AddUserHealthDataResponse> {
            override fun onResponse(
                call: Call<AddUserHealthDataResponse>,
                response: Response<AddUserHealthDataResponse>
            ) {
                if (response.isSuccessful) {
                    Log.i("AmbientTempHelper", "$type uploaded successfully")
                } else {
                    Log.e("AmbientTempHelper", "$type upload failed: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<AddUserHealthDataResponse>, t: Throwable) {
                Log.e("AmbientTempHelper", "$type upload failed: ${t.message}")
            }
        })
    }
}