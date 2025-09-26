package com.smartringpro.mannaheal.ui.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.smartringpro.mannaheal.adapter.ListAdapter
import com.smartringpro.mannaheal.adapter.OnItemClickListener
import com.smartringpro.mannaheal.api.specializations.interfaces.Specializations
import com.smartringpro.mannaheal.databinding.FragmentSpecialistsBinding
import com.smartringpro.mannaheal.ui.activities.HomeActivity
import com.smartringpro.mannaheal.ui.fragments.DoctorsFragment

class SpecialistsFragment : Fragment(), OnItemClickListener {
    private var _binding: FragmentSpecialistsBinding? = null
    private val binding get() = _binding!!
    private val specialistsList: List<Specializations> = buildList {
        add(Specializations(1,"General Physician","gd.png"))
        add(Specializations(3,"Cardiology", "cardio.jfif"))
        add(Specializations(4,"Pediatrician", "pedes.jfif"))
        add(Specializations(4,"Dermatology", "demi.jfif"))
        add(Specializations(5,"Psychiatrist", "psyhc.jfif"))
        add(Specializations(6,"Others", "other.jfif"))
    }
//    private val SpecializationRepository = SpecializationRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpecialistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("FragmentTitle", "" )
        editor.apply()
        getSpecialists()
    }

    override fun onItemClick(position: Int) {
        val clickedId = specialistsList[position].department_id
        val doctorsFragment = DoctorsFragment()
        val bundle = Bundle()
        bundle.putInt("departmentId", clickedId)
        doctorsFragment.arguments = bundle
        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("FragmentTitle", specialistsList[position].description )
        editor.apply()
        (activity as HomeActivity).openFragment(doctorsFragment, specialistsList[position].description, true)
    }

    private fun getSpecialists() {
//        SpecializationRepository.getSpecialists().enqueue(object  : Callback<List<Specializations>> {
//            override fun onResponse(
//                call: Call<List<Specializations>>,
//                response: Response<List<Specializations>>
//            ) {
//                if (!isAdded || isDetached) return
//                val apiResp = response.body()!!
//                specialistsList = apiResp
//                val adapter = ListAdapter(requireContext(), apiResp, this@SpecialistsFragment)
//
//                binding.listView.adapter = adapter
//                // Success case
//                Log.i("getSpecialists", "onResponse: $apiResp")
//            }
//
//            override fun onFailure(call: Call<List<Specializations>>, t: Throwable) {
//                Log.i("getSpecialists", "Error")
//            }
//        })
        val adapter = ListAdapter(requireContext(), specialistsList, this@SpecialistsFragment)
        binding.listView.adapter = adapter
    }
}