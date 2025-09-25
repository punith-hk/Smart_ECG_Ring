package com.smartringpro.mannaheal.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.databinding.FragmentSleepBinding
import com.yucheng.ycbtsdk.Constants
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.response.BleDataResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SleepFragment : Fragment() {
    private var _binding: FragmentSleepBinding? = null
    private val binding get() = _binding!!

    private val TAG = "SleepFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSleepBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Call immediately when fragment loads
        fetchSleepHistory()
    }

    private fun fetchSleepHistory() {
        YCBTClient.healthHistoryData(0x0504, object : BleDataResponse {
            override fun onDataResponse(
                responseCode: Int,
                ratio: Float,
                resultMap: HashMap<*, *>?
            ) {
                Log.i(TAG, "ResponseCode=$responseCode, ratio=$ratio, data=$resultMap")

                if (responseCode != 0 || resultMap == null) {
                    Log.w(TAG, "Sleep data fetch failed or result is null")
                    return
                }

                val dataList = resultMap["data"] as? List<HashMap<String, Any>>
                if (dataList.isNullOrEmpty()) {
                    Log.i(TAG, "No sleep data available")
                    return
                }

                dataList.forEach { item ->
                    val startTime = (item["sleepStartTime"] as? Long)?.let {
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(Date(it))
                    }
                    val duration = item["sleepDuration"] as? Int
                    val sleepState = item["sleepState"] as? Int

                    Log.i(
                        TAG,
                        "Sleep -> Start=$startTime, Duration=$duration min, State=$sleepState"
                    )
                }
            }
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

