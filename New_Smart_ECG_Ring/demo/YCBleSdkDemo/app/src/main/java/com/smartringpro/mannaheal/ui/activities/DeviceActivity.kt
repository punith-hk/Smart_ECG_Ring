package com.smartringpro.mannaheal.ui.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.adapter.BleScanAdapter
import com.smartringpro.mannaheal.databinding.ActivityDeviceBinding
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.bean.ScanDeviceBean
import com.yucheng.ycbtsdk.response.BleScanResponse

class DeviceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeviceBinding
    private lateinit var bleScanAdapter: BleScanAdapter
    private val deviceList = ArrayList<ScanDeviceBean>()
    private val macSet = HashSet<String>()
    private val scanTime = 7000L
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bleScanAdapter = BleScanAdapter(this, deviceList)
        binding.deviceListView.layoutManager = LinearLayoutManager(this)
        binding.deviceListView.adapter = bleScanAdapter

        binding.mSwipeRefreshLayout.setOnRefreshListener {
            startScanWithUIFeedback()
        }
        binding.scanAgain.setOnClickListener {
            startScanWithUIFeedback()
        }

        // Start scan automatically on launch
        startScanWithUIFeedback()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun startScanWithUIFeedback() {
        stopScan()
        deviceList.clear()
        macSet.clear()
        bleScanAdapter.notifyDataSetChanged()
        binding.scanProgressText.text = "Scanning in progress"
        binding.searchingDeviceText.text = "Searching for available devices..."
        binding.scanAgain.isEnabled = false
        binding.mSwipeRefreshLayout.isRefreshing = true
        val blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink)
        binding.scanProgressText.startAnimation(blinkAnimation)
        startScan()
        handler.postDelayed({
            stopScan()
            binding.scanProgressText.clearAnimation()
            if (deviceList.isEmpty()) {
                binding.scanProgressText.text = "No devices found"
                binding.searchingDeviceText.text = "Please scan again"
            } else {
                binding.scanProgressText.text = "Scan complete"
                binding.searchingDeviceText.text = "Devices found: ${deviceList.size}"
            }
            binding.scanAgain.isEnabled = true
        }, scanTime)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun startScan() {
        YCBTClient.startScanBle(object : BleScanResponse {
            override fun onScanResponse(i: Int, scanDeviceBean: ScanDeviceBean?) {
                if (scanDeviceBean != null) {
                    Log.d("BLE_SCAN", "Device found: $scanDeviceBean")
                    runOnUiThread {
                        if (macSet.add(scanDeviceBean.deviceMac)) {
                            deviceList.add(scanDeviceBean)
                            bleScanAdapter.notifyItemInserted(deviceList.size - 1)
                        }
                    }
                }
            }
        }, 6)
    }

    private fun stopScan() {
        YCBTClient.stopScanBle()
        binding.mSwipeRefreshLayout.isRefreshing = false
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
