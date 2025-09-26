package com.smartringpro.mannaheal.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.smartringpro.mannaheal.api.userHealthData.AddUserAppDetailResponse
import com.smartringpro.mannaheal.api.userHealthData.DeviceStatusRequest
import com.smartringpro.mannaheal.api.userHealthData.UserHealthDataRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object UserAppDetailSender {
    fun sendUserAppDetail(context: Context, isBackground: Boolean = false) {

        val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        val currentMessage = sharedPreferences.getString("message", "Unknown") ?: "Unknown"
        val lastSentMessage = sharedPreferences.getString("lastSentMessage", null)

        Log.d("UserAppDetailSender", "Current message=$currentMessage, Last sent=$lastSentMessage")

        if (isBackground && currentMessage == lastSentMessage) {
            Log.i("UserAppDetailSender", "No change in message, skipping sendUserAppDetail.")
            return
        }
        // Save the new message as last sent
        sharedPreferences.edit().putString("lastSentMessage", currentMessage).apply()
        Log.i("UserAppDetailSender", "Message marked as last sent")

        val lat = sharedPreferences.getString("lat", "0.0")
        val lng = sharedPreferences.getString("lng", "0.0")
        val ring = sharedPreferences.getString("ring", "0")
        val location = sharedPreferences.getString("location", "0")
        val firmwareVersion = sharedPreferences.getString("firmwareVersion", "1.0")
        val bluetooth = sharedPreferences.getString("bluetooth", "0")
        val battery = sharedPreferences.getString("battery", "0")
        val userId = sharedPreferences.getInt("id",-1)

        val appVersion =  try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }

        val request = DeviceStatusRequest(
            userId = userId.toString(),
            appVersion = appVersion,
            firmwareVersion = firmwareVersion.toString(),
            location = location.toString(),
            bluetooth = bluetooth.toString(),
            ring = ring.toString(),
            battery = battery.toString(),
            latitude = lat.toString(),
            longitude = lng.toString(),
            version = Build.VERSION.RELEASE,
            status = currentMessage
        )

        Log.i("UserAppDetailSender", "Preparing to send user app detail: $request")

        val repository = UserHealthDataRepository()
        repository.saveUserAppDetail(request).enqueue(object :
            Callback<AddUserAppDetailResponse> {
            override fun onResponse(
                call: Call<AddUserAppDetailResponse>,
                response: Response<AddUserAppDetailResponse>
            ) {
                Log.i("UserAppDetailSender", "Successfully sent user app detail, response=$response")
            }

            override fun onFailure(call: Call<AddUserAppDetailResponse>, t: Throwable) {
                Log.e("UserAppDetailSender", "Failed to send user app detail", t)
            }
        })
    }
}
