package com.smartringpro.mannaheal.ui.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.gson.Gson
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.userHealthData.SleepBeanResponse
import com.smartringpro.mannaheal.api.userHealthData.SleepDataByDateWise
import com.smartringpro.mannaheal.api.userHealthData.SleepResponse
import com.smartringpro.mannaheal.api.userHealthData.UserHealthDataRepository
import com.smartringpro.mannaheal.databinding.FragmentSleepBinding
import com.yucheng.ycbtsdk.Constants
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.response.BleDataResponse
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.compareTo
import kotlin.div
import kotlin.rem
import kotlin.text.toFloat
import kotlin.text.toInt
import kotlin.text.toLong
import kotlin.times

data class SleepBean(
    val statisticTime: Long,
    val startSleepHour: Int,
    val startSleepMinute: Int,
    val endSleepHour: Int,
    val endSleepMinute: Int,
    val totalTimes: Int,
    val deepSleepTimes: Int,
    val lightSleepTimes: Int,
    val wakeupTimes: Int,
    val sleepDetailList: MutableList<SleepDetailBean>
)

data class SleepDetailBean(
    val startTime: Long,
    val duration: Long,
    val sleepType: Int
)

class SleepBarEntry(
    x: Float,
    yVals: FloatArray,
    val date: String // Add date as a property
) : BarEntry(x, yVals)

class SleepFragment : Fragment() {
    private var _binding: FragmentSleepBinding? = null
    private val binding get() = _binding!!

    private val TAG = "SleepFragment"
    private var userId: Int = -1
    private val repository = UserHealthDataRepository()

    private lateinit var sleepDataChart: LineChart
    private lateinit var sleepDataChartWeekly: BarChart
    private lateinit var sleepDataChartMonthly: BarChart

    private lateinit var tabDay: TextView
    private lateinit var tabWeek: TextView
    private lateinit var tabMonth: TextView

    private lateinit var dateText: TextView
    private lateinit var previousIcon: ImageView
    private lateinit var nextIcon: ImageView
    private lateinit var selectedChartTime: TextView
    private lateinit var selectedChartValue: TextView
    private lateinit var sleepValueText: TextView
    private lateinit var deepSleepValueText: TextView
    private lateinit var lightSleepValueText: TextView
    private lateinit var remValueText: TextView
    private lateinit var awakeValueText: TextView
    private lateinit var sleepScoreText: TextView
    private lateinit var sleepAnalyseValue: TextView

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

    private var sleepData: MutableList<SleepBeanResponse> = mutableListOf()
    private val ringSleepData = mutableListOf<SleepBean>()
    private var sleepDataByDateWise: MutableList<SleepDataByDateWise> = mutableListOf()
//    private val lightSleepList = mutableListOf<SleepDetail>()
//    private val deepSleepList = mutableListOf<SleepDetail>()
//    private val awakeTimeList = mutableListOf<SleepDetail>()
//    private val remSleepList = mutableListOf<SleepDetail>()

    private var isRingDataReady = false
    private var isApiDataReady = false

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_sleep, container, false)

        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("id", -1)

        selectedChartTime = view.findViewById(R.id.selectedChartTime)
        selectedChartValue = view.findViewById(R.id.selectedChartValue)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabDay = view.findViewById(R.id.tabDay)
        tabWeek = view.findViewById(R.id.tabWeek)
        tabMonth = view.findViewById(R.id.tabMonth)

        dateText = view.findViewById(R.id.dateText)
        previousIcon = view.findViewById(R.id.previousIcon)
        nextIcon = view.findViewById(R.id.nextIcon)

        sleepValueText = view.findViewById(R.id.sleepValueText)
        deepSleepValueText = view.findViewById(R.id.deepSleepValueText)
        lightSleepValueText = view.findViewById(R.id.lightSleepValueText)
        remValueText = view.findViewById(R.id.remValueText)
        awakeValueText = view.findViewById(R.id.awakeValueText)
        sleepScoreText = view.findViewById(R.id.sleepScoreText)
        sleepAnalyseValue = view.findViewById(R.id.sleepAnalyseValue)

        sleepDataChart = view.findViewById(R.id.sleepDataChart)
        sleepDataChartWeekly = view.findViewById(R.id.sleepDataChartWeekly)
        sleepDataChartMonthly = view.findViewById(R.id.sleepDataChartMonthly)

        val tabs = listOf(tabDay, tabWeek, tabMonth)

        // Set initial selection to "Day"
        selectTab(tabDay, tabs)
        isDayMode = true

        // Set click listeners for tabs
        tabDay.setOnClickListener {
            sleepDataChart.visibility = View.VISIBLE
            sleepDataChartWeekly.visibility = View.GONE
            sleepDataChartMonthly.visibility = View.GONE
            isDayMode = true
            isWeekMode = false
            isMonthMode = false
            selectTab(tabDay, tabs)
            updateDate()
        }
        tabWeek.setOnClickListener {
            sleepDataChart.visibility = View.GONE
            sleepDataChartWeekly.visibility = View.VISIBLE
            sleepDataChartMonthly.visibility = View.GONE
            isDayMode = false
            isWeekMode = true
            isMonthMode = false
            selectTab(tabWeek, tabs)
            updateWeekDates()
        }
        tabMonth.setOnClickListener {
            sleepDataChart.visibility = View.GONE
            sleepDataChartWeekly.visibility = View.GONE
            sleepDataChartMonthly.visibility = View.VISIBLE
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

        fetchSleepDataFromApi()
        fetchSleepHistoryFromRing()
        updateDate()
        setupChart()
        setupWeeklySleepChart()
        setupMonthlySleepChart()
    }

    private fun fetchSleepHistoryFromRing() {
        YCBTClient.healthHistoryData(Constants.DATATYPE.Health_HistorySleep, object : BleDataResponse {
            override fun onDataResponse(responseCode: Int, ratio: Float, resultMap: HashMap<*, *>?) {
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

                ringSleepData.clear()

                dataList.forEach { item ->
                    val sleepDetailList = mutableListOf<SleepDetailBean>()
                    val sleepDataList = item["sleepData"] as? List<HashMap<String, Any>>

                    sleepDataList?.forEach { sleepItem ->
                        // Start time in seconds
                        val startTime = ((sleepItem["sleepStartTime"] as? Number)?.toLong() ?: 0L) / 1000
                        val duration = ((sleepItem["sleepLen"] as? Number)?.toLong() ?: 0L) / 1000

                        val rawSleepType = (sleepItem["sleepType"] as? Number)?.toInt() ?: 0
                        val sleepType = when (rawSleepType) {
                            241 -> 0 // Awake
                            242 -> 2 // Light
                            243 -> 1 // Deep
                            else -> 3 // REM / Unknown
                        }

                        sleepDetailList.add(SleepDetailBean(startTime, duration, sleepType))
                    }

                    val startTimeMillis = (item["startTime"] as? Number)?.toLong() ?: 0L
                    val endTimeMillis = (item["endTime"] as? Number)?.toLong() ?: 0L

                    val calendarStart = Calendar.getInstance().apply { timeInMillis = startTimeMillis }
                    val calendarEnd = Calendar.getInstance().apply { timeInMillis = endTimeMillis }

                    val deepSleepTimes = (item["deepSleepTotal"] as? Number)?.toInt() ?: 0
                    val lightSleepTimes = (item["lightSleepTotal"] as? Number)?.toInt() ?: 0
                    val wakeupTimes = (item["wakeCount"] as? Number)?.toInt() ?: 0
                    val totalTimes = deepSleepTimes + lightSleepTimes + wakeupTimes

                    val sleepBean = SleepBean(
                        statisticTime = startTimeMillis / 1000, // seconds
                        startSleepHour = calendarStart.get(Calendar.HOUR_OF_DAY),
                        startSleepMinute = calendarStart.get(Calendar.MINUTE),
                        endSleepHour = calendarEnd.get(Calendar.HOUR_OF_DAY),
                        endSleepMinute = calendarEnd.get(Calendar.MINUTE),
                        totalTimes = totalTimes,
                        deepSleepTimes = deepSleepTimes,
                        lightSleepTimes = lightSleepTimes,
                        wakeupTimes = wakeupTimes,
                        sleepDetailList = sleepDetailList
                    )

                    ringSleepData.add(sleepBean)
//                    sendSleepData(sleepBean)
                    Log.i(TAG, "🟢 Converted SleepBean (YCBT → API format): $sleepBean")
                }

                isRingDataReady = true
                tryCompareSleepData()
            }
        })
    }

    private fun fetchSleepDataFromApi() {
        if (userId == -1) {
            Toast.makeText(context, "User ID not found. Please log in again.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        repository.getSleepData(userId).enqueue(object : Callback<SleepResponse> {
            override fun onResponse(call: Call<SleepResponse>, response: Response<SleepResponse>) {
                if (response.isSuccessful) {
                    val sleepResponse = response.body()
                    if (sleepResponse != null) {

                        sleepData.clear()

                        sleepData.addAll(sleepResponse.data)
                        isApiDataReady = true

                        selectedDateSleepDataList()

                        tryCompareSleepData()

                    } else {
                        Log.e("SleepFragment", "Response body is null")
                    }
                } else {
                    Log.e("SleepFragment", "Error: ${response.errorBody()?.string() ?: "Unknown error"}")
                }
            }

            override fun onFailure(call: Call<SleepResponse>, t: Throwable) {
                Log.e("SleepFragment", "API call failed: ${t.message}")
            }
        })
    }

    private fun tryCompareSleepData() {
        Log.e("SleepFragment", "Ring -> $isRingDataReady, $isApiDataReady")
        if (isRingDataReady && isApiDataReady) {
            compareSleepData()
            isRingDataReady = false
            isApiDataReady = false
        }
    }

    private fun compareSleepData() {
        if (ringSleepData.isEmpty()) {
            Log.i("SleepFragment", "No ring sleep data available to compare")
            return
        }

        if (sleepData.isEmpty()) {
            Log.i("SleepFragment", "No API sleep data found, entire ring data is new")
            ringSleepData.forEach { bean ->
                sendSleepData(bean)
            }
            return
        }

        // Convert API dates (from statisticTime) -> dd-MM-yyyy
        val apiDates = sleepData.mapNotNull { response ->
            try {
                val instant = Instant.ofEpochSecond(response.statisticTime)
                val dateTime = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Kolkata"))
                dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            } catch (e: Exception) {
                null
            }
        }.toSet()

        // Convert Ring dates (from statisticTime) -> dd-MM-yyyy
        val ringDates = ringSleepData.mapNotNull { bean ->
            try {
                val instant = Instant.ofEpochSecond(bean.statisticTime)
                val dateTime = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Kolkata"))
                dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            } catch (e: Exception) {
                null
            }
        }.toSet()

        // Compare sets
        val newDates = ringDates.subtract(apiDates)

        if (newDates.isNotEmpty()) {
            Log.i("SleepFragment", "New sleep data available for: $newDates")
            val newRecords = ringSleepData.filter { bean ->
                val instant = Instant.ofEpochSecond(bean.statisticTime)
                val dateStr = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Kolkata")).format(formatter)
                newDates.contains(dateStr)
            }

            if (newRecords.isNotEmpty()) {
                newRecords.forEach { bean ->
                    sendSleepData(bean) // upload one by one
                }
            }
        } else {
            Log.i("SleepFragment", "Sleep data is up to date with API")
        }
    }

    private fun sendSleepData(sleepBean: SleepBean) {
        repository.saveSleepData(userId, sleepBean).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.i(TAG, "Saved sleep record ${sleepBean.statisticTime} for user $userId")
                } else {
                    Log.e(TAG, "Failed to upload sleep record: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e(TAG, "Upload failed: ${t.message}")
            }
        })
    }

    private fun selectTab(selectedTab: TextView, allTabs: List<TextView>) {
        allTabs.forEach { it.isSelected = false }
        selectedTab.isSelected = true
    }

    private fun selectedDateSleepDataList() {
        if (sleepData.isEmpty()) {
            Log.i("Filtered Sleep Data", "No data available to filter.")
            return
        }
        // Filter heart rate data based on the selected date
        val filteredData = sleepData.filter { data ->
            try {
                var createdDate: String? = null
                val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
                    .withZone(ZoneId.of("UTC"))
                if (data.statisticTime != null) {
                    val instant = Instant.ofEpochSecond(data.statisticTime.toLong())
                    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"))
                    createdDate = dateTime.format(inputFormatter)
                } else {
                    createdDate = data.created_at
                }
                val createdAtInstant = Instant.from(inputFormatter.parse(createdDate))
                val createdAtDate = createdAtInstant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()
                createdAtDate.isEqual(selectedDate)
            } catch (e: Exception) {
                Log.e("Filtered Sleep Data", "Error parsing date: ${data.created_at}", e)
                false
            }
        }

        populateChart(filteredData)
        getSleepData(filteredData)
        calculateSleepScore(filteredData)
    }

    private fun selectedWeekSleepDataList() {
        if (sleepData.isEmpty()) {
            Log.i("Filtered Sleep Data", "No Sleep data available to filter.")
            return
        }

        // Log initial data
        Log.d("Filtered Sleep Data", "Initial Data: $sleepData")

        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
            .withZone(ZoneId.of("UTC"))

        // Filter data based on the selected week
        val filteredData = sleepData.filter { data ->
            try {
                var createdDate: String? = null
                if (data.statisticTime != null) {
                    // Convert epoch time to Instant
                    val instant = Instant.ofEpochSecond(data.statisticTime.toLong())
                    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"))
                    createdDate = dateTime.format(inputFormatter)
                } else {
                    // Use created_at directly
                    createdDate = data.created_at
                }

                // Parse the date string and convert to LocalDate
                val createdAtInstant = Instant.from(inputFormatter.parse(createdDate))
                val createdAtDate = createdAtInstant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()

                // Check if the date is within the selected week range
                val isInWeekRange = createdAtDate in weekStartDate..weekEndDate
                Log.d(
                    "Date Filtering",
                    "Date: $createdDate, Parsed: $createdAtDate, In Range: $isInWeekRange (Start: $weekStartDate, End: $weekEndDate)"
                )
                isInWeekRange
            } catch (e: Exception) {
                Log.e("Date Parsing Error", "Error parsing date: ${data.created_at}", e)
                false
            }
        }.toMutableList()

        // Log filtered data
        Log.d("Filtered Sleep Data", "Filtered Data: $filteredData")

        // Populate the chart with filtered data
        populateWeeklySleepChart(filteredData)
        getSleepDailyData(filteredData)
        calculateSleepScore(filteredData)
    }

    private fun selectedMonthSleepDataList() {
        if (sleepData.isEmpty()) {
            Log.i("Filtered Sleep Data", "No Sleep data available to filter.")
            return
        }

        // Log initial data
        Log.d("Filtered Sleep Data", "Initial Data: $sleepData")

        // Filter data based on the selected month
        val filteredData = sleepData.filter { data ->
            try {
                // Use statisticTime (epoch time) to determine the date
                val instant = Instant.ofEpochSecond(data.statisticTime.toLong())
                val localDate =
                    instant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate() // Convert to IST

                // Check if the date is within the selected month range
                val isInMonthRange = localDate in monthStartDate..monthEndDate
                Log.d(
                    "Date Filtering",
                    "Date: $localDate, In Range: $isInMonthRange (Start: $monthStartDate, End: $monthEndDate)"
                )
                isInMonthRange
            } catch (e: Exception) {
                Log.e("Date Parsing Error", "Error parsing statisticTime: ${data.statisticTime}", e)
                false
            }
        }.toMutableList()

        // Log filtered data
        Log.d("Filtered Sleep Data", "Filtered Data: $filteredData")

        // Populate the chart with filtered data
        populateMonthlySleepChart(filteredData)
        getSleepDailyData(filteredData)
        calculateSleepScore(filteredData)
    }

    private fun calculateSleepScore(data: List<SleepBeanResponse>) {
        // If no data, set score and efficiency to 0 and return
        if (data.isEmpty()) {
            sleepScoreText.text = "0"
            sleepAnalyseValue.text = "0%"
            return
        }

        // Calculate total sleep duration (all_times) and total awake time
        var totalSleepDuration = 0
        var totalAwakeTime = 0

        data.forEach { entry ->
            // Calculate total sleep duration for this entry
            totalSleepDuration += entry.lightSleepTimes + entry.deepSleepTimes + entry.wakeupTimes +
                    entry.sleep_details
                        .filter { it.sleepType == 3 } // Filter for REM sleep
                        .sumOf { ((it.endTime - it.startTime) / 60).toInt() } // Convert to minutes

            // Calculate total awake time for this entry
            totalAwakeTime += entry.wakeupTimes
        }

        // Calculate sleep efficiency
        val sleepEfficiency = if (totalSleepDuration > 0) {
            ((totalSleepDuration - totalAwakeTime).toDouble() / totalSleepDuration) * 100
        } else {
            0.0
        }

        // Update the sleepAnalyseValue view with the calculated efficiency
        sleepAnalyseValue.text = "${sleepEfficiency.toInt()}%"

        // Calculate the sleep score for each entry and store in a list
        val sleepScores = data.map { entry ->
            // Calculate total sleep duration for this entry
            val totalSleepDurationEntry = entry.lightSleepTimes + entry.deepSleepTimes + entry.wakeupTimes +
                    entry.sleep_details
                        .filter { it.sleepType == 3 } // Filter for REM sleep
                        .sumOf { ((it.endTime - it.startTime) / 60).toInt() } // Convert to minutes

            // Calculate the sleep score for this entry
            val sleepScore = (totalSleepDurationEntry / 480.0 * 100).toInt()

            // Cap the score between 12.5 and 100
            when {
                sleepScore > 100 -> 100
                sleepScore < 12.5 -> 12
                else -> sleepScore
            }
        }

        // Calculate the average sleep score
        val averageScore = sleepScores.average().toInt()

        // Update the sleepScoreText view with the average score
        sleepScoreText.text = averageScore.toString()
    }

    private fun setupChart() {
        // Disable chart description
        sleepDataChart.description.isEnabled = false

        // Enable touch interactions
        sleepDataChart.setTouchEnabled(true)
        sleepDataChart.isDragEnabled = true
        sleepDataChart.setScaleEnabled(true)
        sleepDataChart.setPinchZoom(true)
        sleepDataChart.isScaleXEnabled = true
        sleepDataChart.isScaleYEnabled = false

        // Configure Y-axis
        sleepDataChart.axisLeft.apply {
            isEnabled = true
            axisMinimum = 0f
            axisMaximum = 5f // Adjust based on the max Y-value (4 + 1 for padding)
            setDrawLabels(false) // Hide labels
            setDrawGridLines(false)
        }
        sleepDataChart.axisRight.isEnabled = false

        // Configure X-axis
        val xAxis = sleepDataChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f // Label every hour
        xAxis.labelCount = 6 // Show 6 labels at a time
        xAxis.axisMinimum = 0f

        // Format X-axis labels as time
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val hour = value.toInt()
                val minute = ((value - hour) * 60).toInt()
                return String.format("%02d:%02d", hour, minute)
            }
        }

        // Configure legend
        sleepDataChart.legend.isEnabled = true

        // Enable zooming and scrolling
        sleepDataChart.setScaleEnabled(true)
        sleepDataChart.setPinchZoom(true)
        sleepDataChart.isScaleXEnabled = true
        sleepDataChart.isScaleYEnabled = false

        // Add click listener (optional)
        sleepDataChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e != null) {
                    val normalizedHour = e.x % 24
                    val hour = normalizedHour.toInt()
                    val minute = ((normalizedHour - hour) * 60).toInt()
                    val timeFormatted = String.format("%02d:%02d", hour, minute)

                    // Get the selected sleep type based on the Y-value
                    val sleepType = when (e.y) {
                        1f -> "Light Sleep"
                        2f -> "Deep Sleep"
                        3f -> "Wide Awake"
                        4f -> "REM Sleep"
                        else -> "Unknown"
                    }

                    // Update the UI with the selected time and sleep type
                    selectedChartTime.text = timeFormatted
                    selectedChartValue.text = sleepType

                    Log.i(
                        "Sleep Data Chart Click",
                        "Selected Point - Time: $timeFormatted, Sleep Type: $sleepType"
                    )
                }
            }

            override fun onNothingSelected() {
                Log.i("Sleep Data Chart Click", "No point selected")
            }
        })
    }

    private fun populateChart(sleepDataList: List<SleepBeanResponse>) {
        Log.i("Sleep Data", "chart data count: ${sleepDataList.size}")
        Log.i("Sleep Data", "Raw sleepDataList: ${Gson().toJson(sleepDataList)}")

        // GMT+05:30 offset in seconds
        val timezoneOffsetSeconds = 5 * 3600 + 30 * 60

        // Lists to hold line entries for each sleep type
        val lightSleepEntries = mutableListOf<Entry>()  // Sleep Type 0
        val deepSleepEntries = mutableListOf<Entry>()   // Sleep Type 1
        val wideAwakeEntries = mutableListOf<Entry>()  // Sleep Type 2
        val remSleepEntries = mutableListOf<Entry>()    // Sleep Type 3

        // Track min and max hours for axis range
        var minStartHour = Float.MAX_VALUE
        var maxEndHour = Float.MIN_VALUE

        val prevEndTime = mutableMapOf<Int, Float>() // Store the last end time for each sleep type

        sleepDataList.forEach { sleepData ->
            sleepData.sleep_details.forEach { detail ->
                try {
                    // Adjust timestamps for GMT+05:30
                    val adjustedStartTime = (detail.startTime + timezoneOffsetSeconds) / 3600f
                    val adjustedEndTime = (detail.endTime + timezoneOffsetSeconds) / 3600f

                    // Update min and max hours
                    minStartHour = minOf(minStartHour, adjustedStartTime)
                    maxEndHour = maxOf(maxEndHour, adjustedEndTime)

                    val sleepType = detail.sleepType
                    val entries = when (sleepType) {
                        0 -> lightSleepEntries
                        1 -> deepSleepEntries
                        2 -> wideAwakeEntries
                        3 -> remSleepEntries
                        else -> null
                    }

                    entries?.let {
                        // Add break if there was a previous segment
                        prevEndTime[sleepType]?.let { previousEnd ->
                            if (adjustedStartTime > previousEnd + 0.01f) {
                                it.add(Entry(previousEnd, 0f))  // End previous
                                it.add(Entry(adjustedStartTime, 0f)) // Start new with a break
                            }
                        }

                        // Add the new sleep segment
                        it.add(Entry(adjustedStartTime, sleepType + 1f))
                        it.add(Entry(adjustedEndTime, sleepType + 1f))

                        // Store current end time for the next iteration
                        prevEndTime[sleepType] = adjustedEndTime
                    }
                } catch (e: Exception) {
                    Log.e("Sleep Data", "Error parsing sleep details", e)
                }
            }
        }


        // Check if there's data to display
        if (lightSleepEntries.isEmpty() && deepSleepEntries.isEmpty() &&
            wideAwakeEntries.isEmpty() && remSleepEntries.isEmpty()
        ) {
            Log.i("Sleep Data", "No valid data to display on the chart.")
            sleepDataChart.data = null
            sleepDataChart.invalidate()
            return
        }

        // Create LineDataSets for each sleep type
        val lightSleepDataSet = LineDataSet(lightSleepEntries, "Light Sleep").apply {
            color = Color.LTGRAY
            setDrawCircles(false) // Hide circles
            setDrawValues(false) // Hide values
            lineWidth = 0f
            setDrawFilled(true) // Fill the area under the line
            fillColor = Color.LTGRAY
            fillAlpha = 128 // Semi-transparent fill
        }
        val deepSleepDataSet = LineDataSet(deepSleepEntries, "Deep Sleep").apply {
            color = Color.BLUE
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 0f
            setDrawFilled(true)
            fillColor = Color.BLUE
            fillAlpha = 128
        }
        val wideAwakeDataSet = LineDataSet(wideAwakeEntries, "Wide Awake").apply {
            color = Color.GREEN
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 0f
            setDrawFilled(true)
            fillColor = Color.GREEN
            fillAlpha = 128
        }
        val remSleepDataSet = LineDataSet(remSleepEntries, "REM Sleep").apply {
            color = Color.CYAN
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 0f
            setDrawFilled(true)
            fillColor = Color.CYAN
            fillAlpha = 128
        }

        // Combine datasets into LineData
        val lineData =
            LineData(lightSleepDataSet, deepSleepDataSet, wideAwakeDataSet, remSleepDataSet)

        // Configure chart
        sleepDataChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            data = lineData
            invalidate()

            // X-axis configuration
            xAxis.apply {
                axisMinimum = minStartHour - 1f
                axisMaximum = maxEndHour + 1f
                granularity = 1f
                labelCount = 7
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val normalizedHour = value % 24
                        return String.format("%02d:00", normalizedHour.toInt())
                    }
                }
            }

            // Y-axis configuration
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 5f
                setDrawGridLines(false)
            }
            axisRight.isEnabled = false

            // Enable zooming and scrolling
            setScaleEnabled(true)
            setPinchZoom(true)
            isScaleXEnabled = true
            isScaleYEnabled = false

            // Set visible range dynamically
            setVisibleXRangeMaximum(maxEndHour - minStartHour + 2f)
        }
    }

    private fun setupWeeklySleepChart() {
        sleepDataChartWeekly.apply {
            description.isEnabled = false // Disable chart description
            setTouchEnabled(true) // Enable touch interactions
            isDragEnabled = true // Enable dragging
            setScaleEnabled(true) // Enable scaling
            setPinchZoom(true) // Enable pinch zoom
            isScaleXEnabled = true // Enable X-axis scaling
            isScaleYEnabled = false // Disable Y-axis scaling

            // Configure grid lines and axis
            axisLeft.setDrawGridLines(true)
            axisRight.setDrawGridLines(false)
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawAxisLine(false)

            axisLeft.gridColor = Color.LTGRAY
            axisLeft.gridLineWidth = 0.5f

            // Configure X-axis for weekly data
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

            // Enable dragging to load previous weeks
            setVisibleXRangeMaximum(7f) // Show 7 labels at a time

            // Add listener for selecting data points (optional)
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e != null && e is SleepBarEntry && h != null) {
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

                        // Get the date from the SleepBarEntry
                        val date = e.date

                        // Get the sleep values (deep, light, REM, awake)
                        val values = e.yVals

                        // Determine which stack was clicked
                        val stackIndex = h.stackIndex // Index of the clicked stack
                        val stackLabel = when (stackIndex) {
                            0 -> "Deep sleep"
                            1 -> "Light sleep"
                            2 -> "Rapid eye movement"
                            3 -> "Wide awake"
                            else -> "Unknown"
                        }

                        // Get the value of the clicked stack
                        val clickedValue = values[stackIndex].toInt()

                        // Convert minutes to hours and minutes format
                        val timeText = convertMinutesToHoursMinutes(clickedValue)

                        // Display the selected bar's date and clicked stack's value
                        selectedChartTime.text = date
                        selectedChartValue.text = "$stackLabel $timeText"

                        Log.i(
                            "Sleep Data Chart",
                            "Selected Point - Day: $dayLabel, Date: $date, Stack: $stackLabel, Value: $clickedValue"
                        )
                    }
                }

                override fun onNothingSelected() {
                    Log.i("Sleep Data Chart", "No point selected")
                }
            })
        }
    }

    private fun convertMinutesToHoursMinutes(minutes: Int): String {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return "$hours Hour $remainingMinutes Min"
    }

    private fun populateWeeklySleepChart(filteredData: List<SleepBeanResponse>) {
        val entries = mutableListOf<SleepBarEntry>()
        val daysOfWeek = mutableListOf<String>()

        // Prepare entries for the chart
        filteredData.forEach { data ->
            try {

                val instant = Instant.ofEpochSecond(data.statisticTime.toLong())
                val localDate = instant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()

                // Map the day of the week to an X-axis position (0 = Monday, 6 = Sunday)
                val xValue = when (localDate.dayOfWeek) {
                    DayOfWeek.MONDAY -> 0f
                    DayOfWeek.TUESDAY -> 1f
                    DayOfWeek.WEDNESDAY -> 2f
                    DayOfWeek.THURSDAY -> 3f
                    DayOfWeek.FRIDAY -> 4f
                    DayOfWeek.SATURDAY -> 5f
                    DayOfWeek.SUNDAY -> 6f
                    else -> -1f // Invalid
                }

                if (xValue != -1f) {
                    // Calculate REM time
                    val remTime =
                        data.totalTimes - (data.lightSleepTimes + data.deepSleepTimes + data.wakeupTimes)

                    // Create a BarEntry with the sleep types as the stack values
                    val sleepValues = floatArrayOf(
                        data.deepSleepTimes.toFloat(), // Deep sleep (bottom)
                        data.lightSleepTimes.toFloat(), // Light sleep (above deep sleep)
                        remTime.toFloat(), // REM (above light sleep)
                        data.wakeupTimes.toFloat() // Wide awake (top)
                    )
                    entries.add(SleepBarEntry(xValue, sleepValues, localDate.toString()))
                    daysOfWeek.add(localDate.toString())
                }
            } catch (e: Exception) {
                Log.e(
                    "Sleep Data",
                    "Error parsing statisticTime for chart: ${data.statisticTime}",
                    e
                )
            }
        }

        // Configure Y-axis range
        val maxYValue = entries.maxOfOrNull { it.yVals?.sum() ?: 0f } ?: 100f
        val leftAxis = sleepDataChartWeekly.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = maxYValue + 10f // Add some padding
        sleepDataChartWeekly.axisRight.isEnabled = false

        if (entries.isNotEmpty()) {
            // Create BarDataSet for the stacked data
            val dataSet = BarDataSet(entries as List<BarEntry>?, "").apply { // Empty label
                colors = listOf(
                    Color.parseColor("#7326FE"), // Deep sleep
                    Color.parseColor("#C8A7FF"), // Light sleep
                    Color.parseColor("#69E2EE"), // REM
                    Color.parseColor("#5BE343")  // Wide awake
                )
                stackLabels =
                    arrayOf("Deep sleep", "Light sleep", "Rapid eye movement", "Wide awake")
                setDrawValues(false) // Hide values on bars
            }

            // Enable the legend
            sleepDataChartWeekly.legend.isEnabled = true
            sleepDataChartWeekly.legend.textColor = Color.BLACK
            sleepDataChartWeekly.legend.formSize = 10f
            sleepDataChartWeekly.legend.form = Legend.LegendForm.SQUARE

            // Update chart data
            val barData = BarData(dataSet)
            barData.barWidth = 0.5f // Adjust bar width
            sleepDataChartWeekly.data = barData

            // Ensure the last data point is in view
            val lastEntryX = entries.lastOrNull()?.x ?: 6f
            sleepDataChartWeekly.setVisibleXRangeMaximum(7f)
            sleepDataChartWeekly.moveViewToX(lastEntryX - 3f) // Show 3 days before the last data point

            sleepDataChartWeekly.invalidate()
        } else {
            Log.i("Sleep Data", "No valid data to display on the chart.")
            sleepDataChartWeekly.data = null
            sleepDataChartWeekly.invalidate()
        }
    }

    private fun setupMonthlySleepChart() {
        sleepDataChartMonthly.apply {
            description.isEnabled = false // Disable chart description
            setTouchEnabled(true) // Enable touch interactions
            isDragEnabled = true // Enable dragging
            setScaleEnabled(true) // Enable scaling
            setPinchZoom(true) // Enable pinch zoom
            isScaleXEnabled = true // Enable X-axis scaling
            isScaleYEnabled = false // Disable Y-axis scaling

            // Configure grid lines and axis
            axisLeft.setDrawGridLines(true)
            axisRight.setDrawGridLines(false)
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawAxisLine(false)

            // Configure X-axis for monthly data
            val xAxis = xAxis
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
            setVisibleXRangeMaximum(7f) // Show 7 labels at a time

            // Add listener for selecting data points (optional)
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e != null && e is SleepBarEntry && h != null) {
                        val day = e.x.toInt()
                        val date = e.date // Extract the date

                        // Get the sleep values (deep, light, REM, awake)
                        val values = e.yVals

                        // Determine which stack was clicked
                        val stackIndex = h.stackIndex // Index of the clicked stack
                        val stackLabel = when (stackIndex) {
                            0 -> "Deep sleep"
                            1 -> "Light sleep"
                            2 -> "Rapid eye movement"
                            3 -> "Wide awake"
                            else -> "Unknown"
                        }

                        // Get the value of the clicked stack
                        val clickedValue = values[stackIndex].toInt()

                        // Convert minutes to hours and minutes format
                        val timeText = convertMinutesToHoursMinutes(clickedValue)

                        // Display the selected bar's date and clicked stack's value
                        selectedChartTime.text = date
                        selectedChartValue.text = "$stackLabel: $timeText"

                        Log.i(
                            "Sleep Data Chart",
                            "Selected Point - Day: $day, Date: $date, Stack: $stackLabel, Value: $clickedValue"
                        )
                    }
                }

                override fun onNothingSelected() {
                    Log.i("Sleep Data Chart", "No point selected")
                }
            })
        }
    }

    private fun populateMonthlySleepChart(filteredData: List<SleepBeanResponse>) {
        val entries = mutableListOf<SleepBarEntry>() // Use custom SleepBarEntry class

        // Prepare entries for the chart
        filteredData.forEach { data ->
            try {
                // Use statisticTime (epoch time) to determine the date
                val instant = Instant.ofEpochSecond(data.statisticTime.toLong())
                val localDate =
                    instant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate() // Convert to IST

                // Extract the day of the month (e.g., 14 for "2025-03-14")
                val dayOfMonth = localDate.dayOfMonth

                // Calculate REM time
                val remTime =
                    data.totalTimes - (data.deepSleepTimes + data.lightSleepTimes + data.wakeupTimes)

                // Create a SleepBarEntry with the sleep types as the stack values
                val sleepValues = floatArrayOf(
                    data.deepSleepTimes.toFloat(), // Deep sleep (bottom)
                    data.lightSleepTimes.toFloat(), // Light sleep (above deep sleep)
                    remTime.toFloat(), // REM (above light sleep)
                    data.wakeupTimes.toFloat() // Wide awake (top)
                )
                entries.add(SleepBarEntry(dayOfMonth.toFloat(), sleepValues, localDate.toString()))
            } catch (e: Exception) {
                Log.e(
                    "Sleep Data",
                    "Error parsing statisticTime for chart: ${data.statisticTime}",
                    e
                )
            }
        }

        // Configure Y-axis range
        val maxYValue = entries.maxOfOrNull { it.yVals?.sum() ?: 0f } ?: 100f
        val leftAxis = sleepDataChartMonthly.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = maxYValue + 10f // Add some padding
        sleepDataChartMonthly.axisRight.isEnabled = false

        if (entries.isNotEmpty()) {
            // Create BarDataSet for the stacked data
            val dataSet = BarDataSet(entries as List<BarEntry>?, "").apply { // Empty label
                colors = listOf(
                    Color.parseColor("#7326FE"), // Deep sleep
                    Color.parseColor("#C8A7FF"), // Light sleep
                    Color.parseColor("#69E2EE"), // REM
                    Color.parseColor("#5BE343")  // Wide awake
                )
                stackLabels =
                    arrayOf("Deep sleep", "Light sleep", "Rapid eye movement", "Wide awake")
                setDrawValues(false) // Hide values on bars
            }

            // Enable the legend
            sleepDataChartMonthly.legend.isEnabled = true
            sleepDataChartMonthly.legend.textColor = Color.BLACK
            sleepDataChartMonthly.legend.formSize = 10f
            sleepDataChartMonthly.legend.form = Legend.LegendForm.SQUARE

            // Update chart data
            val barData = BarData(dataSet)
            barData.barWidth = 0.5f // Adjust bar width
            sleepDataChartMonthly.data = barData

            // Ensure the last data point is in view and 2-3 days of the next date are visible
            val lastEntryX = entries.lastOrNull()?.x ?: 31f
            sleepDataChartMonthly.setVisibleXRangeMaximum(7f)
            sleepDataChartMonthly.moveViewToX(lastEntryX - 5f)

            sleepDataChartMonthly.invalidate()
        } else {
            Log.i("Sleep Data", "No valid data to display on the chart.")
            sleepDataChartMonthly.data = null
            sleepDataChartMonthly.invalidate()
        }
    }

    private fun getSleepData(data: List<SleepBeanResponse>) {
        // Reset all text views to default "--" values
        selectedChartTime.text = "--:--"
        selectedChartValue.text = "--"
        sleepValueText.text = "--"
        deepSleepValueText.text = "--"
        lightSleepValueText.text = "--"
        awakeValueText.text = "--"
        remValueText.text = "--"

        if (data.isNotEmpty()) {
            val firstEntry = data.first()

            // Parse the `created_at` field to time (HH:MM)
            try {
                val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
                    .withZone(ZoneId.of("UTC"))
                val createdAtInstant = Instant.from(inputFormatter.parse(firstEntry.created_at))

                // Add +05:30 offset to the UTC time
                val offset = ZoneOffset.ofHoursMinutes(5, 30)
                val offsetDateTime = createdAtInstant.atOffset(offset)
                val localTime = offsetDateTime.toLocalTime()

                val hour = localTime.hour
                val minute = localTime.minute
                val timeFormatted = String.format("%02d:%02d", hour, minute)

                // Update the text views for the first entry
                selectedChartTime.text = timeFormatted

                val sleepRecord = data[0]  // Get first sleep record

                // Assign values from the data class (ensure values are not zero)
                sleepValueText.text =
                    formatMinutesToHours(sleepRecord.deepSleepTimes + sleepRecord.lightSleepTimes).ifBlank { "--" }
                deepSleepValueText.text =
                    formatMinutesToHours(sleepRecord.deepSleepTimes).ifBlank { "--" }
                lightSleepValueText.text =
                    formatMinutesToHours(sleepRecord.lightSleepTimes).ifBlank { "--" }
                awakeValueText.text = formatMinutesToHours(sleepRecord.wakeupTimes).ifBlank { "--" }

                // Directly filter REM sleep and assign its value
                val remSleepTime = sleepRecord.sleep_details
                    .filter { it.sleepType == 3 }
                    .sumOf { ((it.endTime - it.startTime) / 60).toInt() }  // Convert to minutes
                remValueText.text = formatMinutesToHours(remSleepTime).ifBlank { "--" }

                // Get the first start time and sleep type from sleep_details
                if (sleepRecord.sleep_details.isNotEmpty()) {
                    val firstSleepDetail = sleepRecord.sleep_details.first()

                    // Convert start time to hours and minutes (with +05:30 offset)
                    val startTimeSeconds =
                        firstSleepDetail.startTime + (5 * 3600) + (30 * 60) // Add 5 hours and 30 minutes
                    val startHour = (startTimeSeconds % 86400) / 3600  // Convert to hours
                    val startMinute =
                        ((startTimeSeconds % 3600) / 60).toInt()  // Convert to minutes
                    val startTimeFormatted = String.format("%02d:%02d", startHour, startMinute)

                    // Get the sleep type
                    val sleepType = when (firstSleepDetail.sleepType) {
                        0 -> "Light Sleep"
                        1 -> "Deep Sleep"
                        2 -> "Wide Awake"
                        3 -> "REM Sleep"
                        else -> "Unknown"
                    }

                    // Update the UI with the first start time and sleep type
                    selectedChartTime.text = startTimeFormatted.ifBlank { "--:--" }
                    selectedChartValue.text = sleepType.ifBlank { "--" }

                    Log.i(
                        "Sleep Initial Data",
                        "First Start Time: $startTimeFormatted, Sleep Type: $sleepType"
                    )
                } else {
                    Log.i("Sleep Initial Data", "No sleep details available for the first entry.")
                }
            } catch (e: Exception) {
                Log.e(
                    "Sleep Init Data",
                    "Error formatting first entry date: ${firstEntry.created_at}",
                    e
                )
            }
        } else {
            Log.i("Sleep Data", "No data available for the selected date.")
        }
    }

    private fun getSleepDailyData(data: List<SleepBeanResponse>) {
        // Reset all text views to default "--" values
        selectedChartTime.text = "--:--"
        selectedChartValue.text = "--"
        sleepValueText.text = "--"
        deepSleepValueText.text = "--"
        lightSleepValueText.text = "--"
        awakeValueText.text = "--"
        remValueText.text = "--"

        if (data.isNotEmpty()) {
            // Get the last entry from the data
            val lastEntry = data.last()

            try {
                // Parse the `created_at` field to get the date
                val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
                    .withZone(ZoneId.of("UTC"))
                val createdAtInstant = Instant.from(inputFormatter.parse(lastEntry.created_at))

                // Add +05:30 offset to the UTC time
                val offset = ZoneOffset.ofHoursMinutes(5, 30)
                val offsetDateTime = createdAtInstant.atOffset(offset)
                val localDate = offsetDateTime.toLocalDate()

                // Format the date as "yyyy-MM-dd"
                val dateFormatted = localDate.toString()

                // Get the sleep type of the last entry (use the first sleep detail)
                val sleepType = if (lastEntry.sleep_details.isNotEmpty()) {
                    when (lastEntry.sleep_details.first().sleepType) {
                        0 -> "Light Sleep"
                        1 -> "Deep Sleep"
                        2 -> "Wide Awake"
                        3 -> "REM Sleep"
                        else -> "Unknown"
                    }
                } else {
                    "Unknown"
                }

                // Display the last entry's date and sleep type
                selectedChartTime.text = dateFormatted
                selectedChartValue.text = sleepType

                // Calculate the total sum of all sleep types for the filtered data
                val totalDeepSleep = data.sumOf { it.deepSleepTimes }
                val totalLightSleep = data.sumOf { it.lightSleepTimes }
                val totalAwake = data.sumOf { it.wakeupTimes }
                val totalRemSleep = data.sumOf { entry ->
                    entry.sleep_details
                        .filter { it.sleepType == 3 }
                        .sumOf { ((it.endTime - it.startTime) / 60).toInt() } // Convert to minutes
                }

                // Calculate total sleep time (deep + light + REM)
                val totalSleepTime = totalDeepSleep + totalLightSleep + totalRemSleep

                // Update the text views with the total values
                sleepValueText.text = formatMinutesToHours(totalSleepTime).ifBlank { "--" }
                deepSleepValueText.text = formatMinutesToHours(totalDeepSleep).ifBlank { "--" }
                lightSleepValueText.text = formatMinutesToHours(totalLightSleep).ifBlank { "--" }
                awakeValueText.text = formatMinutesToHours(totalAwake).ifBlank { "--" }
                remValueText.text = formatMinutesToHours(totalRemSleep).ifBlank { "--" }

                Log.i(
                    "Sleep Data",
                    "Last Entry - Date: $dateFormatted, Sleep Type: $sleepType"
                )
                Log.i(
                    "Sleep Data",
                    "Total Values - Deep: $totalDeepSleep, Light: $totalLightSleep, Awake: $totalAwake, REM: $totalRemSleep"
                )
            } catch (e: Exception) {
                Log.e(
                    "Sleep Init Data",
                    "Error formatting last entry date: ${lastEntry.created_at}",
                    e
                )
            }
        } else {
            Log.i("Sleep Data", "No data available for the selected week.")
        }
    }

    private fun formatMinutesToHours(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) {
            "$hours h $mins min"
        } else {
            "$mins min"
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
        sleepDataChart.data = null
        sleepDataChart.invalidate()
        selectedDateSleepDataList()
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
        sleepDataChartWeekly.data = null
        sleepDataChartWeekly.invalidate()
        selectedWeekSleepDataList()
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
        sleepDataChartMonthly.data = null
        sleepDataChartMonthly.invalidate()
        selectedMonthSleepDataList()
    }

    private fun goToPreviousMonth() {
        monthStartDate = monthStartDate.minusMonths(1).withDayOfMonth(1)
        monthEndDate = monthStartDate.withDayOfMonth(monthStartDate.lengthOfMonth())
        updateMonthDates()
        Log.d("Previous month requested", "Month range: $monthStartDate to $monthEndDate")
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
        sleepDataChart.data = null
        sleepDataChart.invalidate()
        selectedChartTime.text = "--:--"
        selectedChartValue.text = "--"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
