package com.smartringpro.mannaheal.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.smartringpro.mannaheal.helper.AutoSyncHelper
import com.smartringpro.mannaheal.util.ConnectionPreferences
import com.smartringpro.mannaheal.helper.AutoSyncAmbientTempHelper
import com.yucheng.ycbtsdk.Constants.BLEState
import com.yucheng.ycbtsdk.YCBTClient

class TemperatureWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    private val isBleConnected = YCBTClient.connectState() == BLEState.ReadWriteOK

    override fun doWork(): Result {

        val macAddress = ConnectionPreferences.getMacAddress(applicationContext)
        val name = ConnectionPreferences.getDeviceName(applicationContext)

        if (!isBleConnected && macAddress != null && name != null) {
            checkRingConnection()
//            bleConnectHelper.connection(macAddress)
        } else {
            Log.i("TemperatureWorker","Scheduled tempCheck")
            val autoSyncAmbientTempHelper = AutoSyncAmbientTempHelper(applicationContext)
//            autoSyncAmbientTempHelper.checkAmbientTemp()
        }

        return Result.success()
    }

    private fun checkRingConnection() {
//        bleConnectHelper.setBleConnectionListener(object : BleConnectionListener {
//            override fun disConnection() {
//            }
//
//            override fun discoveredServices() {
//            }
//
//            override fun onConnectionFail() {
//                RingConnectionStatus.postValue("Failed")
//                bleConnectHelper.onDestroy()
//            }
//
//            override fun onConnectionSuccess() {
//                RingConnectionStatus.postValue("Connected")
//                Log.i("onConnectionSuccess","Scheduled syncConfig tempCheck")
//
//                val autoSyncAmbientTempHelper = AutoSyncAmbientTempHelper(applicationContext)
////                autoSyncAmbientTempHelper.checkAmbientTemp()
//            }
//        })
    }
}