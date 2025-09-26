package com.smartringpro.mannaheal.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.databinding.FragmentHomeBinding
import com.smartringpro.mannaheal.ui.activities.HomeActivity
import com.yucheng.ycbtsdk.Constants.BLEState
import com.yucheng.ycbtsdk.YCBTClient

class HomeFragment : Fragment() {

    //    private var bleConnectHelper: BleConnectHelper = BleApplication.getBleConnectHelper()!!
    private var isBleConnected = false
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var tempUnit: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        isBleConnected = YCBTClient.connectState() == BLEState.ReadWriteOK

        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        tempUnit =
            sharedPreferences.getString("temperature_unit", "Celsius degrees") ?: "Celsius degrees"
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        tempUnit =
            sharedPreferences.getString("temperature_unit", "Celsius degrees") ?: "Celsius degrees"
        val lastUserData = sharedPreferences.getString("lastUserData", "")
//        if (lastUserData != "") {
//            val data = Gson().fromJson(lastUserData, LastDataBean::class.java)
//            setData(data, false)
//        }
        applyTemperatureUnit(0f)

        val progressBar = binding.progressCircular
        val progressPercentage = binding.tvProgressPercentage

        val btnCalories = binding.caloriesCard
        val btnSleepCard = binding.sleepCard
        val btnHeartRate = binding.heartRateCard
        val btnBloodOxygen = binding.bloodOxygenCard
        val btnBloodPressure = binding.bloodPressureCard
        val btnBodyTemp = binding.temperatureCard
        val btnAmbientTemp = binding.ambientTemperatureCard
        val btnEcg = binding.ecgCard
        val btnHrv = binding.hrvCard
        val btnStress = binding.stressCard

        val stepsTaken = 0
        val stepsGoal = 10000
        val progressValue = (stepsTaken.toFloat() / stepsGoal * 100).toInt()
        setIcons()
//        getLastData()
        // Update the CircularProgressBar and TextView
        progressBar.setProgressWithAnimation(
            progressValue.toFloat(), // Progress value as float
            1000 // Animation duration in milliseconds
        )
        "$progressValue%".also { progressPercentage.text = it }

        btnHeartRate.setOnClickListener {
            (activity as HomeActivity).openHealthDataFragment("heart_rate", "Heart Rate")
        }
        btnBloodOxygen.setOnClickListener {
            (activity as HomeActivity).openHealthDataFragment("blood_oxygen", "Blood Oxygen")
        }
        btnBloodPressure.setOnClickListener {
            (activity as HomeActivity).openHealthDataFragment("blood_pressure", "Blood pressure")
        }
        btnBodyTemp.setOnClickListener {
            (activity as HomeActivity).openHealthDataFragment("temperature", "Temperature")
        }
        btnAmbientTemp.setOnClickListener {
            (activity as HomeActivity).openHealthDataFragment(
                "ambient_temperature",
                "Ambient temperature"
            )
        }
        btnCalories.setOnClickListener {
            (activity as HomeActivity).openFragment(CaloriesFragment(), "Calories", true)
        }
        btnSleepCard.setOnClickListener {
            (activity as HomeActivity).openFragment(SleepFragment(), "Sleep", true)
        }
        btnEcg.setOnClickListener {
            (activity as HomeActivity).openHealthDataFragment("ecg", "ECG")
        }
        btnHrv.setOnClickListener {
            (activity as HomeActivity).openHealthDataFragment("hrv", "Heart rate variability")
        }
        btnStress.setOnClickListener {
            (activity as HomeActivity).openHealthDataFragment("stress", "Stress")
        }

//        RingConnectionStatus.observe(viewLifecycleOwner) { event ->
//            Log.i("HomeFragment", "RingConnectionStatus $event")
//            if (event == "Connected") {
//                getLastData()
//            }
//        }

        isBleConnected()
    }

    private fun applyTemperatureUnit(data: Float, ambient: Boolean = false): String {
        if (data == 0f) {
            val unitSymbol = if (tempUnit == "Fahrenheit") "°F" else "°C"
            return "--$unitSymbol"
        }
        val (convertedData, unitSymbol) = if (tempUnit == "Fahrenheit") {
            if (data < 45f) {
                Pair(celsiusToFahrenheit(data), "°F")
            } else {
                Pair(data, "°F")
            }
        } else {
            if (ambient) {
                if (data > 45f) {
                    Pair(fahrenheitToCelisius(data), "°C")
                } else {
                    Pair(data, "°C")
                }
            } else {
                Pair(data, "°C")
            }
        }
        return "$convertedData$unitSymbol"
    }

    private fun fahrenheitToCelisius(data: Float): Float {
        return ((data - 32) * 5 / 9).toBigDecimal().setScale(1, java.math.RoundingMode.HALF_EVEN)
            .toFloat()
    }

    private fun celsiusToFahrenheit(celsius: Float): Float {
        return (celsius * 9 / 5 + 32).toBigDecimal().setScale(1, java.math.RoundingMode.HALF_EVEN)
            .toFloat()
    }

//    private fun getLastData() {
//        if (isBleConnected()) {
//            bleConnectHelper.getLastData(object : BleCallBack<LastDataBean> {
//                override fun result(data: LastDataBean) {
//                    if (!isAdded || isDetached) return
//                    setData(data)
//                    val sb = StringBuilder()
//                    sb.append("step:${data.step},")
//                    sb.append("sleepTime:${data.sleepTime},")
//                    sb.append("ecg:${data.ecg},")
//                    sb.append("heartRate:${data.heartRate},")
//                    sb.append("bloodOxygen=${data.bloodOxygen},")
//                    sb.append("bloodHighPressure-高:${data.bloodHighPressure},")
//                    sb.append("bloodLowPressure-低:${data.bloodLowPressure},")
//                    sb.append("运动距离:${data.distance},")
//                    sb.append("当日卡路里:${data.totalCalorie},")
//                    sb.append("当日距离:${data.totalDistance},")
//                    sb.append("当日体温:${data.bodyTemperature}")
//
//                    Log.i("HomeFragment", "$sb")
//                }
//
//            })
//        }
//    }

//    private fun setData(data: LastDataBean, saveData: Boolean = true) {
//
//        val sharedPreferences: SharedPreferences =
//            requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
//        val aTemp = sharedPreferences.getString("ambient_temperature", "32.7")
//        if (saveData) {
//            val editor = sharedPreferences.edit()
//            val jsonLastData = Gson().toJson(data)
//            editor.putString("lastUserData", jsonLastData)
//            editor.apply()
//            Log.i("saveLastRingData", data.toString())
//        }
//        val temp = applyTemperatureUnit(data.bodyTemperature)
//        binding.temperatureText.text = temp
//
//
//        "${data.heartRate} times/min".also { binding.heartRateText.text = it }
//        "${data.totalCalorie} kcal".also { binding.caloriesText.text = it }
//
//        val hours = "${data.sleepTime}".toInt() / 60
//        val minutes = "${data.sleepTime}".toInt() % 60
//        "$hours Hour $minutes Min".also { binding.sleepText.text = it }
//
//        "${data.bloodOxygen} %".also { binding.bloodOxygenText.text = it }
//        "${data.bloodHighPressure} /${data.bloodLowPressure} mmHg".also {
//            binding.bloodPressureText.text = it
//        }
//
//        "${data.step} Steps".also { binding.tvStepsTaken.text = it }
//
//
//
//        binding.ambientTemperatureText.text = applyTemperatureUnit(aTemp!!.toFloat(), true)
//
//        val targetStepGoal = 10000
//        val totalStepGoal = "${data.step}".toInt()
//        val stepGoal = targetStepGoal - totalStepGoal
//        "$stepGoal steps away from reaching goals".also { binding.tvStepsGoal.text = it }
//
//        val progressValue = (totalStepGoal.toFloat() / targetStepGoal * 100).toInt()
//
//        binding.progressCircular.setProgressWithAnimation(
//            progressValue.toFloat(), // Progress value as float
//            1000 // Animation duration in milliseconds
//        )
//
//        val stepGoalPct = (totalStepGoal.toDouble() / targetStepGoal.toDouble()) * 100
//        "${"%.2f".format(stepGoalPct)}%".also { binding.tvProgressPercentage.text = it }
//
//        "${data.ecg} BPM".also { binding.ecgText.text = it }
//        "${data.hrv} times".also { binding.hrvText.text = it }
//        "${data.pressure}".also { binding.stressText.text = it }
//    }

    fun setIcons() {

        val appIconPath = "https://app.mannaheal.com/app/"
        val cImageUri: Uri = Uri.parse("${appIconPath}calories.png")
        val sImageUri: Uri = Uri.parse("${appIconPath}sleep.png")
        val hImageUri: Uri = Uri.parse("${appIconPath}heart.gif")
        val hrvImageUri: Uri = Uri.parse("${appIconPath}hrv.png")
        val boImageUri: Uri = Uri.parse("${appIconPath}bo2.png")
        val bpImageUri: Uri = Uri.parse("${appIconPath}bp.png")
        val tempImageUri: Uri = Uri.parse("${appIconPath}bt.png")
        val atImageUri: Uri = Uri.parse("${appIconPath}at.png")
        val ecgImageUri: Uri = Uri.parse("${appIconPath}ecg.png")
        val stressImageUri: Uri = Uri.parse("${appIconPath}stress.png")
        try {
            Glide.with(requireContext()).load(cImageUri).into(binding.cIcon)
            Glide.with(requireContext()).load(sImageUri).into(binding.sIcon)
            Glide.with(requireContext()).load(hImageUri).into(binding.hIcon)
            Glide.with(requireContext()).load(hrvImageUri).into(binding.hrvIcon)
            Glide.with(requireContext()).load(boImageUri).into(binding.boIcon)
            Glide.with(requireContext()).load(bpImageUri).into(binding.bpIcon)
            Glide.with(requireContext()).load(tempImageUri).into(binding.tempIcon)
            Glide.with(requireContext()).load(atImageUri).into(binding.atIcon)
            Glide.with(requireContext()).load(ecgImageUri).into(binding.ecgIcon)
            Glide.with(requireContext()).load(stressImageUri).into(binding.stressIcon)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isBleConnected(): Boolean {
        return if (isBleConnected) {
            true
        } else {
            Toast.makeText(activity, getString(R.string.str_toast_no_device), Toast.LENGTH_LONG ).show()
            false
        }
    }

}

