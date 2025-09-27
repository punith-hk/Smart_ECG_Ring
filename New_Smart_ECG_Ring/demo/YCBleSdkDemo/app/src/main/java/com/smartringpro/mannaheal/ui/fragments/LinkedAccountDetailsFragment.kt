package com.smartringpro.mannaheal.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.linkedAccountData.LastRingDataResponse
import com.smartringpro.mannaheal.api.linkedAccountData.LinkedAccountRepository
import com.smartringpro.mannaheal.api.profile.ProfileDataRepository
import com.smartringpro.mannaheal.api.profile.ProfileDataResponse
import com.smartringpro.mannaheal.databinding.FragmentLinkedAccountDetailsBinding
import com.smartringpro.mannaheal.ui.activities.HomeActivity
import org.threeten.bp.LocalDate
import org.threeten.bp.Period
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LinkedAccountDetailsFragment : Fragment() {

    private var _binding: FragmentLinkedAccountDetailsBinding? = null
    private val binding get() = _binding!!

    private var linkedId: Int? = null
    private var linkedName: String? = null

    private val repository = LinkedAccountRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        linkedId = arguments?.getInt("linked_id")
        linkedName = arguments?.getString("linked_name")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLinkedAccountDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Disassociation logic
        binding.btnDisassociation.setOnClickListener {
            parentFragmentManager.popBackStackImmediate(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            (activity as? HomeActivity)?.openFragment(CareFragment(), "Linked Account", false)

        }

        fetchUserProfileData()
        getLinkedAccountHealthData()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun getLinkedAccountHealthData() {
        val userId = linkedId
        if (userId == null || userId == -1) {
            Toast.makeText(context, "User ID not found. Please log in again.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        repository.getLastRingData(userId).enqueue(object : Callback<LastRingDataResponse> {
            override fun onResponse(
                call: Call<LastRingDataResponse>,
                response: Response<LastRingDataResponse>
            ) {
                if (response.isSuccessful && response.body()?.data?.isNotEmpty() == true) {
                    val data = response.body()?.data ?: return

                    for (item in data) {
                        when (item.type) {
                            "heart_rate" -> binding.linkedAccountHeartRateValue.text =
                                "${item.value} times/min"

                            "temperature" -> binding.linkedAccountTemperatureValue.text =
                                "${item.value} °C"

                            "stress" -> binding.linkedAccountStressValue.text =
                                "${item.value} units"

                            "blood_oxygen" -> binding.linkedAccountOxygenValue.text =
                                "${item.value}%"

                            "blood_pressure" -> binding.linkedAccountBpValue.text =
                                "${item.value} mmHg"

                            "hrv" -> binding.linkedAccountHrvValue.text = "${item.value} ms"
                            "calories" -> binding.linkedAccountCalories.text = "${item.value} kcal"
                            // Add more if needed
                        }
                    }

                    val location = response.body()?.location
                    val webView = binding.mapWebView
                    val placeholder = binding.mapPlaceholderText

                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude

                        if (!latitude.isNullOrEmpty() && !longitude.isNullOrEmpty()) {
                            val html = """
            <html>
                <body style="margin:0;padding:0;">
                    <iframe width="100%" height="100%" frameborder="0" style="border:0"
                        src="https://www.google.com/maps?q=$latitude,$longitude&output=embed" allowfullscreen>
                    </iframe>
                </body>
            </html>
        """.trimIndent()

                            webView.settings.javaScriptEnabled = true
                            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)

                            webView.visibility = View.VISIBLE
                            placeholder.visibility = View.GONE
                        } else {
                            // Invalid lat/lng
                            webView.visibility = View.GONE
                            placeholder.visibility = View.VISIBLE
                        }
                    } else {
                        webView.visibility = View.GONE
                        placeholder.visibility = View.VISIBLE
                    }

                } else {
                    val errorMsg = response.errorBody()?.string()
                    Log.e("Health", "API Error: $errorMsg")
                }
            }

            override fun onFailure(call: Call<LastRingDataResponse>, t: Throwable) {
                Log.e("Health", "API call failed: ${t.message}", t)
            }
        })
    }

    private fun calculateAgeFromDob(dob: String?): Int? {
        return try {
            val formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd") // or adjust based on actual format
            val birthDate = LocalDate.parse(dob ?: return null, formatter)
            Period.between(birthDate, LocalDate.now()).years
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchUserProfileData() {

        val userId = linkedId

        if (userId == null || userId == -1) {
            Toast.makeText(context, "User ID not found. Please log in again.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val repository = ProfileDataRepository()
        val call = repository.getUserProfileData(userId)

        call.enqueue(object : Callback<ProfileDataResponse> {
            override fun onResponse(
                call: Call<ProfileDataResponse>,
                response: Response<ProfileDataResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val profileData = response.body()?.data
                    if (profileData != null) {
                        // Set name
                        val fullName =
                            "${profileData.first_name ?: ""} ${profileData.last_name ?: ""}".trim()
                        binding.linkedAccountName.text = fullName

                        // Set age & gender
                        val age = calculateAgeFromDob(profileData.dob)
                        val genderRaw = profileData.gender ?: ""
                        val gender = when (genderRaw.uppercase()) {
                            "M" -> "Male"
                            "F" -> "Female"
                            else -> genderRaw
                        }
                        binding.linkedAccountAgeGender.text =
                            if (age != null) "$age years old - $gender" else gender

                        // Set height & weight
                        val height =
                            profileData.height?.toDoubleOrNull()?.toInt()?.toString() ?: "-"
                        val weight =
                            profileData.weight?.toDoubleOrNull()?.toInt()?.toString() ?: "-"
                        binding.linkedAccountHeightWeight.text =
                            "Height: ${height}cm  Weight: ${weight}kg"

                        // Set image
                        if (!profileData.patient_image_url.isNullOrEmpty()) {
                            Glide.with(requireContext())
                                .load(profileData.patient_image_url)
                                .circleCrop()
                                .into(binding.linkedAccountProfileImage)
                        } else {
                            binding.linkedAccountProfileImage.setImageResource(R.drawable.baseline_account_circle_24)
                        }
                    } else {
                        Toast.makeText(requireContext(), "No user data found", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Log.e("LinkedAccount", "API Error: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch profile data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ProfileDataResponse>, t: Throwable) {
                Log.e("LinkedAccount", "API Failure: ${t.message}")
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

}