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
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.databinding.FragmentDeviceBinding
import com.smartringpro.mannaheal.model.ConnectEvent
import com.smartringpro.mannaheal.service.BackgroundService
import com.smartringpro.mannaheal.ui.activities.DeviceActivity
import com.smartringpro.mannaheal.util.ApplicationPreferences
import com.smartringpro.mannaheal.util.ConnectionPreferences
import com.yucheng.ycbtsdk.Constants
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.response.BleConnectResponse
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

var Extra_macAddress: String? = null
var Extra_name: String? = null

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

    private var hasRetriedConnection = false
    private var currentStatus: String = ""
    private var retryCount = 0
    private val maxRetries = 2

    private var isManualDisconnectUI = false
    private var isBleConnected = false

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
                    Extra_macAddress = null
                    Extra_name = null

                    binding.connectionStatus.text = "Disconnecting..."
                    binding.disConnectionBtn.isEnabled = false

                    YCBTClient.disconnectBle()

                    ConnectionPreferences.saveConnectionState(requireContext(), false, null, null)

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

    private fun connectionCheck() {
        if (_binding == null || !isAdded) return
        Log.i("Device Fragment", "Connected device: $Extra_name $Extra_macAddress")
        if (Extra_macAddress != null && Extra_name != null && Extra_macAddress!!.isNotEmpty() && Extra_name!!.isNotEmpty()) {
            binding.bindDevice.visibility = View.GONE
            binding.ringName.text = Extra_name
            binding.macId.text = Extra_macAddress
            binding.deviceConnectedView.visibility = View.VISIBLE
            binding.deviceSettingsView.visibility = View.VISIBLE
            binding.firmWareDetails.visibility = View.VISIBLE
            binding.disConnectionBtn.visibility = View.VISIBLE

            if (!isBleConnected) {
                binding.connectionStatus.text = "Connecting..."
                startBackgroundServiceForConnection(Extra_macAddress!!, Extra_name!!)
            } else {
                binding.connectionStatus.text = "Connected"
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
        val intent = Intent(requireContext(), com.smartringpro.mannaheal.service.BackgroundService::class.java).apply {
            putExtra("Extra_MacAddress", mac)
            putExtra("Extra_name", name)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
        Log.i("Device Fragment", "Started BackgroundService for BLE connection: $mac $name")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBleConnectEvent(event: com.smartringpro.mannaheal.model.ConnectEvent) {
        isBleConnected = event.state == com.yucheng.ycbtsdk.Constants.BLEState.Connected || event.state == 1
        when (event.state) {
            com.yucheng.ycbtsdk.Constants.BLEState.Connected, 1 -> {
                binding.connectionStatus.text = "Connected"
                Toast.makeText(
                    requireContext(),
                    getString(com.smartringpro.mannaheal.R.string.connect_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
            com.yucheng.ycbtsdk.Constants.BLEState.Disconnect, 3 -> {
                binding.connectionStatus.text = "Disconnected"
                Toast.makeText(
                    requireContext(),
                    "Disconnected",
                    Toast.LENGTH_SHORT
                ).show()
            }
            com.yucheng.ycbtsdk.Constants.BLEState.Connecting, 5 -> {
                binding.connectionStatus.text = "Connecting..."
            }
            else -> {
                binding.connectionStatus.text = "Other"
            }
        }
    }


//    private fun connectionBle() {
//        Log.i("Device Fragment", "connectionBle check: ${com.smartringpro.mannaheal.ui.fragments.Extra_name}")
//        if (com.smartringpro.mannaheal.ui.fragments.Extra_name?.isEmpty() == true) return
//
//        Log.i("Device Fragment", "bleConnectHelper connected check: ${com.smartringpro.mannaheal.ui.fragments.Extra_name}")
//        val intent = Intent(requireContext(), BackgroundService::class.java).apply {
//            putExtra("Extra_MacAddress", com.smartringpro.mannaheal.ui.fragments.Extra_macAddress)
//            putExtra("Extra_name", com.smartringpro.mannaheal.ui.fragments.Extra_name)
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
//
//        ConnectionPreferences.saveConnectionState(
//            requireContext(),
//            true,
//            com.smartringpro.mannaheal.ui.Extra_macAddress,
//            com.smartringpro.mannaheal.ui.Extra_name
//        )
//    }



    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
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