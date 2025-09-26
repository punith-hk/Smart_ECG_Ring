package com.smartringpro.mannaheal.workers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.smartringpro.mannaheal.helper.AutoSyncAmbientTempHelper
import com.smartringpro.mannaheal.helper.AutoSyncHelper
import com.smartringpro.mannaheal.service.BackgroundService
import com.smartringpro.mannaheal.util.ConnectionPreferences
import com.smartringpro.mannaheal.util.UserAppDetailSender
import com.yucheng.ycbtsdk.Constants.BLEState
import com.yucheng.ycbtsdk.YCBTClient

class OtherVitalsSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private val isBleConnected = YCBTClient.connectState() == BLEState.ReadWriteOK

    override fun doWork(): Result {
        val macAddress = ConnectionPreferences.getMacAddress(applicationContext)
        val name = ConnectionPreferences.getDeviceName(applicationContext)

        if (!isBleConnected && macAddress != null && name != null) {
            Log.i("AutoOtherSyncWorker", "BLE not connected. Initiating connection...")
//            checkRingConnection()
//            bleConnectHelper.connection(macAddress)
//            val serviceIntent = Intent(applicationContext, BackgroundService::class.java).apply {
//                putExtra("Extra_MacAddress", macAddress)
//                putExtra("Extra_name", name)
//            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                applicationContext.startForegroundService(serviceIntent)
//            } else {
//                applicationContext.startService(serviceIntent)
//            }
        } else {
            Log.i("AutoOtherSyncWorker", "BLE already connected. Running vitalDataSync directly...")

            val autoSyncHelper = AutoSyncHelper(applicationContext)
            autoSyncHelper.otherVitalDataSync()

            val autoSyncAmbientTemp = AutoSyncAmbientTempHelper(applicationContext)
            autoSyncAmbientTemp.checkAmbientTemp()

//            bleConnectHelper.getBatteryLevel(object : BleCallBack<Int> {
//                override fun result(data: Int) {
//                    Log.i("AutoSyncWorker", "Battery level: $data%")
//                    setAppPref("battery", "$data")
//                    UserAppDetailSender.sendUserAppDetail(applicationContext, false)
//                }
//            })
        }

        return Result.success()
    }

    private fun checkRingConnection() {
//        bleConnectHelper.setBleConnectionListener(object : BleConnectionListener {
//            override fun disConnection() {
//                // No action for now
//            }
//
//            override fun discoveredServices() {
//                // No action for now
//            }
//
//            override fun onConnectionFail() {
//                Log.w("AutoOtherSyncWorker", "BLE connection failed")
//                RingConnectionStatus.postValue("Failed")
//                bleConnectHelper.onDestroy()
//            }
//
//            override fun onConnectionSuccess() {
//                Log.i("AutoOtherSyncWorker", "BLE connected successfully")
//                RingConnectionStatus.postValue("Connected")
//
//                val autoSyncHelper = AutoSyncHelper(applicationContext)
//                autoSyncHelper.otherVitalDataSync()
//
//                val autoSyncAmbientTemp = AutoSyncAmbientTempHelper(applicationContext)
//                autoSyncAmbientTemp.checkAmbientTemp()
//            }
//        })
    }

    private fun setAppPref(key: String, value: String, message: String = "") {
        val sharedPreferences = applicationContext.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        if (message != "") {
            editor.putString("message", message)
        }
        editor.apply()
    }
}
