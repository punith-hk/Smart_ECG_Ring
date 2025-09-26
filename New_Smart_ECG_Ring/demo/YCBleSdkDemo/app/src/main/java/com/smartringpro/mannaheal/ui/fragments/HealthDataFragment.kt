package com.smartringpro.mannaheal.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.R.*
import com.smartringpro.mannaheal.api.userHealthData.AddUserHealthDataResponse
import com.smartringpro.mannaheal.api.userHealthData.GetUserHealthDataByDayResponse
import com.smartringpro.mannaheal.api.userHealthData.GetUserHealthDataResponse
import com.smartringpro.mannaheal.api.userHealthData.RingValueEntry
import com.smartringpro.mannaheal.api.userHealthData.UserHealthDataRepository
import com.smartringpro.mannaheal.helper.AutoTestConfigSyncHelper
import com.smartringpro.mannaheal.util.ConnectionPreferences
import com.yucheng.ycbtsdk.Constants
import com.yucheng.ycbtsdk.Constants.BLEState.ReadWriteOK
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.response.BleDataResponse
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.collections.get
import kotlin.text.toDouble
import kotlin.text.toFloat
import kotlin.toString

class DateEntry(
    x: Float, // X-axis value (e.g., day of the week)
    y: Float, // Y-axis value (e.g., health data value)
    val date: String // Date in "yyyy-MM-dd" format
) : Entry(x, y)

class HealthDataFragment : Fragment() {

    private lateinit var tempUnit: String
    private var testType: String? = null

    private var testCode: Int? = null

    private var testUnit: String? = null

    private var userId: Int = -1

    private val repository = UserHealthDataRepository()

    private var selectedHealthTypeDataList: MutableList<GetUserHealthDataResponse.GetHealthData> =
        mutableListOf()

    private var latestRingDataList: MutableList<RingValueEntry> = mutableListOf()
    private var isRingDataHandled = false

    private var selectedHealthTypeDataByDayList: MutableList<GetUserHealthDataByDayResponse.GetHealthData> =
        mutableListOf()

    private lateinit var healthDataChart: LineChart
    private lateinit var healthDataWeeklyChart: LineChart
    private lateinit var healthDataMonthlyChart: LineChart

    private lateinit var tabDay: TextView
    private lateinit var tabWeek: TextView
    private lateinit var tabMonth: TextView

    private lateinit var btnStartTest: Button
    private lateinit var btnStopTest: Button
    private lateinit var testResultValue: TextView
    private lateinit var bpTestResultValue: TextView
    private lateinit var testResultUnit: TextView
    private lateinit var chartValueUnit: TextView
    private lateinit var remainingTimeText: TextView
    private lateinit var countDownTimer: CountDownTimer

    private var remainingSeconds = 60

    private lateinit var dateText: TextView
    private lateinit var previousIcon: ImageView
    private lateinit var nextIcon: ImageView
    private lateinit var selectedChartTime: TextView
    private lateinit var selectedChartValue: TextView

    private lateinit var minValueText: TextView
    private lateinit var maxValueText: TextView
    private lateinit var averageValueText: TextView

    private lateinit var systolicBpValueText: TextView
    private lateinit var diastolicBpValueText: TextView

    private lateinit var startTestContainer: LinearLayout

    private var isDayMode = false
    private var isWeekMode = false
    private var isMonthMode = false

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    private var selectedDate: LocalDate = LocalDate.now()

    private var weekStartDate: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY)
    private var weekEndDate: LocalDate = LocalDate.now().with(DayOfWeek.SUNDAY)

    private var monthStartDate: LocalDate = LocalDate.now().withDayOfMonth(1)
    private var monthEndDate: LocalDate =
        LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())

    private var hasSyncedOnce = false

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
        "heart_rate" to drawable.baseline_favorite_24,
        "blood_oxygen" to drawable.baseline_favorite_24,
        "blood_pressure" to drawable.baseline_favorite_24,
        "temperature" to drawable.baseline_device_thermostat_24,
        "ambient_temperature" to drawable.baseline_device_thermostat_24,
        "calories" to drawable.baseline_local_fire_department_24,
        "sleep" to drawable.baseline_airline_seat_individual_suite_24,
        "hrv" to drawable.baseline_favorite_24,
        "stress" to drawable.baseline_favorite_24,
        "ecg" to drawable.baseline_favorite_24
    )

    private var isBleConnected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            testType = it.getString("type") // Retrieve the type argument
        }
        testCode = testTypeToHistoryMap[testType]


        val sharedPreferences =
            requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        tempUnit =
            sharedPreferences.getString("temperature_unit", "Celsius degrees") ?: "Celsius degrees"
        Log.i("Health Fragment", "$testType, $testCode, $testUnit")
    }

    override fun onResume() {
        super.onResume()

        isBleConnected = YCBTClient.connectState() == Constants.BLEState.ReadWriteOK
        if (!isBleConnected) {
            Toast.makeText(requireContext(), "Please connect to the device", Toast.LENGTH_LONG).show()
            return
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_health_data, container, false)

        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("id", -1)


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val iconResId = testTypeToIconMap[testType]

        iconResId?.let { resId ->
            val minIconView = view.findViewById<ImageView>(R.id.min_value_card_icon)
            val maxIconView = view.findViewById<ImageView>(R.id.max_value_card_icon)
            val avgIconView = view.findViewById<ImageView>(R.id.average_value_card_icon)
            minIconView?.setImageResource(resId)
            maxIconView?.setImageResource(resId)
            avgIconView?.setImageResource(resId)
        }


        // Initialize Views
        testResultValue = view.findViewById(R.id.testResultValue)
        bpTestResultValue = view.findViewById(R.id.bpTestResultValue)
        chartValueUnit = view.findViewById(R.id.selectedChartValueUnit)
        testResultUnit = view.findViewById(R.id.testResultUnit)
        selectedChartTime = view.findViewById(R.id.selectedChartTime)
        selectedChartValue = view.findViewById(R.id.selectedChartValue)
        btnStartTest = view.findViewById(R.id.btnStartTest)
        btnStopTest = view.findViewById(R.id.btnStopTest)
        remainingTimeText = view.findViewById(R.id.remainingTimeText)
        startTestContainer = view.findViewById(R.id.start_test_container)

        val normalLayout = view.findViewById<LinearLayout>(R.id.normalLayout)
        val bloodPressureValueLayout =
            view.findViewById<RelativeLayout>(R.id.bloodPressureValueLayout)
        val generalGridLayout = view.findViewById<GridLayout>(R.id.general_grid_layout)
        val bpGridLayout = view.findViewById<GridLayout>(R.id.bp_grid_layout)
        if (testType == "blood_pressure") {
            normalLayout.visibility = View.GONE
            generalGridLayout.visibility = View.GONE
            bloodPressureValueLayout.visibility = View.VISIBLE
            bpGridLayout.visibility = View.VISIBLE
        } else {
            normalLayout.visibility = View.VISIBLE
            generalGridLayout.visibility = View.VISIBLE
            bloodPressureValueLayout.visibility = View.GONE
            bpGridLayout.visibility = View.GONE
        }

        if (testType == "sleep" || testType == "calories") {
            startTestContainer.visibility = View.GONE
        } else {
            startTestContainer.visibility = View.VISIBLE
        }

        // Initialize tab views
        tabDay = view.findViewById(R.id.tabDay)
        tabWeek = view.findViewById(R.id.tabWeek)
        tabMonth = view.findViewById(R.id.tabMonth)

        dateText = view.findViewById(R.id.dateText)
        previousIcon = view.findViewById(R.id.previousIcon)
        nextIcon = view.findViewById(R.id.nextIcon)

        minValueText = view.findViewById(R.id.minValueText)
        maxValueText = view.findViewById(R.id.maxValueText)
        averageValueText = view.findViewById(R.id.averageValueText)

        systolicBpValueText = view.findViewById(R.id.systolicBpValueText)
        diastolicBpValueText = view.findViewById(R.id.diastolicBpValueText)

        healthDataChart = view.findViewById(R.id.healthDataChartDaily)
        healthDataWeeklyChart = view.findViewById(R.id.healthDataChartWeekly)
        healthDataMonthlyChart = view.findViewById(R.id.healthDataChartMonthly)

        updateDate()
        setupDailyChart()
        setupWeeklyChart()
        setupMonthlyChart()

        val tabs = listOf(tabDay, tabWeek, tabMonth)

        // Set initial selection to "Day"
        selectTab(tabDay, tabs)
        isDayMode = true

        // Set click listeners for tabs
        tabDay.setOnClickListener {
            healthDataChart.visibility = View.VISIBLE
            healthDataWeeklyChart.visibility = View.GONE
            healthDataMonthlyChart.visibility = View.GONE
            isDayMode = true
            isWeekMode = false
            isMonthMode = false
            selectTab(tabDay, tabs)
            updateDate()
        }
        tabWeek.setOnClickListener {
            healthDataChart.visibility = View.GONE
            healthDataWeeklyChart.visibility = View.VISIBLE
            healthDataMonthlyChart.visibility = View.GONE
            isDayMode = false
            isWeekMode = true
            isMonthMode = false
            selectTab(tabWeek, tabs)
            updateWeekDates()
        }
        tabMonth.setOnClickListener {
            healthDataChart.visibility = View.GONE
            healthDataWeeklyChart.visibility = View.GONE
            healthDataMonthlyChart.visibility = View.VISIBLE
            isDayMode = false
            isWeekMode = false
            isMonthMode = true
            selectTab(tabMonth, tabs)
            updateMonthDates()
        }

        previousIcon.setOnClickListener {
            if (isDayMode) {
                goToPreviousDate()
            } else if (isWeekMode) {
                goToPreviousWeek()
            } else if (isMonthMode) {
                goToPreviousMonth()
            }
        }

        nextIcon.setOnClickListener {
            if (isDayMode) {
                goToNextDate()
            } else if (isWeekMode) {
                goToNextWeek()
            } else if (isMonthMode) {
                goToNextMonth()
            }
        }

        fetchHealthDataByType()
        fetchHealthDataByDayByType()

        btnStartTest.setOnClickListener {
            Toast.makeText(context, "Starting test", Toast.LENGTH_SHORT).show()
            startHandleTest()
        }

        btnStopTest.setOnClickListener {
            Toast.makeText(context, "Test stopped", Toast.LENGTH_SHORT).show()
            stopHandleTest()
        }

        updateTestDetails()
//        when (testType) {
//            "heart_rate" -> heartDataSync()
//            "blood_pressure" -> bloodPressureDataSync()
//            "blood_oxygen" -> bloodOxygenDataSync()
//            "temperature" -> syncTemperatureData()
//            "hrv" -> syncAutoHRVData()
//            "stress" -> syncAutoPressureData()
//            else -> Log.w("Health Fragment", "No sync method for type: $testType")
//        }
    }

    private fun compareRingAndApiData(
        ringData: List<RingValueEntry>,
        apiDataRaw: List<GetUserHealthDataResponse.GetHealthData>
    ) {
        if (hasSyncedOnce) {
            Log.i("HealthSyncCompare", "Already synced once, skipping...")
            return
        }

        if (ringData.isEmpty()) {
            Log.w("HealthSyncCompare", "No ring data available")
            return
        }

        // Convert API data
        val apiData = apiDataRaw.mapNotNull { apiItem ->
            val ts = apiItem.timestamp?.toLongOrNull()
            if (ts != null) {
                RingValueEntry(apiItem.value, ts)
            } else null
        }

        val sortedRing = ringData.sortedByDescending { it.timestamp }
        val sortedApi = apiData.sortedByDescending { it.timestamp }

        // Log latest 15
        Log.i("HealthSyncCompare", "=== Latest 15 from API ===")
        sortedApi.take(15).forEach {
            Log.i(
                "HealthSyncCompare",
                "API → ${it.timestamp} ${convertTimestampToBangaloreTime(it.timestamp)}, Value=${it.value}"
            )
        }

        Log.i("HealthSyncCompare", "=== Latest 15 from Ring ===")
        sortedRing.take(15).forEach {
            Log.i(
                "HealthSyncCompare",
                "RING → ${it.timestamp} ${convertTimestampToBangaloreTime(it.timestamp)}, Value=${it.value}"
            )
        }

        // Always send once after both values are loaded
        saveNewRingEntriesToApi(sortedRing)

        hasSyncedOnce = true
        Log.i("HealthSyncCompare", "Data sent once, flag set → further sends disabled ✅")
    }


    private fun saveNewRingEntriesToApi(
        newEntries: List<RingValueEntry>
    ) {
        if (testType.isNullOrBlank()) {
            Log.e("Health Ring Response", "testType is null or empty — cannot save data")
            return
        }

        repository.saveHealthDataBatch(userId, testType!!, newEntries)
            .enqueue(object : Callback<AddUserHealthDataResponse> {
                override fun onResponse(
                    call: Call<AddUserHealthDataResponse>,
                    response: Response<AddUserHealthDataResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        Log.i(
                            "Health Ring Response",
                            "Batch $testType new data synced successfully"
                        )
                        Log.i("Health Ring Response", "Response ${response.body()} ")
                        fetchHealthDataByType()
                    } else {
                        Log.e(
                            "Health Ring Response",
                            "Failed to save $testType data: ${response.message()}"
                        )
                    }
                }

                override fun onFailure(call: Call<AddUserHealthDataResponse>, t: Throwable) {
                    Log.e("Health Ring Response", "Error saving $testType data: ${t.message}")
                }
            })
    }

    private fun convertTimestampToBangaloreTime(timestamp: Long): String {
        val instant = Instant.ofEpochSecond(timestamp)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Kolkata"))
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a")
        return dateTime.format(formatter)
    }

    private fun updateTestDetails() {
        // Update the test unit dynamically based on the test type
        testUnit = testTypeToUnitMap[testType]

        val unitText = testUnit ?: "--"
        testResultUnit.text = unitText
        chartValueUnit.text = unitText
    }


    private fun startHandleTest() {
        if (testCode != null) {
//            bleConnectHelper.setHandleTest(testCode!!, 1)
            startTest()
        } else {
            showMessage("Test type is not supported.")
        }
    }

    fun stopHandleTest() {

//        bleConnectHelper.setHandleTest(testCode!!, 0)
        stopTest()
    }

    fun getHandleTest() {
//        bleConnectHelper.getHandleTestResult(testCode!!)
    }

    private fun selectTab(selectedTab: TextView, allTabs: List<TextView>) {
        allTabs.forEach { it.isSelected = false }
        selectedTab.isSelected = true
    }

    private fun startTest() {
        btnStartTest.visibility = View.GONE
        btnStopTest.visibility = View.VISIBLE

        // Start countdown timer (60 seconds)
        countDownTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()
                val remainTimeValue = "Remaining $remainingSeconds s"
                remainingTimeText.text = remainTimeValue
                getHandleTest() // Call storing function every second
            }

            override fun onFinish() {
                remainingTimeText.text = "Remaining 0 s"
                getHandleTest()
                stopHandleTest()
//                sendHealthDataToApi()
            }
        }
        countDownTimer.start()
    }

    private fun stopTest() {
        countDownTimer.cancel()
        btnStartTest.visibility = View.VISIBLE
        btnStopTest.visibility = View.GONE
        remainingTimeText.text = "Remaining 0 s"
        remainingSeconds = 60
    }

    private fun applyTemperatureUnit(data: Float, ambient: Boolean = false): String {
        if (data == 0f) {
            val unitSymbol = if (tempUnit == "Fahrenheit") "°F" else "°C"
            return "--$unitSymbol"
        }
        val (convertedData, unitSymbol) = if (tempUnit == "Fahrenheit") {
            if (data < 50f) {
                Pair(celsiusToFahrenheit(data), "°F")
            } else {
                Pair(data, "°F")
            }
        } else {
            if (data > 50f) {
                Pair(fahrenheitToCelisius(data), "°C")
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


    private fun fetchHealthDataByType() {
        if (userId == -1) {
            Toast.makeText(context, "User ID not found. Please log in again.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val type = testType ?: return

        repository.getHealthData(userId, type)
            .enqueue(object : Callback<GetUserHealthDataResponse> {
                override fun onResponse(
                    call: Call<GetUserHealthDataResponse>,
                    response: Response<GetUserHealthDataResponse>
                ) {
                    if (response.isSuccessful && response.body()?.data?.isNotEmpty() == true) {
                        selectedHealthTypeDataList = response.body()!!.data.toMutableList()
                        fetchRingData()
//                        compareRingAndApiData(latestRingDataList, selectedHealthTypeDataList)
                        selectedDateHealthDataList()

                        val latestEntry = selectedHealthTypeDataList.maxByOrNull {
                            it.timestamp?.toLongOrNull() ?: 0L
                        }
                        latestEntry?.let {
                            val ts = it.timestamp?.toLongOrNull() ?: 0L
                            context?.let { ctx ->
                                ConnectionPreferences.setLastVitalTime(
                                    ctx,
                                    type,
                                    ts,
                                    it.value.toString()
                                )
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<GetUserHealthDataResponse>, t: Throwable) {
                    Log.e("Health Fragment", "API call failed: ${t.message}")
                }
            })
    }

    private fun fetchHealthDataByDayByType() {
        if (userId == -1) {
            Toast.makeText(context, "User ID not found. Please log in again.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val type = testType ?: return

        repository.getHealthDataByDay(userId, type)
            .enqueue(object : Callback<GetUserHealthDataByDayResponse> {
                override fun onResponse(
                    call: Call<GetUserHealthDataByDayResponse>,
                    response: Response<GetUserHealthDataByDayResponse>
                ) {

                    if (response.isSuccessful && response.body()?.data?.isNotEmpty() == true) {
                        selectedHealthTypeDataByDayList = response.body()!!.data.toMutableList()
//                        selectedHealthTypeDataByDayList.forEach {
//                            Log.i(
//                                "Health Response Week",
//                                "Type: $testType, Date: ${it.vDate}, Value: ${it.value}, ${it.diastolicValue}"
//                            )
//                        }
                    } else {
                        Log.e("Health Response Week", "Error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<GetUserHealthDataByDayResponse>, t: Throwable) {
                    Log.e("Health Response Week", "API call failed: ${t.message}")
                }
            })
    }


    private fun selectedDateHealthDataList() {
        if (selectedHealthTypeDataList.isEmpty()) {
            Log.i("Filtered Health Data", "No heart rate data available to filter.")
            return
        }
        // Filter heart rate data based on the selected date
        var filteredData = selectedHealthTypeDataList.filter { data ->
            try {
                var createdDate: String? = null
                val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
                    .withZone(ZoneId.of("UTC"))
                if (data.timestamp != null) {
                    val instant = Instant.ofEpochSecond(data.timestamp.toLong())
                    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"))
                    createdDate = dateTime.format(inputFormatter)
                } else {
                    createdDate = data.created_at
                }
                val createdAtInstant = Instant.from(inputFormatter.parse(createdDate))
                val createdAtDate = createdAtInstant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()
                createdAtDate.isEqual(selectedDate)
            } catch (e: Exception) {
                Log.e("Filtered Health Data", "Error parsing date: ${data.created_at}", e)
                false
            }
        }

        if (testType == "temperature") {
            filteredData.map { data ->
                var newValue = applyTemperatureUnit(
                    data.value
                        .replace("°C", "")
                        .replace("°F", "").toFloat()
                ).replace("°F", "")
                newValue = newValue.replace("°C", "")
                data.value = newValue
            }
        }

        if (testType == "ambient_temperature") {
            filteredData.map { data ->
                val newValue = applyTemperatureUnit(
                    data.value
                        .replace("°C", "")
                        .replace("°F", "")
                        .toFloat(),
                    true
                ).replace("°F", "")
                    .replace("°C", "")
                data.value = newValue
            }
        }

        Log.d("Health Data By Day", "Filtered Data After Conversion: $filteredData")

        populateDailyChart(filteredData)
        getInitialData(filteredData)
    }

    private fun selectedWeekHealthDataList() {
        if (selectedHealthTypeDataByDayList.isEmpty()) {
            Log.i("Health Data By Week", "No heart rate data available to filter.")
            return
        }

        val weeklyDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        // Filter data based on the selected week
        var filteredData = selectedHealthTypeDataByDayList.filter { data ->
            try {
                data.vDate?.let { dateStr ->
                    val itemDate = LocalDate.parse(dateStr, weeklyDateFormatter)
                    val isInWeekRange = itemDate in weekStartDate..weekEndDate
                    Log.d(
                        "Health Date Filtering",
                        "Date: $dateStr, Parsed: $itemDate, In Range: $isInWeekRange (Start: $weekStartDate, End: $weekEndDate)"
                    )
                    isInWeekRange
                } ?: false
            } catch (e: DateTimeParseException) {
                Log.e("Date Parsing Error", "Invalid date format: ${data.vDate}", e)
                false
            }
        }.toMutableList()

        // Apply temperature unit conversions if needed
        if (testType == "temperature") {
            filteredData = filteredData.map { data ->
                val convertedValue = applyTemperatureUnit(data.value.toFloat())
                    .replace("°F", "")
                    .replace("°C", "")
                    .toDouble()
                Log.d(
                    "Temperature Conversion",
                    "Original: ${data.value}, Converted: $convertedValue"
                )
                data.copy(value = convertedValue)
            }.toMutableList()
        }

        if (testType == "ambient_temperature") {
            filteredData = filteredData.map { data ->
                val convertedValue = applyTemperatureUnit(data.value.toFloat(), true)
                    .replace("°F", "")
                    .replace("°C", "")
                    .toDouble()
                Log.d(
                    "Ambient Temp Conversion",
                    "Original: ${data.value}, Converted: $convertedValue"
                )
                data.copy(value = convertedValue)
            }.toMutableList()
        }

        // Populate chart and set initial data
        populateWeeklyChart(filteredData)
        getInitialWeeklyData(filteredData)
    }

    private fun selectedMonthHealthDataList() {
        if (selectedHealthTypeDataByDayList.isEmpty()) {
            Log.i("Filtered Data By Week", "No heart rate data available to filter.")
            return
        }

        val weeklyDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        // Filter data based on the selected week
        var filteredData = selectedHealthTypeDataByDayList.filter { data ->
            try {
                data.vDate?.let { dateStr ->
                    val itemDate = LocalDate.parse(dateStr, weeklyDateFormatter)
                    val isInMonthRange = itemDate in monthStartDate..monthEndDate
                    Log.d(
                        "Date Filtering",
                        "Date: $dateStr, Parsed: $itemDate, In Range: $isInMonthRange (Start: $monthStartDate, End: $monthEndDate)"
                    )
                    isInMonthRange
                } ?: false
            } catch (e: DateTimeParseException) {
                Log.e("Date Parsing Error", "Invalid date format: ${data.vDate}", e)
                false
            }
        }.toMutableList()

        // Apply temperature unit conversions if needed
        if (testType == "temperature") {
            filteredData = filteredData.map { data ->
                val convertedValue = applyTemperatureUnit(data.value.toFloat())
                    .replace("°F", "")
                    .replace("°C", "")
                    .toDouble()
                Log.d(
                    "Temperature Conversion",
                    "Original: ${data.value}, Converted: $convertedValue"
                )
                data.copy(value = convertedValue)
            }.toMutableList()
        }

        if (testType == "ambient_temperature") {
            filteredData = filteredData.map { data ->
                val convertedValue = applyTemperatureUnit(data.value.toFloat(), true)
                    .replace("°F", "")
                    .replace("°C", "")
                    .toDouble()
                Log.d(
                    "Ambient Temp Conversion",
                    "Original: ${data.value}, Converted: $convertedValue"
                )
                data.copy(value = convertedValue)
            }.toMutableList()
        }

        // Populate chart and set initial data
        populateMonthlyChart(filteredData)
        getInitialWeeklyData(filteredData)
    }


    private fun getInitialData(data: List<GetUserHealthDataResponse.GetHealthData>) {
        if (data.isNotEmpty()) {
            val firstEntry = data.first()

            // Parse the `created_at` field to time (HH:MM)
            try {
                val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
                    .withZone(ZoneId.of("UTC"))
                val createdAtInstant = Instant.from(inputFormatter.parse(firstEntry.created_at))
                val localTime = createdAtInstant.atZone(ZoneId.systemDefault()).toLocalTime()

                val hour = localTime.hour
                val minute = localTime.minute
                val timeFormatted = String.format("%02d:%02d", hour, minute)

                // Update the text views for the first entry
                selectedChartTime.text = timeFormatted
                selectedChartValue.text = firstEntry.value.toString()

                Log.i(
                    "Health Init Data",
                    "Selected Point - Time: $timeFormatted, Value: ${firstEntry.value}"
                )
            } catch (e: Exception) {
                Log.e(
                    "Health Init Data",
                    "Error formatting first entry date: ${firstEntry.created_at}",
                    e
                )
            }

            // Calculate min, max, and average values from the data list
            val values = data.map { it.value.toDoubleOrNull() ?: 0.0 }

            val minValue = values.minOrNull() ?: 0.0
            val maxValue = values.maxOrNull() ?: 0.0
            val averageValue = if (values.isNotEmpty()) values.average() else 0.0

            // Update the text views for min, max, and average
            if (testType != "blood_pressure") {
                minValueText.text = minValue.toInt().toString()
                maxValueText.text = maxValue.toInt().toString()
                averageValueText.text = averageValue.toInt().toString()
            } else {
                // For blood pressure, only show the last value
                val lastValue = data.first().value
                if (lastValue.contains("/")) {
                    val parts = lastValue.split("/")
                    if (parts.size == 2) {
                        val systolic = parts[0].toIntOrNull() ?: 0
                        val diastolic = parts[1].toIntOrNull() ?: 0
                        systolicBpValueText.text = systolic.toString()
                        diastolicBpValueText.text = diastolic.toString()
                    } else {
                        Log.e("Health Data", "Invalid blood pressure format: $lastValue")
                    }
                } else {
                    Log.e(
                        "Health Data",
                        "Blood pressure value does not contain '/' separator: $lastValue"
                    )
                }
            }
        } else {
            Log.i("Health Data", "No data available for the selected date.")
            selectedChartTime.text = "--:--"
            selectedChartValue.text = "--"
            minValueText.text = "--"
            maxValueText.text = "--"
            averageValueText.text = "--"
            systolicBpValueText.text = "--"
            diastolicBpValueText.text = "--"
        }
    }

    private fun getInitialWeeklyData(data: List<GetUserHealthDataByDayResponse.GetHealthData>) {
        if (data.isNotEmpty()) {
            val firstEntry = data.first()

            // Parse the `vDate` field to display the date (e.g., "2025-03-14")
            try {
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val localDate = LocalDate.parse(firstEntry.vDate, dateFormatter)

                // Format the date as needed (e.g., "Friday, 2025-03-14")
                val dayOfWeek = localDate.dayOfWeek
                val dayLabel = when (dayOfWeek) {
                    DayOfWeek.MONDAY -> "Monday"
                    DayOfWeek.TUESDAY -> "Tuesday"
                    DayOfWeek.WEDNESDAY -> "Wednesday"
                    DayOfWeek.THURSDAY -> "Thursday"
                    DayOfWeek.FRIDAY -> "Friday"
                    DayOfWeek.SATURDAY -> "Saturday"
                    DayOfWeek.SUNDAY -> "Sunday"
                    else -> ""
                }
                val dateFormatted = "$dayLabel, ${localDate.format(dateFormatter)}"

                // Update the text views for the first entry
                selectedChartTime.text = dateFormatted
                selectedChartValue.text = firstEntry.value.toString()

                Log.i(
                    "Health Init Weekly Data",
                    "Selected Point - Date: $dateFormatted, Value: ${firstEntry.value}"
                )
            } catch (e: Exception) {
                Log.e(
                    "Health Init Weekly Data",
                    "Error formatting first entry date: ${firstEntry.vDate}",
                    e
                )
            }

            // Calculate min, max, and average values from the data list
            val values = data.map { it.value.toDouble() ?: 0.0 }

            val minValue = values.minOrNull() ?: 0.0
            val maxValue = values.maxOrNull() ?: 0.0
            val averageValue = if (values.isNotEmpty()) values.average() else 0.0

            // Update the text views for min, max, and average
            if (testType != "blood_pressure") {
                minValueText.text = minValue.toInt().toString()
                maxValueText.text = maxValue.toInt().toString()
                averageValueText.text = averageValue.toInt().toString()
            } else {
                // For blood pressure, only show the last value
//                val lastValue = data.first().value
//                if (lastValue.contains("/")) {
//                    val parts = lastValue.split("/")
//                    if (parts.size == 2) {
//                        val systolic = parts[0].toIntOrNull() ?: 0
//                        val diastolic = parts[1].toIntOrNull() ?: 0
//                        systolicBpValueText.text = systolic.toString()
//                        diastolicBpValueText.text = diastolic.toString()
//                    } else {
//                        Log.e("Health Data", "Invalid blood pressure format: $lastValue")
//                    }
//                } else {
//                    Log.e(
//                        "Health Data",
//                        "Blood pressure value does not contain '/' separator: $lastValue"
//                    )
//                }
            }
            Log.i("Health Weekly Data", "Min: $minValue, Max: $maxValue, Average: $averageValue")
        } else {
            Log.i("Health Weekly Data", "No data available for the selected week.")
            selectedChartTime.text = "--:--"
            selectedChartValue.text = "--"
            minValueText.text = "--"
            maxValueText.text = "--"
            averageValueText.text = "--"
            systolicBpValueText.text = "--"
            diastolicBpValueText.text = "--"
        }
    }

    private fun setupDailyChart() {
        healthDataChart.description.isEnabled = false
        healthDataChart.setTouchEnabled(true)
        healthDataChart.isDragEnabled = true
        healthDataChart.setScaleEnabled(true)  // Allow zooming & scrolling
        healthDataChart.setPinchZoom(true)  // Smooth zooming
        healthDataChart.isScaleXEnabled = true  // Enable horizontal scaling
        healthDataChart.isScaleYEnabled = false // Disable vertical scaling

        healthDataChart.axisLeft.setDrawGridLines(false)
        healthDataChart.axisRight.setDrawGridLines(false)
        healthDataChart.xAxis.setDrawGridLines(false)
        healthDataChart.axisLeft.setDrawAxisLine(false)

        // Enable scrolling/panning
        healthDataChart.isDragDecelerationEnabled = true
        healthDataChart.dragDecelerationFrictionCoef = 0.9f  // Smooth scrolling

        // Add listener for selecting data points
        healthDataChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e != null) {
                    val hour = e.x.toInt()
                    val minute = ((e.x - hour) * 60).toInt()  // Convert decimal to minutes
                    val timeFormatted = String.format("%02d:%02d", hour, minute)
                    val heartRate = e.y.toInt()

                    selectedChartTime.text = timeFormatted
                    selectedChartValue.text = heartRate.toString()

                    Log.i(
                        "Health Data Chart Click",
                        "Selected Point - Time: $timeFormatted, Value: $heartRate"
                    )
                }
            }

            override fun onNothingSelected() {
                Log.i("Health Data Chart Click", "No point selected")
            }
        })

        // Get current time
        val now = LocalDateTime.now()
        val currentHour = now.hour + now.minute / 60.0f

        // Define X-axis range
        val xAxis = healthDataChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f // Label every hour
        xAxis.labelCount = 5  // Show 5 labels at a time

        // Set full range (0 to 24 hours)
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 24f

        // Format X-axis labels to show hours
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                // Get the visible range of the chart
                val visibleRange =
                    healthDataChart.viewPortHandler.scaleX * (xAxis.axisMaximum - xAxis.axisMinimum)

                // If zoomed in (visible range is less than 5 hours), show 30-minute intervals
                return if (visibleRange <= 5f) {
                    val hour = value.toInt()
                    val minute = ((value - hour) * 60).toInt()
                    String.format("%02d:%02d", hour, minute) // Format as "05:30", "06:00", etc.
                } else {
                    val hour = value.toInt()
                    String.format("%02d:00", hour) // Format as "05:00", "06:00", etc.
                }
            }
        }

        // Set visible range dynamically
        healthDataChart.setVisibleXRangeMaximum(5f)  // Only 5 labels at a time

        healthDataChart.setOnChartGestureListener(object : OnChartGestureListener {
            override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
                updateXAxisLabels()
            }

            override fun onChartGestureStart(
                me: MotionEvent?,
                lastPerformedGesture: ChartTouchListener.ChartGesture?
            ) {
            }

            override fun onChartGestureEnd(
                me: MotionEvent?,
                lastPerformedGesture: ChartTouchListener.ChartGesture?
            ) {
            }

            override fun onChartLongPressed(me: MotionEvent?) {}
            override fun onChartDoubleTapped(me: MotionEvent?) {}
            override fun onChartSingleTapped(me: MotionEvent?) {}
            override fun onChartFling(
                me1: MotionEvent?,
                me2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ) {
            }

            override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
        })
    }

    private fun updateXAxisLabels() {
        val xAxis = healthDataChart.xAxis
        val visibleRange = healthDataChart.highestVisibleX - healthDataChart.lowestVisibleX

        if (visibleRange > 6) {
            // Show only full hours
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val hour = value.toInt()
                    return String.format("%02d:00", hour)
                }
            }
            xAxis.granularity = 1f
        } else {
            // Show 30-minute intervals when zoomed in
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val hour = value.toInt()
                    val minutes = if (value - hour >= 0.5) 30 else 0
                    return String.format("%02d:%02d", hour, minutes)
                }
            }
            xAxis.granularity = 0.5f
        }

        healthDataChart.invalidate() // Refresh the chart
    }


    private fun populateDailyChart(healthDataList: List<GetUserHealthDataResponse.GetHealthData>) {
        Log.i("Health Data", "chart data count: ${healthDataList.size}")

        val systolicEntries = mutableListOf<Entry>()
        val diastolicEntries = mutableListOf<Entry>()

        healthDataList.forEach { healthData ->
            try {
                val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
                    .withZone(ZoneId.of("UTC"))
                var createdAt: String? = null
                if (healthData.timestamp != null) {
                    val instant = Instant.ofEpochSecond(healthData.timestamp.toLong())
                    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"))
                    createdAt = dateTime.format(inputFormatter)
                } else {
//                    createdAt = healthData.created_at
                }
                val createdAtInstant = Instant.from(inputFormatter.parse(createdAt))
                val localTime = createdAtInstant.atZone(ZoneId.systemDefault()).toLocalTime()
                val hourFraction = localTime.hour + localTime.minute / 60f

                if (testType == "blood_pressure") {
                    // Extract systolic and diastolic values
                    val bpParts = healthData.value.split("/")
                    val systolic = bpParts.getOrNull(0)?.toFloatOrNull() ?: 0f // Systolic value
                    val diastolic = bpParts.getOrNull(1)?.toFloatOrNull() ?: 0f // Diastolic value

//                    Log.i(
//                        "Health Data Chart",
//                        "HourFraction: $hourFraction, Systolic: $systolic, Diastolic: $diastolic"
//                    )

                    systolicEntries.add(Entry(hourFraction, systolic))
                    diastolicEntries.add(Entry(hourFraction, diastolic))
                } else {
                    val value = healthData.value.toFloatOrNull() ?: 0f
//                    Log.i(
//                        "Chart Entry",
//                        "X (hourFraction): $hourFraction | Y (value): $value | created_at: $createdAt"
//                    )
                    systolicEntries.add(Entry(hourFraction, value))
                }
            } catch (e: Exception) {
                Log.e("Health Data", "Error parsing date for chart: ${healthData.created_at}", e)
            }
        }

        // Sort the entries in ascending order by hourFraction (x value)
        val sortedSystolicEntries = systolicEntries.sortedBy { it.x }
        val sortedDiastolicEntries = diastolicEntries.sortedBy { it.x }

        val maxYValue = if (testType == "blood_pressure") {
            maxOf(
                healthDataList.mapNotNull { it.value.split("/").getOrNull(0)?.toFloatOrNull() }
                    .maxOrNull() ?: 0f,
                healthDataList.mapNotNull { it.value.split("/").getOrNull(1)?.toFloatOrNull() }
                    .maxOrNull() ?: 0f
            )
        } else {
            healthDataList.maxOfOrNull { it.value.toFloatOrNull() ?: 0f } ?: 0f
        }

        val leftAxis = healthDataChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = maxYValue + 30f
        healthDataChart.axisRight.isEnabled = false

        if (sortedSystolicEntries.isNotEmpty()) {
            // Systolic Line DataSet
            val systolicDataSet = LineDataSet(sortedSystolicEntries, "Systolic BP")
            systolicDataSet.setDrawValues(false)
            systolicDataSet.color = Color.RED
            systolicDataSet.valueTextSize = 12f
            systolicDataSet.setCircleColor(Color.BLUE)
            systolicDataSet.circleRadius = 4f

            // Diastolic Line DataSet (only for blood_pressure)
            val diastolicDataSet =
                if (testType == "blood_pressure" && sortedDiastolicEntries.isNotEmpty()) {
                    LineDataSet(sortedDiastolicEntries, "Diastolic BP").apply {
                        setDrawValues(false)
                        color = Color.BLUE
                        valueTextSize = 12f
                        setCircleColor(Color.BLUE)
                        circleRadius = 4f
                    }
                } else {
                    null
                }

            // Create LineData and update the chart
            val lineData = LineData().apply {
                addDataSet(systolicDataSet)
                diastolicDataSet?.let { addDataSet(it) }
            }

            healthDataChart.legend.isEnabled = testType == "blood_pressure"
            healthDataChart.data = lineData

            healthDataChart.post {
                val lastEntryX = sortedSystolicEntries.lastOrNull()?.x ?: 0f

                healthDataChart.moveViewToX(lastEntryX)
            }

            healthDataChart.xAxis.apply {
                axisMaximum = (sortedSystolicEntries.lastOrNull()?.x
                    ?: 0f) + 1 // Extend axis just after the last value
            }

            healthDataChart.invalidate()
        } else {
            Log.i("Health Data", "No valid data to display on the chart.")
            healthDataChart.data = null
            healthDataChart.invalidate()
        }
    }

    private fun setupWeeklyChart() {
        healthDataWeeklyChart.description.isEnabled = false
        healthDataWeeklyChart.setTouchEnabled(true) // Disable all touch interactions
        healthDataWeeklyChart.isDragEnabled = true // Disable dragging
        healthDataWeeklyChart.setScaleEnabled(true) // Disable scaling
        healthDataWeeklyChart.setPinchZoom(true) // Disable pinch zoom
        healthDataWeeklyChart.isScaleXEnabled = true // Disable X-axis scaling
        healthDataWeeklyChart.isScaleYEnabled = false // Disable Y-axis scaling

        healthDataWeeklyChart.axisLeft.setDrawGridLines(false)
        healthDataWeeklyChart.axisRight.setDrawGridLines(false)
        healthDataWeeklyChart.xAxis.setDrawGridLines(false)
        healthDataWeeklyChart.axisLeft.setDrawAxisLine(false)

        // Configure X-axis for weekly data
        val xAxis = healthDataWeeklyChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f // Ensure labels are spaced correctly
        xAxis.labelCount = 7 // Show 7 labels (M, T, W, T, F, S, S)

        // Set constant labels for days of the week
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when (value.toInt()) {
                    0 -> "M" // Monday
                    1 -> "T" // Tuesday
                    2 -> "W" // Wednesday
                    3 -> "T" // Thursday
                    4 -> "F" // Friday
                    5 -> "S" // Saturday
                    6 -> "S" // Sunday
                    else -> ""
                }
            }
        }

        // Set X-axis range (0 to 6 for 7 days)
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 6f

        // Ensure all 7 labels are visible without scrolling or zooming
        healthDataWeeklyChart.setVisibleXRange(0f, 6f)

        // Add listener for selecting data points (optional)
        healthDataWeeklyChart.setOnChartValueSelectedListener(object :
            OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e != null && e is DateEntry) { // Check if the entry is a DateEntry
                    val dayIndex = e.x.toInt()
                    val dayLabel = when (dayIndex) {
                        0 -> "Monday"
                        1 -> "Tuesday"
                        2 -> "Wednesday"
                        3 -> "Thursday"
                        4 -> "Friday"
                        5 -> "Saturday"
                        6 -> "Sunday"
                        else -> ""
                    }
                    val value = e.y.toInt()
                    val date = e.date // Extract the date

                    // Display day and date
                    selectedChartTime.text = "$dayLabel, $date"
                    selectedChartValue.text = value.toString()

//                    Log.i(
//                        "Health Data Chart Click",
//                        "Selected Point - Day: $dayLabel, Date: $date, Value: $value"
//                    )
                }
            }

            override fun onNothingSelected() {
                Log.i("Health Data Chart Click", "No point selected")
            }
        })
    }

    private fun populateWeeklyChart(healthDataList: List<GetUserHealthDataByDayResponse.GetHealthData>) {
        Log.i("Health Data", "chart data count: ${healthDataList.size}")

        val systolicEntries = mutableListOf<DateEntry>()
        val diastolicEntries = mutableListOf<DateEntry>()

        healthDataList.forEach { healthData ->
            try {
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val localDate = LocalDate.parse(healthData.vDate, dateFormatter)

                val xValue = when (localDate.dayOfWeek) {
                    DayOfWeek.MONDAY -> 0f
                    DayOfWeek.TUESDAY -> 1f
                    DayOfWeek.WEDNESDAY -> 2f
                    DayOfWeek.THURSDAY -> 3f
                    DayOfWeek.FRIDAY -> 4f
                    DayOfWeek.SATURDAY -> 5f
                    DayOfWeek.SUNDAY -> 6f
                    else -> -1f
                }

                if (xValue != -1f) {
                    if (testType == "blood_pressure") {
                        val systolic = healthData.value.toFloat() ?: 0f
                        val diastolic = healthData.diastolicValue?.toFloat() ?: 0f

                        systolicEntries.add(DateEntry(xValue, systolic, healthData.vDate))
                        diastolicEntries.add(DateEntry(xValue, diastolic, healthData.vDate))
                    } else {
                        val value = healthData.value.toFloat() ?: 0f

                        // ✅ Use DateEntry for single-value types too
                        systolicEntries.add(DateEntry(xValue, value, healthData.vDate))
                    }
                }

            } catch (e: Exception) {
                Log.e("Health Data", "Error parsing date for chart: ${healthData.vDate}", e)
            }
        }

        val sortedSystolicEntries = systolicEntries.sortedBy { it.x }
        val sortedDiastolicEntries = diastolicEntries.sortedBy { it.x }

        val maxYValue = if (testType == "blood_pressure") {
            maxOf(
                sortedSystolicEntries.maxOfOrNull { it.y } ?: 0f,
                sortedDiastolicEntries.maxOfOrNull { it.y } ?: 0f
            )
        } else {
            sortedSystolicEntries.maxOfOrNull { it.y } ?: 0f
        }

        val leftAxis = healthDataWeeklyChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = maxYValue + 10f
        healthDataWeeklyChart.axisRight.isEnabled = false

        if (sortedSystolicEntries.isNotEmpty()) {
            val systolicDataSet = LineDataSet(sortedSystolicEntries, "Systolic BP").apply {
                setDrawValues(false)
                color = Color.RED
                valueTextSize = 12f
                setCircleColor(Color.BLUE)
                circleRadius = 4f
            }

            val diastolicDataSet =
                if (testType == "blood_pressure" && sortedDiastolicEntries.isNotEmpty()) {
                    LineDataSet(sortedDiastolicEntries, "Diastolic BP").apply {
                        setDrawValues(false)
                        color = Color.BLUE
                        valueTextSize = 12f
                        setCircleColor(Color.BLUE)
                        circleRadius = 4f
                    }
                } else null

            healthDataWeeklyChart.legend.isEnabled = testType == "blood_pressure"

            val lineData = LineData().apply {
                addDataSet(systolicDataSet)
                diastolicDataSet?.let { addDataSet(it) }
            }

            healthDataWeeklyChart.data = lineData
            healthDataWeeklyChart.invalidate()
        } else {
            Log.i("Health Data", "No valid data to display on the chart.")
            healthDataWeeklyChart.data = null
            healthDataWeeklyChart.invalidate()
        }
    }


    private fun setupMonthlyChart() {
        healthDataMonthlyChart.description.isEnabled = false
        healthDataMonthlyChart.setTouchEnabled(true) // Enable touch interactions
        healthDataMonthlyChart.isDragEnabled = true // Enable dragging
        healthDataMonthlyChart.setScaleEnabled(true) // Enable scaling
        healthDataMonthlyChart.setPinchZoom(true) // Enable pinch zoom
        healthDataMonthlyChart.isScaleXEnabled = true // Enable X-axis scaling
        healthDataMonthlyChart.isScaleYEnabled = false // Disable Y-axis scaling

        healthDataMonthlyChart.axisLeft.setDrawGridLines(false)
        healthDataMonthlyChart.axisRight.setDrawGridLines(false)
        healthDataMonthlyChart.xAxis.setDrawGridLines(false)
        healthDataMonthlyChart.axisLeft.setDrawAxisLine(false)

        // Configure X-axis for monthly data
        val xAxis = healthDataMonthlyChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f // Ensure labels are spaced correctly
        xAxis.labelCount = 7 // Show 7 labels at a time

        // Format X-axis labels to show dates (e.g., 1, 2, 3, ..., 31)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString() // Display date as integer (e.g., 1, 2, 3)
            }
        }

        // Set X-axis range (1 to 31 for days in a month)
        xAxis.axisMinimum = 1f
        xAxis.axisMaximum = 31f

        // Enable dragging to load previous dates
        healthDataMonthlyChart.setVisibleXRangeMaximum(7f) // Show 7 labels at a time

        // Add listener for selecting data points (optional)
        healthDataMonthlyChart.setOnChartValueSelectedListener(object :
            OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e != null && e is DateEntry) { // Check if the entry is a DateEntry
                    val day = e.x.toInt()
                    val value = e.y.toInt()
                    val date = e.date // Extract the date

                    // Parse the date to get the day of the week
                    try {
                        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        val localDate = LocalDate.parse(date, dateFormatter)
                        val dayOfWeek = localDate.dayOfWeek
                        val dayLabel = when (dayOfWeek) {
                            DayOfWeek.MONDAY -> "Monday"
                            DayOfWeek.TUESDAY -> "Tuesday"
                            DayOfWeek.WEDNESDAY -> "Wednesday"
                            DayOfWeek.THURSDAY -> "Thursday"
                            DayOfWeek.FRIDAY -> "Friday"
                            DayOfWeek.SATURDAY -> "Saturday"
                            DayOfWeek.SUNDAY -> "Sunday"
                            else -> ""
                        }

                        // Display day of the week, date, and value
                        selectedChartTime.text = "$dayLabel, $date"
                        selectedChartValue.text = value.toString()

//                        Log.i(
//                            "Health Data Chart Click",
//                            "Selected Point - Day: $dayLabel, Date: $date, Value: $value"
//                        )
                    } catch (ex: Exception) {
                        Log.e("Health Data", "Error parsing date: $date", ex)
                    }
                }
            }

            override fun onNothingSelected() {
                Log.i("Health Data Chart Click", "No point selected")
            }
        })
    }

    private fun populateMonthlyChart(healthDataList: List<GetUserHealthDataByDayResponse.GetHealthData>) {
        Log.i("Health Data", "chart data count: ${healthDataList.size}")

        val systolicEntries = mutableListOf<DateEntry>()
        val diastolicEntries = mutableListOf<DateEntry>()

        healthDataList.forEach { healthData ->
            try {
                // Parse the date (e.g., "2025-03-14")
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val localDate = LocalDate.parse(healthData.vDate, dateFormatter)

                // Extract the day of the month (e.g., 14 for "2025-03-14")
                val dayOfMonth = localDate.dayOfMonth

                // Check if the test type is "blood_pressure" and handle systolic and diastolic values
                if (testType == "blood_pressure") {
                    val systolic = healthData.value.toFloat() ?: 0f
                    val diastolic = healthData.diastolicValue?.toFloat() ?: 0f

                    systolicEntries.add(DateEntry(dayOfMonth.toFloat(), systolic, healthData.vDate))
                    diastolicEntries.add(
                        DateEntry(
                            dayOfMonth.toFloat(),
                            diastolic,
                            healthData.vDate
                        )
                    )
                } else {
                    // Non-blood pressure data
                    val value = healthData.value.toFloat() ?: 0f
                    systolicEntries.add(DateEntry(dayOfMonth.toFloat(), value, healthData.vDate))
                }
            } catch (e: Exception) {
                Log.e("Health Data", "Error parsing date for chart: ${healthData.vDate}", e)
            }
        }

        // Sort entries by X-axis value (day of the month)
        val sortedSystolicEntries = systolicEntries.sortedBy { it.x }
        val sortedDiastolicEntries = diastolicEntries.sortedBy { it.x }

        // Determine the maximum Y-axis value
        val maxYValue = if (testType == "blood_pressure") {
            maxOf(
                sortedSystolicEntries.maxOfOrNull { it.y } ?: 0f,
                sortedDiastolicEntries.maxOfOrNull { it.y } ?: 0f
            )
        } else {
            sortedSystolicEntries.maxOfOrNull { it.y } ?: 0f
        }

        // Configure Y-axis range
        val leftAxis = healthDataMonthlyChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = maxYValue + 10f // Add some padding
        healthDataMonthlyChart.axisRight.isEnabled = false

        if (sortedSystolicEntries.isNotEmpty()) {
            // Create LineDataSet for the systolic data
            val systolicDataSet = LineDataSet(sortedSystolicEntries, "Systolic BP").apply {
                setDrawValues(false)
                color = Color.RED
                valueTextSize = 12f
                setCircleColor(Color.BLUE)
                circleRadius = 4f
            }

            // Create LineDataSet for the diastolic data (only if test type is "blood_pressure")
            val diastolicDataSet =
                if (testType == "blood_pressure" && sortedDiastolicEntries.isNotEmpty()) {
                    LineDataSet(sortedDiastolicEntries, "Diastolic BP").apply {
                        setDrawValues(false)
                        color = Color.BLUE
                        valueTextSize = 12f
                        setCircleColor(Color.BLUE)
                        circleRadius = 4f
                    }
                } else null

            // Set the chart's legend visibility based on the test type
            healthDataMonthlyChart.legend.isEnabled = testType == "blood_pressure"

            // Prepare the chart's data with both systolic and diastolic data sets
            val lineData = LineData().apply {
                addDataSet(systolicDataSet)
                diastolicDataSet?.let { addDataSet(it) }
            }

            // Update chart data and display
            healthDataMonthlyChart.data = lineData

            // Ensure the last data point is in view and 2-3 days of the next date are visible
            val lastEntryX = sortedSystolicEntries.lastOrNull()?.x ?: 31f
            healthDataMonthlyChart.setVisibleXRangeMaximum(7f) // Show a range of 7 days
            healthDataMonthlyChart.moveViewToX(lastEntryX - 5f)

            healthDataMonthlyChart.invalidate()
        } else {
            Log.i("Health Data", "No valid data to display on the chart.")
            healthDataMonthlyChart.data = null
            healthDataMonthlyChart.invalidate()
        }
    }

    private fun updateDate() {
        dateText.text = selectedDate.format(dateFormatter)
        if (selectedDate.isBefore(LocalDate.now())) {
            nextIcon.isEnabled = true
            nextIcon.alpha = 1.0f
        } else {
            nextIcon.isEnabled = false
            nextIcon.alpha = 0.5f
        }
        healthDataChart.data = null
        healthDataChart.invalidate()
        selectedDateHealthDataList()
    }

    private fun goToPreviousDate() {
        selectedDate = selectedDate.minusDays(1)
        updateDate()
        Log.d("Previous date requested", "Current date: $selectedDate")
    }

    private fun goToNextDate() {
        if (selectedDate.isBefore(LocalDate.now())) {
            selectedDate = selectedDate.plusDays(1)
            updateDate()
            Log.d("Next date requested", "Current date: $selectedDate")
        }
    }

    private fun updateWeekDates() {
        "${weekStartDate.format(dateFormatter)} - ${weekEndDate.format(dateFormatter)}".also {
            dateText.text = it
        }

        nextIcon.isEnabled = !weekEndDate.isAfter(LocalDate.now())
        nextIcon.alpha = if (nextIcon.isEnabled) 1.0f else 0.5f
        healthDataWeeklyChart.data = null
        healthDataWeeklyChart.invalidate()
        selectedWeekHealthDataList()
    }

    private fun goToPreviousWeek() {
        weekStartDate = weekStartDate.minusWeeks(1).with(DayOfWeek.MONDAY)
        weekEndDate = weekStartDate.with(DayOfWeek.SUNDAY)
        updateWeekDates()
        Log.d("Previous week requested", "Week range: $weekStartDate to $weekEndDate")
    }

    private fun goToNextWeek() {
        if (!weekEndDate.isAfter(LocalDate.now())) {
            weekStartDate = weekStartDate.plusWeeks(1).with(DayOfWeek.MONDAY)
            weekEndDate = weekStartDate.with(DayOfWeek.SUNDAY)
            updateWeekDates()
            Log.d("Next week requested", "Week range: $weekStartDate to $weekEndDate")
        }
    }

    private fun updateMonthDates() {
        dateText.text =
            "${monthStartDate.format(dateFormatter)} - ${monthEndDate.format(dateFormatter)}"

        nextIcon.isEnabled =
            !monthEndDate.isEqual(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()))
        nextIcon.alpha = if (nextIcon.isEnabled) 1.0f else 0.5f
        healthDataMonthlyChart.data = null
        healthDataMonthlyChart.invalidate()
        selectedMonthHealthDataList()
    }

    private fun goToPreviousMonth() {
        monthStartDate = monthStartDate.minusMonths(1).withDayOfMonth(1)
        monthEndDate = monthStartDate.withDayOfMonth(monthStartDate.lengthOfMonth())
        updateMonthDates()
        Log.d("HealthDataFrag", "Month range: $monthStartDate to $monthEndDate")
    }

    private fun goToNextMonth() {
        if (!monthEndDate.isEqual(
                LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())
            )
        ) {
            monthStartDate = monthStartDate.plusMonths(1).withDayOfMonth(1)
            monthEndDate = monthStartDate.withDayOfMonth(monthStartDate.lengthOfMonth())
            updateMonthDates()
            Log.d("Next month requested", "Month range: $monthStartDate to $monthEndDate")
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun noDataAvailable(message: String) {
        Log.i(message, "No valid data to display on the chart.")
        healthDataChart.data = null
        healthDataWeeklyChart.data = null
        healthDataChart.invalidate()
        healthDataWeeklyChart.invalidate()
        selectedChartTime.text = "--:--"
        selectedChartValue.text = "--"
        minValueText.text = "--"
        maxValueText.text = "--"
        averageValueText.text = "--"
        systolicBpValueText.text = "--"
        diastolicBpValueText.text = "--"
    }

    private fun fetchRingData() {

        if (!isBleConnected) {
            Log.w("Health Fragment", "Device not connected, aborting fetch")
            return
        }
        // Sync ring config with server before fetching data
//        AutoTestConfigSyncHelper(requireContext()).syncFromApiAndUpdateRing()

        testCode?.let { code ->
            Log.i("Health Fragment", "Fetching history for $testType ($code)")

            // 🔹 Calls the ring library (YCBTClient) to fetch history data
            YCBTClient.healthHistoryData(code, object : BleDataResponse {
                override fun onDataResponse(
                    responseCode: Int,
                    ratio: Float,
                    resultMap: HashMap<*, *>?
                ) {
                    if (resultMap == null) {
                        Log.w("Health Fragment", "Ring returned null data for $testType")
                        return
                    }

                    this@HealthDataFragment.onDataResponses(responseCode, ratio, resultMap)

                    // 🔹 Debug log of the raw data
                    Log.i(
                        "Health Fragment",
                        "ResponseCode=$responseCode, ratio=$ratio, data=$resultMap"
                    )
                }
            })
        } ?: run {
            // ❌ If testCode is null, log warning
            Log.w("Health Fragment", "No matching history code for type=$testType")
        }
    }


    private fun onDataResponses(responseCode: Int, ratio: Float, resultMap: HashMap<*, *>?) {
        if (responseCode != 0) {
            Log.w("HealthDataFragment", "Failed to get data: code=$responseCode")
            return
        }

        val dataList = resultMap?.get("data") as? List<HashMap<String, Any>>
        if (dataList.isNullOrEmpty()) {
            Log.i("HealthDataFragment", "No data available for $testType")
            return
        }

        val ringEntries = mutableListOf<RingValueEntry>()

        when (testType) {
            "heart_rate" -> {
                dataList.forEach { item ->
                    val value = item["heartValue"] as? Int
                    val timestamp = item["heartStartTime"] as? Long
                    if (value != null && timestamp != null) {
                        val timestampSeconds = timestamp / 1000 // convert to seconds
                        ringEntries.add(RingValueEntry(value, timestampSeconds))

                        val formattedTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(java.util.Date(timestamp))
                        Log.i("HealthDataFragment", "HeartRate=$value BPM at $formattedTime")
                    }
                }
            }

            "blood_pressure" -> {
                dataList.forEach { item ->
                    val sbp = item["bloodSBP"] as? Int // systolic
                    val dbp = item["bloodDBP"] as? Int // diastolic
                    val timestamp = item["bloodStartTime"] as? Long
                    if (sbp != null && dbp != null && timestamp != null) {
                        // Combine systolic and diastolic into one value string
                        val bpValue = "$sbp/$dbp"
                        ringEntries.add(RingValueEntry(bpValue, timestamp / 1000))

                        Log.i(
                            "HealthDataFragment",
                            "BP=$bpValue mmHg at ${
                                java.text.SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss"
                                ).format(java.util.Date(timestamp))
                            }"
                        )
                    }
                }
            }

            else -> {
                Log.w("HealthDataFragment", "Unsupported type=$testType, raw data=$resultMap")
            }
        }

        // ✅ Now call compare function if we got ring data
        if (ringEntries.isNotEmpty()) {
            compareRingAndApiData(ringEntries, selectedHealthTypeDataList)
        } else {
            Log.i("HealthDataFragment", "No valid entries to compare for $testType")
        }
    }



}
