package com.smartringpro.mannaheal.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.model.ConnectEvent
import com.smartringpro.mannaheal.util.ConnectionPreferences
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.response.BleConnectResponse
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class BackgroundService : Service() {
    private var macAddress: String? = null
    private var name: String? = null
    private var deviceFound = false

    private lateinit var mBluetoothAdapter: BluetoothAdapter

    private var pendingConnect: Boolean = false
    private var pendingMacAddress: String? = null
    private var pendingName: String? = null
    private var currentStatus: String = ""

    private var isManualDisconnect = false
    private var isConnected: Boolean = false

    override fun onCreate() {
        super.onCreate()
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(
            "DeviceBackgroundService",
            "onStartCommand called, about to start foreground notification"
        )
        try {
            startForegroundServiceNotification("Starting...", false)
            Log.i(
                "DeviceBackgroundService",
                "startForegroundServiceNotification called successfully"
            )
        } catch (e: Exception) {
            Log.e("DeviceBackgroundService", "Exception in startForegroundServiceNotification", e)
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            "ACTION_DISCONNECT" -> {
                disconnectAndStop()
                return START_NOT_STICKY
            }
        }

        macAddress =
            intent?.getStringExtra("Extra_MacAddress") ?: ConnectionPreferences.getMacAddress(
                applicationContext
            )
        name = intent?.getStringExtra("Extra_name") ?: ConnectionPreferences.getDeviceName(
            applicationContext
        )

        Log.i("DeviceBackgroundService", "Service started $macAddress")

        // Always attempt to connect if mac/name are present; SDK will handle duplicate connections
        if (macAddress != null && name != null && macAddress != "") {
            Log.i("DeviceBackgroundService", "on start connection $macAddress")
            connectToDevice(macAddress!!, name!!)
        }

        // UI/notification will be updated by the connection callback
        return START_STICKY
    }

    private fun disconnectAndStop() {
        Log.i("DeviceBackgroundService", "disconnectAndStop()")
        isManualDisconnect = true
        isConnected = false
        ConnectionPreferences.saveConnectionState(applicationContext, false, null, null)
        YCBTClient.disconnectBle()
        stopForeground(true)
        stopSelf()
    }

    private fun connectToDevice(macAddress: String, name: String) {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled) {
                setAppPref("bluetooth", "1")
                pendingMacAddress = macAddress
                pendingName = name
                pendingConnect = true
                checkBluetoothPermissionAndProceed()
            } else {
                setAppPref("bluetooth", "0", "Bluetooth not enabled")
                startForegroundServiceNotification("Bluetooth not enabled", true)
            }
        } else {
            setAppPref("bluetooth", "0", "Bluetooth not supported")
            startForegroundServiceNotification("Bluetooth not supported on this device", true)
        }
    }

    private fun checkBluetoothPermissionAndProceed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                setAppPref("bluetooth", "0", "No bluetooth Permission")
                Log.w(
                    "DeviceBackgroundService",
                    "BLUETOOTH_CONNECT permission not granted. Cannot connect."
                )
                startForegroundServiceNotification("Bluetooth permission not granted", true)
                return
            }
        }
        setAppPref("bluetooth", "1")
        pendingMacAddress?.let { mac ->
            Log.i("DeviceBackgroundService", "MAC available: $mac — connecting directly")
            YCBTClient.connectBle(mac, object : BleConnectResponse {
                override fun onConnectResponse(code: Int) {
                    Log.i("DeviceBackgroundService", "YCBTClient.connectBle callback: code=$code")
                    if (code == 1) {
                        isConnected = true
                        startForegroundServiceNotification("Connected to $pendingName", true)
                        ConnectionPreferences.saveConnectionState(applicationContext, true, mac, pendingName)
                    } else {
                        isConnected = false
                        startForegroundServiceNotification("Device not connected", false)
                        ConnectionPreferences.saveConnectionState(applicationContext, false, null, null)
                    }
                }
            })
        }
    }

    private fun startForegroundServiceNotification(
        connectionStatus: String,
        playSound: Boolean = false
    ) {
        createNotificationChannel(playSound)

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priority = if (playSound) {
            NotificationCompat.PRIORITY_MAX
        } else {
            NotificationCompat.PRIORITY_LOW
        }

        val builder = NotificationCompat.Builder(this, "RING_CHANNEL")
            .setContentTitle("Ring Connection Service")
            .setContentText(connectionStatus)
            .setSmallIcon(R.drawable.baseline_album_24)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (!playSound) {
            builder.setSound(null)
        }
        // No .setDefaults(NotificationCompat.DEFAULT_ALL) to avoid forcing sound/vibration

        val notification = builder.build()
        val notificationId = 1 // Use a constant ID here
        startForeground(notificationId, notification)
        currentStatus = connectionStatus
    }

    private fun createNotificationChannel(playSound: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "RING_CHANNEL"
            val channelName = "Ring Connection Notifications"

            val importance = if (playSound) {
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_LOW
            }

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Notifications for Ring connection service"
                enableLights(true)
                enableVibration(true)
                if (playSound) {
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setSound(soundUri, audioAttributes)
                } else {
                    setSound(null, null)
                }
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun setAppPref(key: String, value: String, message: String = "") {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        if (message != "") {
            editor.putString("message", message)
        }
        editor.apply()
    }


    override fun onDestroy() {
        Log.i("DeviceBackgroundService", "Service stopped")
        super.onDestroy()
    }
}
