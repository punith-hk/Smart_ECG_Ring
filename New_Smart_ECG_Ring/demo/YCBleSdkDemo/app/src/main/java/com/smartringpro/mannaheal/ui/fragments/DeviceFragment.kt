package com.smartringpro.mannaheal.ui.fragments

import android.Manifest
import android.app.Activity
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
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.databinding.FragmentDeviceBinding
import com.smartringpro.mannaheal.model.ConnectEvent
import com.smartringpro.mannaheal.service.BackgroundService
import com.smartringpro.mannaheal.ui.activities.DeviceActivity
import com.smartringpro.mannaheal.util.ApplicationPreferences
import com.smartringpro.mannaheal.util.ConnectionPreferences
import com.yucheng.ycbtsdk.Constants.BLEState
import com.yucheng.ycbtsdk.YCBTClient
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DeviceFragment : Fragment() {
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
    private var deviceMacAddress: String? = null
    private var deviceName: String? = null

    private var isBleConnected = false
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

        deviceMacAddress = ConnectionPreferences.getMacAddress(requireContext())
        deviceName = ConnectionPreferences.getDeviceName(requireContext())

        if (deviceMacAddress.isNullOrEmpty() || deviceName.isNullOrEmpty()) {
            deviceMacAddress = null
            deviceName = null
        }

        Log.i("Device Fragment", "Connected device: $deviceName $deviceMacAddress")

        Log.i("Device Fragment", "Connection state: ${YCBTClient.connectState()} ")

        isBleConnected = YCBTClient.connectState() == BLEState.ReadWriteOK

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
            val intent = Intent(activity, DeviceActivity::class.java)
            startActivity(intent)
        }

        binding.disConnectionBtn.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Hint")
                .setMessage("Are you sure you want to unpair the device?")
                .setPositiveButton("Sure") { dialog, _ ->
                    showLoadingDialog("Disconnecting, please wait...", 4000)
                    isManualDisconnectUI = true
                    deviceMacAddress = null
                    deviceName = null

                    val intent = Intent(requireContext(), BackgroundService::class.java)
                    intent.action = "ACTION_DISCONNECT"
                    requireContext().startService(intent)

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

    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onStop() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onStop()
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
        ApplicationPreferences.putString(
            requireContext(),
            "temperature_unit",
            unit,
            "UserPreferences"
        )

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

        if (deviceMacAddress != null && deviceName != null && deviceMacAddress!!.isNotEmpty() && deviceName!!.isNotEmpty()) {
            binding.bindDevice.visibility = View.GONE
            binding.ringName.text = deviceName
            binding.macId.text = deviceMacAddress
            binding.deviceConnectedView.visibility = View.VISIBLE
            binding.deviceSettingsView.visibility = View.VISIBLE
            binding.firmWareDetails.visibility = View.VISIBLE
            binding.disConnectionBtn.visibility = View.VISIBLE

            Log.i("Device Fragment", "Connected device: $isBleConnected")

            if (!isBleConnected) {
                binding.connectionStatus.text = "Connecting..."
                startBackgroundServiceForConnection(deviceMacAddress!!, deviceName!!)
            } else {
                binding.connectionStatus.text = "Connected"
                setupBatteryInfo()
                setUpFirmWare()
            }
        } else {
            binding.bindDevice.visibility = View.VISIBLE
            binding.deviceConnectedView.visibility = View.GONE
            binding.deviceSettingsView.visibility = View.GONE
            binding.firmWareDetails.visibility = View.GONE
            binding.disConnectionBtn.visibility = View.GONE
        }
    }

    private fun startBackgroundServiceForConnection(mac: String, name: String) {
        val intent = Intent(requireContext(), BackgroundService::class.java).apply {
            putExtra("Device_MacAddress", mac)
            putExtra("Device_name", name)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
        Log.i("Device Fragment", "Started BackgroundService for BLE connection: $mac $name")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBleConnectEvent(event: ConnectEvent) {
        isBleConnected = event.state == 1

        Log.i("Device fragment callback", "${event.state}")

        when (event.state) {
            0 -> { // Disconnecting
                binding.connectionStatus.text = "Disconnecting..."
            }
            1 -> { // Connected
                Log.i("Device fragment callback", "Device connected")
                binding.connectionStatus.text = "Connected"
                setupBatteryInfo()
                setUpFirmWare()
                ConnectionPreferences.saveConnectionState(
                    requireContext(),
                    true,
                    deviceMacAddress,
                    deviceName
                )
                Toast.makeText(requireContext(), getString(R.string.connect_success), Toast.LENGTH_SHORT).show()
            }
            3 -> { // Disconnected
                binding.connectionStatus.text = "Disconnected"
                Toast.makeText(requireContext(), "Disconnected", Toast.LENGTH_SHORT).show()
            }
            5 -> { // Connecting
                binding.connectionStatus.text = "Connecting..."
            }
            else -> {
                binding.connectionStatus.text = "Device not found"
            }
        }
    }

    private fun setUpFirmWare() {
        val firmWareVersion = YCBTClient.getBindDeviceVersion()
        binding.firmWareVersion.text = firmWareVersion
        ApplicationPreferences.putString(requireContext(), "firmwareVersion", firmWareVersion)
        Log.i("Device Fragment", "Result: firmware hardware version: $firmWareVersion")
    }

    private fun setupBatteryInfo() {

        val batteryValue = YCBTClient.getDeviceBatteryValue() // 0 - 100
        val batteryState = YCBTClient.getDeviceBatteryState() // 0 = normal, 1 = charging?

        Log.i("Device fragment", "Battery $batteryValue, $batteryState")

        updateBatteryIcon(batteryValue)

//        if (batteryValue >= 0) { // valid value
//
//            if (batteryState == 1) {
//                binding.batteryChargingIcon.visibility = View.VISIBLE
//            } else {
//                binding.batteryChargingIcon.visibility = View.GONE
//            }
//        }
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

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}