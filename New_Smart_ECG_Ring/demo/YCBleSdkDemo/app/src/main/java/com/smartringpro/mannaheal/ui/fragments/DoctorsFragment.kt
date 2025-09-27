package com.smartringpro.mannaheal.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
//import androidx.fragment.app.activityViewModels
import com.smartringpro.mannaheal.adapter.DoctorListAdapter
import com.smartringpro.mannaheal.adapter.OnItemClickListener
import com.smartringpro.mannaheal.api.register.SpecializationRepository
import com.smartringpro.mannaheal.api.specializations.interfaces.Departments
import com.smartringpro.mannaheal.api.specializations.interfaces.Doctor
import com.smartringpro.mannaheal.databinding.FragmentDoctorsBinding
import com.smartringpro.mannaheal.model.SharedViewModel
import com.smartringpro.mannaheal.ui.activities.HomeActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorsFragment : Fragment(), OnItemClickListener {
    private  var _binding: FragmentDoctorsBinding? = null
    private val binding get() = _binding!!
    private lateinit var doctorsList: List<Doctor>
    private val SpecializationRepository = SpecializationRepository()
//    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDoctorsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getDoctors()
    }


    private fun getDoctors() {
        val departmentId = arguments?.getInt("departmentId")
        Log.i("getDoctors", "onResponse: Before cal $departmentId")
        if (departmentId != null) {
            SpecializationRepository.getDoctors(departmentId).enqueue(object  :
                Callback<Departments> {
                override fun onResponse(
                    call: Call<Departments>,
                    response: Response<Departments>
                ) {
                    if (!isAdded || isDetached) return
                    val apiResp = response.body()!!
                    doctorsList = apiResp.data.doctors
                    Log.i("getDoctors", "onResponse: $apiResp")
                    val adapter = DoctorListAdapter(requireContext(), doctorsList, this@DoctorsFragment)

                    binding.listView.adapter = adapter
                }

                override fun onFailure(call: Call<Departments>, t: Throwable) {
                    Log.i("getDoctors", "Error")
                }
            })
        }
    }

    override fun onItemClick(position: Int) {
//        sharedViewModel.setDoctorData(doctorsList[position])
        val clickedId = doctorsList[position].id
        val fragment = SymptomsFragment()
        val bundle = Bundle()
        bundle.putInt("doctorId", clickedId)
        val sharedPreferences = requireContext().getSharedPreferences("SymptomsPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("clearSymptomsData", true)  // Save the flag to clear data
        editor.apply()
        fragment.arguments = bundle
        (activity as HomeActivity).openFragment(fragment, "Book an Appointment", true)
    }
}