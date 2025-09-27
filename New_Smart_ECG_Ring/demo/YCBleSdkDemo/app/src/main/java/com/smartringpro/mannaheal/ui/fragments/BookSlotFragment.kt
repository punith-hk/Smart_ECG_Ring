package com.smartringpro.mannaheal.ui.fragments

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
//import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.adapter.WeekPagerAdapter
import com.smartringpro.mannaheal.api.register.SpecializationRepository
import com.smartringpro.mannaheal.api.specializations.interfaces.AppointmentData
import com.smartringpro.mannaheal.api.specializations.interfaces.Appointments
import com.smartringpro.mannaheal.api.specializations.interfaces.Doctor
import com.smartringpro.mannaheal.api.specializations.interfaces.ScheduleWeek
import com.smartringpro.mannaheal.api.specializations.interfaces.Schedules
import com.smartringpro.mannaheal.api.specializations.interfaces.bookAppointmentResponse
import com.smartringpro.mannaheal.api.symptoms.SaveSymptomsResponse
import com.smartringpro.mannaheal.api.symptoms.SymptomsRepository
import com.smartringpro.mannaheal.databinding.DialogConfirmSlotBinding
import com.smartringpro.mannaheal.databinding.FragmentBookSlotBinding
import com.smartringpro.mannaheal.model.SharedViewModel
import com.smartringpro.mannaheal.ui.activities.HomeActivity
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.properties.Delegates

interface SlotClickListener {
    fun onSlotClicked(time: String, date: String, slot: Int)
}

class BookSlotFragment : Fragment(), SlotClickListener {
    private var _binding: FragmentBookSlotBinding? = null
    private var _dBinding: DialogConfirmSlotBinding? = null
    private val binding get() = _binding!!
    private val dBinding get() = _dBinding!!
    private val SpecializationRepository = SpecializationRepository()
//    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var selectedDoctor: Doctor? = null
    private var appointment_date: String? = null
    private var appointment_time: String? = null
    private var selectedDependant = 0
    private var symptomsMap = mutableMapOf<String, List<String>>()
    private lateinit var timeToSlot: MutableMap<String, Int>
    private var doctorAppointments: List<AppointmentData> = emptyList()
    private lateinit var dialog: AlertDialog
    private var userId by Delegates.notNull<Int>()

    private lateinit var symptomsContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBookSlotBinding.inflate(
            inflater,
            container,
            false
        )// In your Activity or Fragment
        _dBinding = DialogConfirmSlotBinding.inflate(layoutInflater)
        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("id", -1)
        dialog = AlertDialog.Builder(requireContext())
            .setView(_dBinding!!.root)
            .create()
        dBinding.bookAnotherAppointment.setOnClickListener {
            setDoctorAppointments()
            dialog.dismiss()
        }
        dBinding.patientDashboard.setOnClickListener {
            dialog.dismiss()
            (activity as HomeActivity).openFragment(HomeFragment(), "Health", false)
        }
        timeToSlot = getSlotMapping()
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        symptomsContainer = view.findViewById(R.id.symptomsContainer)

        arguments?.let { bundle ->
            for (key in bundle.keySet()) {
                bundle.getStringArrayList(key)?.let { symptomsList ->
                    symptomsMap[key] = symptomsList
                }
            }
        }

        Log.d("BookSlotFragment", "Received Symptoms Data: $symptomsMap")

        if (symptomsMap.isNotEmpty()) {
            updateSymptomsUI(symptomsMap)
        }

        binding.editSymptoms.setOnClickListener {
            (activity as HomeActivity).openFragment(
                SymptomsFragment(),
                "Book an Appointment",
                false
            )
        }

        setDoctorDetails()

        binding.makeAnAppointment.isEnabled = false

        binding.makeAnAppointment.setOnClickListener {
            selectedDoctor?.let { doctor ->
                val symptomsList = mutableListOf<String>()

                arguments?.let { bundle ->
                    for (key in bundle.keySet()) {
                        bundle.getStringArrayList(key)?.let { symptoms ->
                            symptomsList.addAll(symptoms)
                        }
                    }
                }

                val symptomsString =
                    symptomsList.joinToString(", ") // Convert list to a single string

                SpecializationRepository.bookAppointments(
                    appointment_date!!,
                    appointment_time!!,
                    doctor_id = doctor.id,
                    patient_id = userId,
                    purpose = symptomsString, // Pass symptoms as a single string
                    status = 1,
                    dependent_id = selectedDependant, // Ensure it's a string
                    type = 2
                ).enqueue(object : Callback<bookAppointmentResponse> {
                    override fun onResponse(
                        call: Call<bookAppointmentResponse>,
                        response: Response<bookAppointmentResponse>
                    ) {
                        saveSymptomsData()
                        val bookedWith =
                            "Appointment Booked with Dr ${doctor.doctor_name} on $appointment_date $appointment_time"
                        val boldText =
                            "Dr ${doctor.doctor_name} on $appointment_date $appointment_time"
                        val start = bookedWith.indexOf(boldText)
                        val end = start + boldText.length
                        val spannableString = SpannableString(bookedWith)
                        spannableString.setSpan(StyleSpan(Typeface.BOLD), start, end, 0)
                        dBinding.bookedWithText.text = spannableString
                        binding.makeAnAppointment.isEnabled = false
                        dialog.show()
                    }

                    override fun onFailure(call: Call<bookAppointmentResponse>, t: Throwable) {
                        Log.i("Appointment", "Appointment Error: ${t.message}")
                    }
                })
            }
        }


    }

    private fun updateSymptomsUI(symptomsMap: Map<String, List<String>>) {
        symptomsContainer.removeAllViews() // Clear previous views

        for ((bodyPart, symptomsList) in symptomsMap) {
            val symptomsText = symptomsList.joinToString(", ")
            val fullText = "$bodyPart: $symptomsText"
            val spannableString = SpannableString(fullText)

            // Make only the body part bold
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                bodyPart.length + 1, // Include the colon (:)
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val symptomText = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4.dpToPx(), 0, 4.dpToPx()) // Add margins
                }
                text = spannableString
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            }

            symptomsContainer.addView(symptomText)
        }
    }


    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun setDoctorSchedules() {
        SpecializationRepository.getSchedules(1).enqueue(object :
            Callback<Schedules> {
            override fun onResponse(
                call: Call<Schedules>,
                response: Response<Schedules>
            ) {
                val apiResp = response.body()!!
                if (apiResp.response == 0) {
                    val next14Days = getNext14Days(apiResp.data, apiResp.leaves)
                    val splitLists = next14Days.chunked(3)
                    val weekAdapter = WeekPagerAdapter(splitLists, this@BookSlotFragment)
                    binding.tvMonthYear.text = splitLists[0][0].month
                    binding.viewPager.adapter = weekAdapter
                    binding.btnPrevWeek.visibility = View.INVISIBLE
                    binding.btnNextWeek.setOnClickListener {
                        if (binding.viewPager.currentItem < weekAdapter.itemCount - 1) {
                            binding.viewPager.currentItem += 1 // Move to the next week
                        }
                        if (binding.viewPager.currentItem == weekAdapter.itemCount - 1) {
                            binding.btnNextWeek.visibility = View.INVISIBLE
                        }
                        binding.btnPrevWeek.visibility = View.VISIBLE
                        binding.tvMonthYear.text =
                            splitLists[binding.viewPager.currentItem][0].month
                    }
                    binding.btnPrevWeek.setOnClickListener {
                        if (binding.viewPager.currentItem > 0) {
                            binding.viewPager.currentItem -= 1 // Move to the previous week
                            binding.btnNextWeek.visibility = View.VISIBLE
                        }
                        if (binding.viewPager.currentItem == 0) {
                            binding.btnPrevWeek.visibility = View.INVISIBLE
                        }
                        binding.tvMonthYear.text =
                            splitLists[binding.viewPager.currentItem][0].month
                    }
                }
            }

            override fun onFailure(call: Call<Schedules>, t: Throwable) {
                Log.i("getDoctors", "Error")
            }
        })
    }

    private fun setDoctorDetails() {

//        binding.doctorCard.bookNow.visibility = View.GONE
//        sharedViewModel.doctorData.observe(viewLifecycleOwner, Observer { data ->
//            // Update your UI with the shared data
//            selectedDoctor = data
//            binding.doctorCard.doctorName.text = data.doctor_name
//            binding.doctorCard.doctorsDegree.text = data.education
//            binding.doctorCard.doctorsSpecialization.text = data.doctor_department
//            val address = "${data.city}, ${data.state}, ${data.country}"
//            val textView = TextView(context)
//            textView.text = address
//            TextViewCompat.setTextAppearance(textView, R.style.bind_the_de)
//            textView.layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            )
////            binding.doctorCard.subtextLayout.addView(textView)
//
//            val imageUri: Uri = Uri.parse(data.doctor_image_url)
//            try {
//                Glide.with(requireContext())
//                    .load(imageUri)
//                    .into(binding.doctorCard.doctorImageIcon)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//            setDoctorAppointments()
//        })

    }

    fun setDoctorAppointments() {
        SpecializationRepository.getDoctorAppointments(selectedDoctor!!.id).enqueue(object :
            Callback<Appointments> {
            override fun onResponse(
                call: Call<Appointments>,
                response: Response<Appointments>
            ) {
                val apiResp = response.body()!!
                if (apiResp.response == 0) {
                    doctorAppointments = apiResp.data
                    setDoctorSchedules()
                }
            }

            override fun onFailure(call: Call<Appointments>, t: Throwable) {
                Log.i("getDoctors", "Error")
            }
        })
    }

    fun getNext14Days(data: List<ScheduleWeek>, leaves: List<String>): List<ScheduleWeek> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val dayWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dayFormat = SimpleDateFormat("E", Locale.getDefault())
        val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val selectedDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val leaveDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return (0 until 15).map {
            val date = dateFormat.format(calendar.time)
            val dayWeek = dayWeekFormat.format(calendar.time)
            val day = dayFormat.format(calendar.time)
            val monthYear = monthYearFormat.format(calendar.time)
            val selectedDate = selectedDateFormat.format(calendar.time)
            val leaveDate = leaveDateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            if (leaves.contains(leaveDate)) {
                ScheduleWeek(day, date, monthYear, selectedDate, listOf<Int>(), null)
            } else {
                if (data.any { x -> x.day == dayWeek }) {
                    val dayData = data.find { x -> x.day == dayWeek }
                    val bookedSlots = getBookedSlots(selectedDate)
                    ScheduleWeek(
                        day,
                        date,
                        monthYear,
                        selectedDate,
                        dayData!!.time_slots,
                        null,
                        bookedSlots
                    )
                } else {
                    ScheduleWeek(day, date, monthYear, selectedDate, listOf<Int>(), null)
                }
            }
        }
    }

    fun getBookedSlots(date: String): MutableList<Int> {
        val slots: MutableList<Int> = mutableListOf()
        doctorAppointments.forEach { appointmentData ->
            if (date == appointmentData.appt_date) {
                for ((time, counter) in timeToSlot) {
                    if (time == appointmentData.appt_time) {
                        slots.add(counter)
                        continue
                    }
                }
            }
        }
        return slots
    }

    override fun onSlotClicked(time: String, date: String, slot: Int) {
        val convertedTime = convertTime(time)
        binding.makeAnAppointment.isEnabled = true
        appointment_date = date
        appointment_time = convertedTime
    }

    fun getSlotMapping(): MutableMap<String, Int> {
        val startTime = LocalTime.of(9, 0) // 09:00:00
        val endTime = LocalTime.of(21, 0) // 21:00:00
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        var time = startTime
        var counter = 1

        val timeMap = mutableMapOf<String, Int>()

        while (time <= endTime) {
            timeMap[time.format(formatter)] = counter
            time = time.plusMinutes(30)
            counter++
        }
        return timeMap
    }

    fun convertTime(time: String): String {
        val inputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(time)
        return outputFormat.format(date!!)
    }


    private  fun saveSymptomsData() {
        val userId = userId
        val dependentId = 0

        val gson = Gson()
        val formattedJson = gson.toJson(symptomsMap)

        val repository = SymptomsRepository()
        val call = repository.saveSymptomsData(userId, dependentId, "$appointment_date $appointment_time", formattedJson)

        call.enqueue(object : Callback<SaveSymptomsResponse> {
            override fun onResponse(
                call: Call<SaveSymptomsResponse>,
                response: Response<SaveSymptomsResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val saveSymptomsResponse = response.body()
                    Log.d("symptoms fragment", "Saved Symptoms Response: $saveSymptomsResponse")

                } else {
                    Log.e("ProfileFragment", "Error: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "symptoms fragment", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<SaveSymptomsResponse>, t: Throwable) {
                Log.e("symptoms fragment", "Failure: ${t.message}")
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dialog.isShowing()) {
            dialog.dismiss()
        }
    }
}

//appointment_date: 2025-02-11
//appointment_time: 17:30:00
//doctor_id: 1
//patient_id: 21
//purpose: General Checkup
//status: 1
//dependent_id: 0
//type: 2