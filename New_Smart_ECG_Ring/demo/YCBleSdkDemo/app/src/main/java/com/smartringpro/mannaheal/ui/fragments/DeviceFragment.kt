package com.smartringpro.mannaheal.ui.fragments

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.smartringpro.mannaheal.BleHelperActivity
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.databinding.FragmentDeviceBinding
import com.smartringpro.mannaheal.ui.activities.DeviceActivity
//import com.smartringpro.mannaheal.helper.AutoTestConfigSyncHelper
//import com.smartringpro.mannaheal.services.BackgroundService
//import com.smartringpro.mannaheal.services.RingConnectionStatus
import com.smartringpro.mannaheal.util.ApplicationPreferences
import com.smartringpro.mannaheal.util.ConnectionPreferences
//import com.vanzoo.ble.BleApplication
//import com.vanzoo.ble.bean.AutoTestConfig
//import com.vanzoo.ble.helper.BleConnectHelper
//import com.vanzoo.ble.remote.BleCallBack

var Extra_macAddress: String? = null
var Extra_name: String? = null

class DeviceFragment : Fragment() {

//    private var bleConnectHelper: BleConnectHelper = BleApplication.getBleConnectHelper()!!
//    private lateinit var autoTestConfigSyncHelper: AutoTestConfigSyncHelper

    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var bluetoothResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkBluetoothAndRequestEnable()
        } else {
            Toast.makeText(requireContext(), "Bluetooth permission denied", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private var hasRetriedConnection = false
    private var currentStatus: String = ""
    private var retryCount = 0
    private val maxRetries = 2

    private var isManualDisconnectUI = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the Bluetooth adapter
        val manager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = manager.adapter

        bluetoothResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.i("Device Fragment", "Bluetooth enabled successfully")
            } else {
                Log.i("Device Fragment", "Bluetooth enabling failed or cancelled")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val manager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = manager.adapter
        checkBluetoothPermissionAndProceed()
        Extra_macAddress = ConnectionPreferences.getMacAddress(requireContext())
        Extra_name = ConnectionPreferences.getDeviceName(requireContext())
        _binding = FragmentDeviceBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.tempUnitSetting.setOnClickListener {
            showTemperatureUnitPopup(it)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.bindDevice.setOnClickListener {
            val intent = Intent(activity, BleHelperActivity::class.java)
            startActivity(intent)
        }

        binding.disConnectionBtn.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Hint")
                .setMessage("Are you sure you want to unpair the device?")
                .setPositiveButton("Sure") { dialog, _ ->
                    showLoadingDialog("Disconnecting, please wait...", 4000)
                    isManualDisconnectUI = true
                    Extra_macAddress = null
                    Extra_name = null

//                    val intent = Intent(requireContext(), BackgroundService::class.java)
//                    intent.action = "ACTION_DISCONNECT"
//                    requireContext().startService(intent)

                    binding.connectionStatus.text = "Disconnecting..."
                    binding.disConnectionBtn.isEnabled = false

                    dialog.dismiss()

                    Handler(Looper.getMainLooper()).postDelayed({
                        connectionCheck()
                    }, 3000)

                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        getTemperatureUnit()
        connectionCheck()
//        initObservers()

    }

    private fun showTemperatureUnitPopup(view: View) {
        // Create a PopupMenu
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_temperature_unit, popup.menu)

        // Set item click listener
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.unit_celsius -> {
                    saveTemperatureUnit("Celsius degrees")
                    applyTemperatureUnit("Celsius degrees")
                    true
                }

                R.id.unit_fahrenheit -> {
                    saveTemperatureUnit("Fahrenheit")
                    applyTemperatureUnit("Fahrenheit")
                    true
                }

                else -> false
            }
        }
        // Show the popup menu
        popup.show()
    }

    private fun saveTemperatureUnit(unit: String) {

    }

    private fun getTemperatureUnit() {
        val unit = ApplicationPreferences.getString(
            requireContext(),
            "temperature_unit",
            "Celsius degrees",
            "UserPreferences"
        )
        applyTemperatureUnit(unit)
    }

    private fun applyTemperatureUnit(unit: String) {
        if (unit == "Fahrenheit") {
            "Fahrenheit (°F)".also { binding.tvTemperatureUnit.text = it }
        } else {
            "Celsius degrees (°C)".also { binding.tvTemperatureUnit.text = it }
        }
    }

    private fun connectionCheck() {
        if (_binding == null || !isAdded) return

//        Log.i("Device Fragment", "Connection check: ${bleConnectHelper.isConnected}")
        Log.i("Device Fragment", "Connected device: $Extra_name $Extra_macAddress")
        if (Extra_macAddress != null) {

            binding.bindDevice.visibility = View.GONE
            binding.ringName.text = Extra_name
            binding.macId.text = Extra_macAddress

//            if (!bleConnectHelper.isConnected) {
//                scanAndConnect(Extra_macAddress!!)
//                "Connecting...".also { binding.connectionStatus.text = it }
//            } else {
//                "Connected".also { binding.connectionStatus.text = it }
//
//                setupBatteryInfo()
//
//                autoTestConfigSyncHelper = AutoTestConfigSyncHelper(requireContext(), bleConnectHelper)
//                autoTestConfigSyncHelper.syncFromApiAndUpdateRing()
//
//                bleConnectHelper.getDeviceHardWareVersion(object : BleCallBack<String> {
//                    override fun result(data: String) {
//                        if (!isAdded || isDetached) return
//                        binding.firmWareVersion.text = data
//                        ApplicationPreferences.putString(requireContext(), "firmwareVersion", data)
//                        Log.i("Device Fragment", "Result: firmware hardware version: $data")
//                    }
//                })
//            }
        } else {
            binding.bindDevice.visibility = View.VISIBLE
            binding.deviceConnectedView.visibility = View.GONE
            binding.deviceSettingsView.visibility = View.GONE
            binding.firmWareDetails.visibility = View.GONE
            binding.disConnectionBtn.visibility = View.GONE
        }
    }

//    private fun setupBatteryInfo() {
//        val macAddress = Extra_macAddress
//        if (macAddress.isNullOrEmpty()) return
//
//        if (bleConnectHelper.isConnected) {
//            bleConnectHelper.getBatteryLevel(object : BleCallBack<Int> {
//                override fun result(data: Int) {
//                    if (!isAdded || isDetached) return
//                    Log.i("Device Fragment", "Battery fetched: $data%")
//                    updateBatteryIcon(data)
//                    ApplicationPreferences.putString(requireContext(), "battery", data.toString())
//                }
//            })
//
//            bleConnectHelper.listenBatteryLevel(object : BleCallBack<Int> {
//                override fun result(data: Int) {
//                    if (!isAdded || isDetached) return
//                    Log.i("Device Fragment", "Battery update received: $data%")
//                    updateBatteryIcon(data)
//                    ApplicationPreferences.putString(requireContext(), "battery", data.toString())
//                }
//            })
//        } else {
//            val savedBattery = ApplicationPreferences.getString(requireContext(), "battery", "")
//            if (savedBattery.isNotEmpty()) {
//                Log.i("Device Fragment", "Battery from preferences: $savedBattery%")
//                updateBatteryIcon(savedBattery.toIntOrNull() ?: return)
//            }
//        }
//    }

    private fun scanAndConnect(mac: String) {
        if (mBluetoothAdapter == null) {
            Toast.makeText(
                requireContext(),
                "Bluetooth not supported or not initialized",
                Toast.LENGTH_SHORT
            ).show()
            Log.e("Device Fragment", "BluetoothAdapter is null")
            return
        }

        if (!mBluetoothAdapter.isEnabled) {
            Toast.makeText(requireContext(), "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            Log.e("Device Fragment", "Bluetooth is not enabled")
            return
        }

        Log.i("Device Fragment", "Attempting to connect to BLE device with MAC: $mac")

        Handler(Looper.getMainLooper()).postDelayed({
            Log.i("Device Fragment", "Calling connectionBle() after 1 sec delay")
            connectionBle()
        }, 1000)
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun connectionBle() {
        Log.i("Device Fragment", "connectionBle check: $Extra_name")
        if (Extra_name?.isEmpty() == true) return

//        Log.i("Device Fragment", "bleConnectHelper connected check: $Extra_name")
//        val intent = Intent(requireContext(), BackgroundService::class.java).apply {
//            putExtra("Extra_MacAddress", Extra_macAddress)
//            putExtra("Extra_name", Extra_name)
//        }
//
//        if (isServiceRunning(BackgroundService::class.java)) {
//            if (!bleConnectHelper.isConnected) {
//                // Removed stopService(intent) to avoid race condition
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    requireContext().startForegroundService(intent)
//                } else {
//                    requireContext().startService(intent)
//                }
//                Log.i("Device Fragment", "Service restarted as it was running but not connected")
//            } else {
//                Log.i("Device Fragment", "Service already running and connected")
//            }
//        } else {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                requireContext().startForegroundService(intent)
//            } else {
//                requireContext().startService(intent)
//            }
//            Log.i("Device Fragment", "Service not running, starting new service")
//        }

        ConnectionPreferences.saveConnectionState(
            requireContext(),
            true,
            Extra_macAddress,
            Extra_name
        )
    }

//    private fun initObservers() {
//        retryCount = 0
//
//        RingConnectionStatus.observe(viewLifecycleOwner) { status ->
//            if (!isAdded) return@observe
//            Log.i("DeviceFragment", "RingConnectionStatus: $status")
//            currentStatus = status
//
//            when (status) {
//                "Connected" -> {
//                    binding.connectionStatus.text = "Connected"
//                    retryCount = 0
//                    connectionCheck()
//                }
//
//                "Failed" -> {
//                    if (retryCount > 0) {
//                        binding.connectionStatus.text = "Device not found"
//                    } else {
//                        startRetryLoop()
//                    }
//                }
//
//                "Disconnected" -> {
//                    if (isManualDisconnectUI) {
//                        isManualDisconnectUI = false
//                    } else {
//                        if (retryCount < maxRetries) {
//                            binding.connectionStatus.text = "Connecting..."
//                            startRetryLoop()
//                        } else {
//                            binding.connectionStatus.text = "Device not found"
//                        }
//                    }
//                }
//
//                else -> {
//                    if (retryCount < maxRetries) {
//                        binding.connectionStatus.text = "Connecting..."
//                        startRetryLoop()
//                    } else {
//                        binding.connectionStatus.text = "Device not found"
//                    }
//                }
//            }
//        }
//    }

    private fun startRetryLoop() {
        retryCount++
        Handler(Looper.getMainLooper()).postDelayed({
            if (_binding == null || !isAdded) return@postDelayed
            if (currentStatus != "Connected") {
                Log.i("DeviceFragment", "Retrying connection (attempt $retryCount)")
                connectionCheck()
            }
        }, 10_000)
    }

    private fun updateBatteryIcon(batteryPercentage: Int) {
        binding.batteryPercentage.text = "$batteryPercentage%"
        val drawable =
            ContextCompat.getDrawable(requireContext(), R.drawable.baseline_battery_0_bar_24)

        val color = when {
            batteryPercentage > 30 -> Color.parseColor("#75F94C")
            batteryPercentage > 10 -> Color.parseColor("#FFA500")
            else -> Color.RED
        }
        DrawableCompat.setTint(drawable!!, color)
        binding.batteryIcon.setImageDrawable(drawable)
    }

    private fun checkBluetoothPermissionAndProceed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
        }
        checkBluetoothAndRequestEnable()
    }

    private fun checkBluetoothAndRequestEnable() {
        if (!mBluetoothAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothResultLauncher.launch(intent)
        }

    }

    private fun showLoadingDialog(message: String, durationMillis: Long = 3000) {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.loading_dialog, null)
        val loadingText = dialogView.findViewById<TextView>(R.id.loading_text)
        loadingText.text = message

        val dialog = Dialog(requireContext(), android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(dialogView)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            loadingText.text = "Disconnected"

            // Give user a moment to see it (e.g., 800ms) then dismiss
            Handler(Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
            }, 800)
        }, durationMillis)
    }

//    private fun showDisconnectedDialog(context: Context) {
//        val dialogView = LayoutInflater.from(context).inflate(R.layout.disconnect_alert, null)
//        val dialog = AlertDialog.Builder(context)
//            .setView(dialogView)
//            .setCancelable(false)
//            .create()
//
//        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//
//        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
//            dialog.dismiss()
//        }
//
//        dialogView.findViewById<Button>(R.id.btn_sure).setOnClickListener {
//            val bluetoothIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
//            context.startActivity(bluetoothIntent)
//            dialog.dismiss()
//        }
//
//        dialog.show()
//    }

    override fun onDestroy() {
//        if (bleConnectHelper.isConnected) {
//            bleConnectHelper.listenBatteryLevel(null)
//        }
        _binding = null
        super.onDestroy()
    }
}