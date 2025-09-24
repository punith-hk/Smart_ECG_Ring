package com.smartringpro.mannaheal.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.userHealthData.UserHealthDataRepository
import com.yucheng.ycbtsdk.Constants
import com.yucheng.ycbtsdk.Constants.BLEState.ReadWriteOK
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.response.BleDataResponse
import kotlin.collections.get

class HealthDataFragment : Fragment() {

    private lateinit var tempUnit: String
    private var testType: String? = null

    private var testCode: Int? = null

    private var testUnit: String? = null

    private var userId: Int = -1

    private val repository = UserHealthDataRepository()

    private val testTypeToCodeMap = mapOf(
        "heart_rate" to 0x00,
        "blood_oxygen" to 0x01,
        "blood_pressure" to 0x02,
        "temperature" to 0x03,
        "ambient_temperature" to 0x04,
        "calories" to null,
        "sleep" to null,
        "hrv" to 0x05,
        "stress" to 0x06,
        "ecg" to null
    )

    private val testTypeToHistoryMap = mapOf(
        "heart_rate" to Constants.DATATYPE.Health_HistoryHeart,
        "blood_oxygen" to Constants.DATATYPE.Health_HistoryBloodOxygen,
        "blood_pressure" to Constants.DATATYPE.Health_HistoryBlood,
        "temperature" to Constants.DATATYPE.Health_HistoryTemp,
        "ambient_temperature" to Constants.DATATYPE.Health_HistoryTempAndHumidity,
        "sleep" to Constants.DATATYPE.Health_HistorySleep
        // add more if needed
    )

    private val testTypeToUnitMap = mapOf(
        "heart_rate" to "times/min",
        "blood_oxygen" to "%",
        "blood_pressure" to "mmHg",
        "temperature" to "",
        "ambient_temperature" to "",
        "calories" to "kcal",
        "sleep" to "hour/min",
        "hrv" to "times/min",
        "stress" to "",
        "ecg" to "BPM"
    )

    private val testTypeToIconMap = mapOf(
        "heart_rate" to R.drawable.baseline_favorite_24,
        "blood_oxygen" to R.drawable.baseline_favorite_24,
        "blood_pressure" to R.drawable.baseline_favorite_24,
        "temperature" to R.drawable.baseline_device_thermostat_24,
        "ambient_temperature" to R.drawable.baseline_device_thermostat_24,
        "calories" to R.drawable.baseline_local_fire_department_24,
        "sleep" to R.drawable.baseline_airline_seat_individual_suite_24,
        "hrv" to R.drawable.baseline_favorite_24,
        "stress" to R.drawable.baseline_favorite_24,
        "ecg" to R.drawable.baseline_favorite_24
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            testType = it.getString("type") // Retrieve the type argument
        }
//        testCode = testTypeToCodeMap[testType]

        testCode = testTypeToHistoryMap[testType]
        testUnit = testTypeToUnitMap[testType]

        val sharedPreferences =
            requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        tempUnit =
            sharedPreferences.getString("temperature_unit", "Celsius degrees") ?: "Celsius degrees"
        Log.i("Health Fragment", "$testType, $testCode, $testUnit")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_health_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch health data only if we know what type
        testCode?.let { code ->
            Log.i("Health Fragment", "Fetching history for $testType ($code)")

            YCBTClient.healthHistoryData(code, object : BleDataResponse {
                override fun onDataResponse(responseCode: Int, ratio: Float, resultMap: HashMap<*, *>) {
                    this@HealthDataFragment.onDataResponse(responseCode, ratio, resultMap)
                    Log.i("Health Fragment", "ResponseCode=$responseCode, ratio=$ratio, data=$resultMap")
                }
            })
        } ?: run {
            Log.w("Health Fragment", "No matching history code for type=$testType")
        }
    }

    private fun onDataResponse(responseCode: Int, ratio: Float, resultMap: HashMap<*, *>) {
        if (responseCode != 0) {
            Log.w("HealthDataFragment", "Failed to get data: code=$responseCode")
            return
        }

        val dataList = resultMap["data"] as? List<HashMap<String, Any>>
        if (dataList.isNullOrEmpty()) {
            Log.i("HealthDataFragment", "No data available for $testType")
            return
        }

        when (testType) {
            "heart_rate" -> {
                dataList.forEach { item ->
                    val value = item["heartValue"] as? Int
                    val timestamp = item["heartStartTime"] as? Long
                    val formattedTime = timestamp?.let {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(it))
                    }
                    Log.i("HealthDataFragment", "HeartRate=$value BPM at $formattedTime")
                }
            }

            "blood_pressure" -> {
                dataList.forEach { item ->
                    val sbp = item["bloodSBP"] as? Int // systolic
                    val dbp = item["bloodDBP"] as? Int // diastolic
                    val timestamp = item["bloodStartTime"] as? Long
                    val formattedTime = timestamp?.let {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(it))
                    }
                    Log.i("HealthDataFragment", "BP=$sbp/$dbp mmHg at $formattedTime")
                }
            }

            else -> {
                Log.w("HealthDataFragment", "Unsupported type=$testType, raw data=$resultMap")
            }
        }
    }

    /** 🔹 Live blood pressure measurement (current values, not history) */
    private fun startLiveBloodPressureMeasurement() {
        if (YCBTClient.connectState() == ReadWriteOK) {
            Log.i("Health Fragment", "Starting live blood pressure measurement...")
            YCBTClient.appStartBloodMeasurement(
                0, 0, 0, 0, 0, 0, 0, 0
            ) // all params 0 (optional, can extend later)
            { code, ratio, resultMap ->
                if (code == 0 && resultMap != null && (resultMap["typeResult"] as? Int) == 0) {
                    Toast.makeText(
                        requireContext(),
                        "Blood Pressure measurement started",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.i("Health Fragment", "BP Live Result: $resultMap")
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to start BP measurement",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.w("Health Fragment", "BP Live measurement failed: $resultMap")
                }
            }
        } else {
            Toast.makeText(requireContext(), "Disconnected, please reconnect", Toast.LENGTH_SHORT).show()
            Log.w("Health Fragment", "Device not connected")
        }
    }


}
