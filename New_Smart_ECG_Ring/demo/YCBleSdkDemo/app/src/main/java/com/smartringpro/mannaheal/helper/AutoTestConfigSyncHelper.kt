package com.smartringpro.mannaheal.helper

import android.content.Context
import android.util.Log
import com.smartringpro.mannaheal.api.ringApi.RingApiRepository
import com.smartringpro.mannaheal.api.ringApi.RingConfig
import com.smartringpro.mannaheal.api.ringApi.RingConfigResponse
import com.smartringpro.mannaheal.util.ConnectionPreferences
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.response.BleDataResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AutoTestConfigSyncHelper(
    private val context: Context
) {
    private val supportedTypes = setOf(
        "heart", "hrv", "blood_pressure", "body_temperature", "blood_oxygen"
    )

    /**
     * Fetch API configs, compare with last saved configs,
     * update the ring if there are changes in intervals.
     * Enable/disable is handled locally in code.
     */
    fun syncFromApiAndUpdateRing() {
        val lastSavedConfigs = ConnectionPreferences.getLastRingConfig(context)
        RingApiRepository().getAutoSyncConfig().enqueue(object : Callback<RingConfigResponse> {
            override fun onResponse(
                call: Call<RingConfigResponse>,
                response: Response<RingConfigResponse>
            ) {
                val apiResp = response.body()
                if (response.isSuccessful && apiResp?.data?.isNotEmpty() == true) {
                    val apiConfigs = apiResp.data
                    Log.i("AutoSyncHelperConfig", "API configs: $apiConfigs")
                    if (lastSavedConfigs.isEmpty() || isConfigDifferent(lastSavedConfigs, apiConfigs)) {
                        Log.i("AutoSyncHelperConfig", "Config changed — updating ring")
                        updateRingAndSave(apiConfigs)
                    } else {
                        Log.i("AutoSyncHelperConfig", "Config unchanged — skipping update ✅")
                    }
                } else {
                    Log.w("AutoSyncHelperConfig", "No configs from API or response not successful")
                }
            }

            override fun onFailure(call: Call<RingConfigResponse>, t: Throwable) {
                Log.e("AutoSyncHelperConfig", "Error fetching API config", t)
            }
        })
    }

    /**
     * Compare only interval values (ignore enable flag).
     */
    private fun isConfigDifferent(saved: List<RingConfig>, api: List<RingConfig>): Boolean {
        if (saved.size != api.size) return true
        val sortedSaved = saved.sortedBy { it.type }
        val sortedApi = api.sortedBy { it.type }
        return sortedSaved.zip(sortedApi).any { (old, new) -> old.interval != new.interval }
    }

    /**
     * Update the ring for all configs and save to preferences if all succeed.
     */
    private fun updateRingAndSave(configs: List<RingConfig>) {
        val supportedConfigs = configs.filter { supportedTypes.contains(it.type) }
        var allSuccess = true
        var completed = 0
        val total = supportedConfigs.size
        if (total == 0) {
            Log.i("AutoSyncHelperConfig", "No supported configs to update on ring")
            ConnectionPreferences.setLastRingConfig(context, configs)
            return
        }
        for (config in supportedConfigs) {
            setAutoTestConfig(config) { success ->
                if (!success) allSuccess = false
                completed++
                if (completed == total) {
                    if (allSuccess) {
                        ConnectionPreferences.setLastRingConfig(context, configs)
                        Log.i("AutoSyncHelperConfig", "All supported configs updated and saved to preferences")
                    } else {
                        Log.e("AutoSyncHelperConfig", "Some supported configs failed to update on ring")
                    }
                }
            }
        }
    }

    /**
     * Send updated config to the ring with enable flags
     * decided locally (hardcoded or from preferences).
     * Calls callback with true if success, false otherwise.
     */
    fun setAutoTestConfig(ringConfig: RingConfig, callback: (Boolean) -> Unit) {
        when (ringConfig.type) {
            "heart" -> {
                YCBTClient.settingHeartMonitor(
                    0x01,
                    ringConfig.interval,
                    object : BleDataResponse {
                        override fun onDataResponse(i: Int, v: Float, hashMap: HashMap<*, *>?) {
                            val success = i == 0
                            Log.i("AutoSyncHelperConfig", "Heart monitor set to ${ringConfig.interval} minutes, success=$success")
                            callback(success)
                        }
                    })
            }
            "hrv" -> {
                YCBTClient.settingHRVMonitor(
                    0x01,
                    ringConfig.interval,
                    ringConfig.interval,
                    ringConfig.interval,
                    ringConfig.interval,
                    object : BleDataResponse {
                        override fun onDataResponse(i: Int, v: Float, hashMap: HashMap<*, *>?) {
                            val success = i == 0
                            Log.i("AutoSyncHelperConfig", "HRV monitor set to ${ringConfig.interval} minutes, success=$success")
                            callback(success)
                        }
                    })
            }
            "blood_pressure" -> {
                YCBTClient.settingBloodPressureMonitor(
                    0x01,
                    ringConfig.interval,
                    object : BleDataResponse {
                        override fun onDataResponse(i: Int, v: Float, hashMap: HashMap<*, *>?) {
                            val success = i == 0
                            Log.i("AutoSyncHelperConfig", "Blood pressure monitor set to ${ringConfig.interval} minutes, success=$success")
                            callback(success)
                        }
                    })
            }
            "body_temperature" -> {
                val enable = true // Always enable
                YCBTClient.settingTemperatureMonitor(
                    enable,
                    ringConfig.interval,
                    object : BleDataResponse {
                        override fun onDataResponse(i: Int, v: Float, hashMap: HashMap<*, *>?) {
                            val success = i == 0
                            Log.i("AutoSyncHelperConfig", "Temperature monitor enabled with ${ringConfig.interval} minutes, success=$success")
                            callback(success)
                        }
                    })
            }
            "blood_oxygen" -> {
                val enable = true // Always enable
                YCBTClient.settingBloodOxygenModeMonitor(
                    enable,
                    ringConfig.interval,
                    object : BleDataResponse {
                        override fun onDataResponse(i: Int, v: Float, hashMap: HashMap<*, *>?) {
                            val success = i == 0
                            Log.i("AutoSyncHelperConfig", "Blood oxygen monitor enabled with ${ringConfig.interval} minutes, success=$success")
                            callback(success)
                        }
                    })
            }
            else -> {
                Log.w("AutoSyncHelperConfig", "Unknown type ${ringConfig.type}")
                callback(false)
            }
        }
    }
}
