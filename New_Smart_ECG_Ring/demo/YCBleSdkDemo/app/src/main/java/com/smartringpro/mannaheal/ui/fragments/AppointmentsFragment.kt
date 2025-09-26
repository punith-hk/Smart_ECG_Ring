package com.smartringpro.mannaheal.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.adapter.AppointmentListAdapter
import com.smartringpro.mannaheal.adapter.AppointmentSummaryAdapter
import com.smartringpro.mannaheal.adapter.OnItemClickListener
import com.smartringpro.mannaheal.api.register.SpecializationRepository
import com.smartringpro.mannaheal.api.specializations.interfaces.AppointmentData
import com.smartringpro.mannaheal.api.specializations.interfaces.Appointments
import com.smartringpro.mannaheal.databinding.FragmentAppointmentsBinding
import com.smartringpro.mannaheal.ui.activities.HomeActivity
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AppointmentsFragment : Fragment(), OnItemClickListener {
    private var _binding: FragmentAppointmentsBinding? = null
    private val binding get() = _binding!!
    private var aSummaryList: MutableList<AppointmentData> = mutableListOf()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getAppointmentList()
        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val isAppointmentSummary = sharedPreferences.getInt("isAppointmentSummary", 0)

        if(isAppointmentSummary == 1) {
            binding.listView.visibility = View.GONE
            binding.sListView.visibility = View.VISIBLE
            binding.appointmentSummary.setBackgroundResource(R.drawable.round_button)
            binding.appointments.setBackgroundResource(0)
        } else {
            binding.appointments.setBackgroundResource(R.drawable.round_button)
        }

        val editor = sharedPreferences.edit()
        binding.appointments.setOnClickListener {
            binding.listView.visibility = View.VISIBLE
            binding.sListView.visibility = View.GONE
            binding.appointments.setBackgroundResource(R.drawable.round_button)
            binding.appointmentSummary.setBackgroundResource(0)
            editor.putInt("isAppointmentSummary", 0 )
            editor.apply()
        }
        binding.appointmentSummary.setOnClickListener {
            binding.listView.visibility = View.GONE
            binding.sListView.visibility = View.VISIBLE
            binding.appointmentSummary.setBackgroundResource(R.drawable.round_button)
            binding.appointments.setBackgroundResource(0)
            editor.putInt("isAppointmentSummary", 1 )
            editor.apply()
        }
    }

    private fun getAppointmentList() {
        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("id", -1)
        SpecializationRepository().getAppointments(userId).enqueue(object:Callback<Appointments> {
            override fun onResponse(
                call: Call<Appointments>,
                response: Response<Appointments>
            ) {
                if (!isAdded || isDetached) return
                val apiResp = response.body()!!
                val dataList: List<AppointmentData> = apiResp.data.sortedByDescending { data ->
                    LocalDateTime.parse(
                        "${data.appt_date} ${data.appt_time}",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    )
                }
                dataList.forEach { appointmentData ->
                    if(appointmentData.status == 3) {
                        aSummaryList.add(appointmentData)
                    }
                }
                val adapter = AppointmentListAdapter(requireContext(), dataList,this@AppointmentsFragment)
                binding.listView.adapter = adapter
                val summaryAdapter = AppointmentSummaryAdapter(requireContext(), aSummaryList, this@AppointmentsFragment)
                binding.sListView.adapter = summaryAdapter
                Log.i("getSpecialists", "onResponse: $apiResp")
            }

            override fun onFailure(call: Call<Appointments>, t: Throwable) {
                Log.i("getAppointments", "Error")
            }
        })
    }

    override fun onItemClick(position: Int) {
        val appointmentFragment = AppointmentDetailsFragment()
        val bundle = Bundle()
        bundle.putInt("appointmentId", position)
        appointmentFragment.arguments = bundle
        (activity as HomeActivity).openFragment(appointmentFragment, "Appointment Details", true)
    }
}