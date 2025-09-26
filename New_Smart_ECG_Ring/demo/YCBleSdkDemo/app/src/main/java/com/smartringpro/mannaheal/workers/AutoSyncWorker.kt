package com.smartringpro.mannaheal.workers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.smartringpro.mannaheal.helper.AutoSyncHelper
import com.smartringpro.mannaheal.service.BackgroundService
import com.smartringpro.mannaheal.util.ConnectionPreferences
import com.smartringpro.mannaheal.util.UserAppDetailSender
import com.yucheng.ycbtsdk.Constants.BLEState
import com.yucheng.ycbtsdk.YCBTClient

class AutoSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {

        val isBleConnected = YCBTClient.connectState() == BLEState.ReadWriteOK

        val macAddress = ConnectionPreferences.getMacAddress(applicationContext)
        val name = ConnectionPreferences.getDeviceName(applicationContext)

        if (!isBleConnected && macAddress != null && name != null) {
            Log.i("AutoSyncWorker", "BLE not connected. Starting BackgroundService...")

            val serviceIntent = Intent(applicationContext, BackgroundService::class.java).apply {
                putExtra("Extra_MacAddress", macAddress)
                putExtra("Extra_name", name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(serviceIntent)
            } else {
                applicationContext.startService(serviceIntent)
            }
        } else if (isBleConnected) {
            Log.i("AutoSyncWorker", "BLE already connected. Running vitalDataSync directly...")

            setAppPref("ring", "1", "Connected")
            UserAppDetailSender.sendUserAppDetail(applicationContext, false)

            val autoSyncHelper = AutoSyncHelper(applicationContext)
            autoSyncHelper.heartDataSync()
        } else {
            Log.i("AutoSyncWorker", "No MAC/Name found, cannot proceed.")
        }

        Log.i("AutoSyncWorker", "doWork() exiting after scheduling logic.")

        return Result.success()
    }

    private fun setAppPref(key: String, value: String, message: String = "") {
        val sharedPreferences =
            applicationContext.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        if (message != "") {
            editor.putString("message", message)
        }
        editor.apply()
    }

}