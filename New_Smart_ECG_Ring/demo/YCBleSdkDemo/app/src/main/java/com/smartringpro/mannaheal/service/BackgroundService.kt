package com.smartringpro.mannaheal.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.smartringpro.mannaheal.workers.OtherVitalsSyncWorker
import com.smartringpro.mannaheal.workers.TemperatureWorker
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.helper.AutoSyncHelper
import com.smartringpro.mannaheal.model.ConnectEvent
import com.smartringpro.mannaheal.util.ConnectionPreferences
import com.smartringpro.mannaheal.util.UserAppDetailSender
import com.smartringpro.mannaheal.workers.AutoSyncWorker
import com.yucheng.ycbtsdk.Constants.BLEState
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.response.BleConnectResponse
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit

class BackgroundService : Service() {
    private var macAddress: String? = null
    private var macName: String? = null

    private lateinit var mBluetoothAdapter: BluetoothAdapter

    private var pendingConnect: Boolean = false
    private var currentStatus: String = ""

    private var isManualDisconnect = false
    private var isBleConnected: Boolean = false

    private var isTaskRunning = false

    override fun onCreate() {
        super.onCreate()
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        YCBTClient.registerBleStateChange(bleConnectCallback)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(
            "Device Service",
            "onStartCommand called, about to start foreground notification"
        )
        try {
            startForegroundServiceNotification("Starting...", false)
            Log.i(
                "Device Service",
                "startForegroundServiceNotification called successfully"
            )
        } catch (e: Exception) {
            Log.e("Device Service", "Exception in startForegroundServiceNotification", e)
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
            intent?.getStringExtra("Device_MacAddress") ?: ConnectionPreferences.getMacAddress(
                applicationContext
            )
        macName = intent?.getStringExtra("Device_name") ?: ConnectionPreferences.getDeviceName(
            applicationContext
        )

        Log.i("Device Service", "Service started $macAddress")

        isBleConnected = YCBTClient.connectState() == BLEState.ReadWriteOK

        if (!isBleConnected && macAddress != null && macName != null) {
            Log.i("Device Service", "on start connection $macAddress")
            connectToDevice(macAddress!!, macName!!)
        }

        if (isBleConnected) {
            startForegroundServiceNotification("Connected to $macName", true)
            startRepeatingTask()
        } else {
            startForegroundServiceNotification("Device not connected", false)
        }

        return START_STICKY
    }

    private fun startRepeatingTask() {
        if (!isTaskRunning) {
            isTaskRunning = true

            startAutoSyncWorkers()
        }
    }

    private fun startAutoSyncWorkers() {
        val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        // 15-min Heart Rate Worker
        val heartRateWork = PeriodicWorkRequestBuilder<AutoSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraint)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "HeartRateSyncWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            heartRateWork
        )

        // 1-hour Other Vitals Worker
        val otherVitalsWork = PeriodicWorkRequestBuilder<OtherVitalsSyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(networkConstraint)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OtherVitalsSyncWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            otherVitalsWork
        )

        // 3-hour Temperature Worker
        val temperatureWork = PeriodicWorkRequestBuilder<TemperatureWorker>(3, TimeUnit.HOURS)
            .setConstraints(networkConstraint)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TemperatureWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            temperatureWork
        )
    }

    private fun disconnectAndStop() {
        Log.i("Device Service", "disconnectAndStop()")
        isManualDisconnect = true
        isBleConnected = false
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
                    "Device Service",
                    "BLUETOOTH_CONNECT permission not granted. Cannot connect."
                )
                startForegroundServiceNotification("Bluetooth permission not granted", true)
                return
            }
        }

        setAppPref("bluetooth", "1")

        macAddress?.let { mac ->
            Log.i("Device Service", "MAC available: $mac — connecting directly")
            YCBTClient.connectBle(mac, bleConnectCallback)

            macName?.let { name ->
                if (!ConnectionPreferences.isDeviceSaved(applicationContext, mac)) {
                    ConnectionPreferences.saveDevice(applicationContext, mac, name)
                    Log.i("Device Service", "Device saved: $mac / $name")
                }
            }

        }
    }

    private val bleConnectCallback = BleConnectResponse { code ->
        Log.i("Device Service", "BLE state changed: $code")

        when (code) {
            BLEState.ReadWriteOK -> {
                Log.i("Device Service", "Device connected")
            }

            BLEState.Disconnect -> {
                Log.i("Device Service", "Device disconnected")
            }

            BLEState.Connecting -> {
                Log.i("Device Service", "Connecting...")
            }

            else -> {
                Log.i("Device Service", "Device not found / error")
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBleConnectEvent(event: ConnectEvent) {
        val state = event.state
        Log.i("Device Service callback", "Received ConnectEvent: state=$state")

        when (state) {
            1 -> { // Connected
                isBleConnected = true
                Log.i("Device Service callback", "Device connected")
                ConnectionPreferences.saveConnectionState(
                    applicationContext,
                    true,
                    macAddress,
                    macName
                )
                startForegroundServiceNotification("Connected to $macName", true)
                startRepeatingTask()
                UserAppDetailSender.sendUserAppDetail(applicationContext, false)

                val autoSyncHelper = AutoSyncHelper(applicationContext)
                autoSyncHelper.heartDataSync()

                Toast.makeText(applicationContext, "Connected to $macName", Toast.LENGTH_SHORT).show()
            }

            0 -> { // Disconnected (out of range, battery, etc.)
                isBleConnected = false
                Log.i("Device Service callback", "Device disconnected (out of range / battery / other)")
                startForegroundServiceNotification("Device not connected", false)
                Toast.makeText(applicationContext, "Disconnected", Toast.LENGTH_SHORT).show()
            }

            else -> { // Any other state (optional logging)
                Log.i("Device Service callback", "Other BLE state received: $state")
            }
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

        // Always use high priority for visibility
        val priority = NotificationCompat.PRIORITY_MAX

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
        Log.i("Device Service", "Service stopped")
        YCBTClient.unRegisterBleStateChange(bleConnectCallback)
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onDestroy()
    }
}
