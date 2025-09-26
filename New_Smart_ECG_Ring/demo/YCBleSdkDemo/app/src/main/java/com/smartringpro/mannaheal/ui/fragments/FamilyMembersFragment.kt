package com.smartringpro.mannaheal.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.adapter.FamilyMembersAdapter
import com.smartringpro.mannaheal.api.profile.AddProfileDataResponse
import com.smartringpro.mannaheal.api.profile.DependentsResponse
import com.smartringpro.mannaheal.api.profile.ProfileDataRepository
import com.smartringpro.mannaheal.ui.activities.HomeActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FamilyMembersFragment : Fragment() {

    private var userId: Int = -1

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FamilyMembersAdapter
    private lateinit var btnAddDependent: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_family_members, container, false)

        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("id", -1)

        btnAddDependent = view.findViewById(R.id.btnAddDependent)

        recyclerView = view.findViewById(R.id.familyMembersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        btnAddDependent.setOnClickListener {
            val fragment = ProfileFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("isFromFamilyMembers", true)
                }
            }
            (activity as? HomeActivity)?.openFragment(fragment, "Profile", true)
        }

        if (userId != -1) {
            fetchDependents()
        } else {
            Toast.makeText(requireContext(), "Invalid User ID", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun fetchDependents() {

        val repository = ProfileDataRepository()
        val call = repository.getDependentsData(userId)

        call.enqueue(object : Callback<DependentsResponse> {
            override fun onResponse(
                call: Call<DependentsResponse>,
                response: Response<DependentsResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val familyMembersDataData = response.body()?.data

                    if (familyMembersDataData != null) {
                        adapter = FamilyMembersAdapter(
                            familyMembersDataData,
                            onEditClick = { familyMember ->
                                Log.d("FamilyMembersFragment", "Selected Edit Data: $familyMember")
                                val fragment = ProfileFragment().apply {
                                    arguments = Bundle().apply {
                                        putBoolean("isFromFamilyMembers", true)
                                        putParcelable("selectedFamilyMember", familyMember) // Ensure FamilyMember implements Parcelable
                                    }
                                }

                                (activity as? HomeActivity)?.openFragment(fragment, "Family member", true)
                            },
                            onDeleteClick = { familyMember ->
                                Log.d("FamilyMembersFragment", "Selected Delete Data: $familyMember")
                                deleteDependentById(familyMember.id)
                            }
                        )
                        recyclerView.adapter = adapter

                        Log.d("FamilyMembersFragment", "User Data: $familyMembersDataData")

                    } else {
                        Toast.makeText(requireContext(), "No data found", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Log.e("FamilyMembersFragment", "API Error: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<DependentsResponse>, t: Throwable) {
                Log.e("FamilyMembersFragment", "API Failure: ${t.message}")
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun deleteDependentById(dependentId: Int?) {
        if (dependentId == null || dependentId == 0) {
            Toast.makeText(
                requireContext(),
                "Failed to delete data",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val repository = ProfileDataRepository()
        val call = repository.deleteDependentById(userId, dependentId)

        call.enqueue(object : Callback<AddProfileDataResponse> {
            override fun onResponse(
                call: Call<AddProfileDataResponse>,
                response: Response<AddProfileDataResponse>
            ) {
                if (response.isSuccessful && response.body()?.response == 0) {
                    val deleteProfileResponseData = response.body()
                    fetchDependents()
                    Toast.makeText(
                        requireContext(),
                        deleteProfileResponseData?.message.orEmpty(),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("ProfileFragment", "Delete Family Members Data: $deleteProfileResponseData")
                } else {
                    Log.e("FamilyMembersFragment", "API Error: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        requireContext(),
                        "Failed to delete data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<AddProfileDataResponse>, t: Throwable) {
                Log.e("FamilyMembersFragment", "API Failure: ${t.message}")
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

}