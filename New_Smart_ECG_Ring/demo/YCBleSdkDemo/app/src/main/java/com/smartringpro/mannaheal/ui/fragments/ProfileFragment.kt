package com.smartringpro.mannaheal.ui.fragments

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import android.widget.MultiAutoCompleteTextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.profile.AddProfileDataResponse
import com.smartringpro.mannaheal.api.profile.FamilyMember
import com.smartringpro.mannaheal.api.profile.ProfileDataRepository
import com.smartringpro.mannaheal.api.profile.ProfileDataResponse
import com.smartringpro.mannaheal.api.symptoms.Disease
import com.smartringpro.mannaheal.api.symptoms.SymptomsRepository
import com.smartringpro.mannaheal.databinding.FragmentProfileBinding
import com.smartringpro.mannaheal.ui.activities.HomeActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class ProfileFragment : Fragment() {

    private var userId: Int = -1
    private var userResId: Int = -1
    private var isFromFamilyMembers: Boolean = false
    private var isNewFamilyMember: Boolean = false
    private var familyMemberId: Int = -1

    private lateinit var imageUri: Uri
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>


    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val genderOptions = arrayOf("Select Gender", "Male", "Female")
    private val bloodGroups =
        arrayOf("Select Blood Group", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
    private val relationship =
        arrayOf("Select Relationship", "Father", "Mother", "Sister", "Brother")

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root
        isFromFamilyMembers = arguments?.getBoolean("isFromFamilyMembers", false) ?: false
        val selectedFamilyMember: FamilyMember? = arguments?.getParcelable("selectedFamilyMember")

        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val selectedImageUri = result.data?.data
                    selectedImageUri?.let {
                        val filePath = getFilePathFromUri(it)
                        imageUri = it
                        Glide.with(this)
                            .load(filePath)
                            .circleCrop()
                            .into(binding.ivProfileImage)
                    }
                }
            }
        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("id", -1)
        val editor = sharedPreferences.edit()
        editor.putBoolean("checkProfile", false)
        editor.apply()

        // Remove manual assignments, use binding directly everywhere
        initDropDownSpinners()

        if (userId != -1 && !isFromFamilyMembers) {
            fetchUserProfileData(userId)
        }

        binding.ivCalendar.setOnClickListener {
            // Get the current date
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // Open DatePickerDialog
            val datePickerDialog = DatePickerDialog(
                requireContext(), // or context for Fragment
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Format the selected date and set it in the EditText
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val formattedDate = dateFormat.format(selectedCalendar.time)

                    binding.etDateOfBirth.setText(formattedDate)
                },
                year, month, day // Set default date as current date
            )
            datePickerDialog.show()
        }

        binding.btnUpload.setOnClickListener {
            openGallery()
        }

        binding.btnSaveProfileData.setOnClickListener {
            if (isFromFamilyMembers) {
                if (isNewFamilyMember) {
                    saveNewFamilyMemberProfileData()
                } else {
                    updateFamilyMemberProfileData()
                }
            } else {
                saveUserProfileData()
            }
        }

        updateVisibilityBasedOnFamilyFlag(isFromFamilyMembers)

        if (isFromFamilyMembers) {
            if (selectedFamilyMember != null) {
                binding.etName.setText(selectedFamilyMember.name ?: "")
                binding.etDateOfBirth.setText(selectedFamilyMember.dob ?: "")
                binding.etEmergencyContactNumber.setText(selectedFamilyMember.emergency_phone ?: "")
                binding.etExistingDiseases.setText(selectedFamilyMember.existing_diseases ?: "")
                binding.etExistingMedications.setText(selectedFamilyMember.existing_medications ?: "")
                binding.etDateOfBirth.setText(selectedFamilyMember.dob ?: "")
                setSpinnerValue(binding.spinnerGender, selectedFamilyMember.gender, genderOptions)
                setSpinnerValue(binding.spinnerBloodGroups, selectedFamilyMember.bloodGroup, bloodGroups)
                setSpinnerValue(binding.spinnerRelation, selectedFamilyMember.relation, relationship)

                if (!selectedFamilyMember.dependentImageUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(selectedFamilyMember.dependentImageUrl)
                        .circleCrop()
                        .into(binding.ivProfileImage)
                } else {
                    binding.ivProfileImage.setImageResource(R.drawable.baseline_account_circle_24)
                }
                familyMemberId = selectedFamilyMember.id
                isNewFamilyMember = false
                Log.d("ProfileFragment", selectedFamilyMember.id.toString())
            } else {
                isNewFamilyMember = true
                Log.d("ProfileFragment", "New member")
            }
            Log.d("ProfileFragment", "Opened from FamilyMembersFragment")
        } else {
            Log.d("ProfileFragment", "Opened from HomeActivity (Self Profile)")
        }

        setupDiseasesMultiSelect()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initDropDownSpinners() {

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            genderOptions
        )
        binding.spinnerGender.adapter = adapter

        val bloodGroupAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            bloodGroups
        )
        binding.spinnerBloodGroups.adapter = bloodGroupAdapter

        val relationAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            relationship
        )
        binding.spinnerRelation.adapter = relationAdapter

        binding.spinnerGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position > 0) { // Ignore default "Select Gender" option
                    val selectedGender = when (parent.getItemAtPosition(position).toString()) {
                        "Male" -> "M"
                        "Female" -> "F"
                        else -> ""
                    }
                    Log.d("ProfileFragment", "Gender: $selectedGender")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerBloodGroups.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position > 0) { // Ignore default "Select Blood Group" option
                    val selectedBloodGroup = parent.getItemAtPosition(position).toString()
                    Log.d("ProfileFragment", "Blood Group: $selectedBloodGroup")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerRelation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position > 0) { // Ignore default "Select Blood Group" option
                    val selectedRelation = parent.getItemAtPosition(position).toString()
                    Log.d("ProfileFragment", "Relation: $selectedRelation")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun updateVisibilityBasedOnFamilyFlag(isFromFamilyMembers: Boolean) {
        if (isFromFamilyMembers) {
            binding.etFirstNameGroup.visibility = View.GONE
            binding.etLastNameGroup.visibility = View.GONE
            binding.etEmailIdGroup.visibility = View.GONE
            binding.etPhoneNumberGroup.visibility = View.GONE
            binding.etEmergencyContactNumberGroup.visibility = View.GONE
            binding.etHeightGroup.visibility = View.GONE
            binding.etWeightGroup.visibility = View.GONE
            binding.etAddressGroup.visibility = View.GONE
            binding.etCountryGroup.visibility = View.GONE
            binding.etStateGroup.visibility = View.GONE
            binding.etCityGroup.visibility = View.GONE
            binding.etZipCodeGroup.visibility = View.GONE
            binding.etNameGroup.visibility = View.VISIBLE
            binding.relationshipSelectorGroup.visibility = View.VISIBLE
        } else {
            binding.etFirstNameGroup.visibility = View.VISIBLE
            binding.etLastNameGroup.visibility = View.VISIBLE
            binding.etEmailIdGroup.visibility = View.VISIBLE
            binding.etPhoneNumberGroup.visibility = View.VISIBLE
            binding.etEmergencyContactNumberGroup.visibility = View.VISIBLE
            binding.etHeightGroup.visibility = View.VISIBLE
            binding.etWeightGroup.visibility = View.VISIBLE
            binding.etAddressGroup.visibility = View.VISIBLE
            binding.etCountryGroup.visibility = View.VISIBLE
            binding.etStateGroup.visibility = View.VISIBLE
            binding.etCityGroup.visibility = View.VISIBLE
            binding.etZipCodeGroup.visibility = View.VISIBLE
            binding.etNameGroup.visibility = View.GONE
            binding.relationshipSelectorGroup.visibility = View.GONE
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun getFilePathFromUri(uri: Uri): String {
        var filePath: String? = null
        val cursor = context?.contentResolver?.query(uri, null, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            if (columnIndex != -1) {
                filePath = cursor.getString(columnIndex)
            }
            cursor.close()
        }
        return filePath ?: ""
    }

    private fun fetchUserProfileData(userId: Int) {
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

                        userResId = profileData.user_id

                        binding.etFirstName.setText(profileData.first_name ?: "")
                        binding.etLastName.setText(profileData.last_name ?: "")
                        binding.etEmailId.setText(profileData.email ?: "")
                        binding.etPhoneNumber.setText(profileData.phone_number)
                        binding.etEmergencyContactNumber.setText(profileData.emergency_phone ?: "")
                        binding.etDateOfBirth.setText(profileData.dob ?: "")
                        setSpinnerValue(binding.spinnerGender, profileData.gender, genderOptions)
                        setSpinnerValue(binding.spinnerBloodGroups, profileData.blood_group, bloodGroups)
                        binding.etHeight.setText(profileData.height ?: "")
                        binding.etWeight.setText(profileData.weight ?: "")
                        binding.etAddress.setText(profileData.address ?: "")
                        binding.etCountry.setText(profileData.country ?: "")
                        binding.etState.setText(profileData.state ?: "")
                        binding.etCity.setText(profileData.city ?: "")
                        binding.etZipCode.setText(profileData.pincode ?: "")
                        binding.etExistingDiseases.setText(profileData.existing_diseases ?: "")
                        binding.etExistingMedications.setText(profileData.existing_medications ?: "")

                        // Handle profile image if available
                        if (!profileData.patient_image_url.isNullOrEmpty()) {
                            Glide.with(requireContext())
                                .load(profileData.patient_image_url)
                                .circleCrop()
                                .into(binding.ivProfileImage)
                        } else {
                            binding.ivProfileImage.setImageResource(R.drawable.baseline_account_circle_24)
                        }

                        Log.d("ProfileFragment", "User Data: $profileData")
                    } else {
                        Toast.makeText(requireContext(), "No user data found", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Log.e("ProfileFragment", "API Error: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch profile data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ProfileDataResponse>, t: Throwable) {
                Log.e("ProfileFragment", "API Failure: ${t.message}")
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun setSpinnerValue(spinner: Spinner, value: String?, options: Array<String>) {
        if (value != null) {
            val mappedValue = when (value) {
                "M" -> "Male"
                "F" -> "Female"
                else -> value // If no match, return the value as is
            }

            val position = options.indexOf(mappedValue)
            if (position > 0) {
                spinner.setSelection(position)
            }
        }
    }

    private fun validateInputFields(): Boolean {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val name = binding.etName.text.toString().trim()
        val relation =
            if (binding.spinnerRelation.selectedItemPosition > 0) binding.spinnerRelation.selectedItem.toString() else ""
        val email = binding.etEmailId.text.toString().trim()
        val gender =
            if (binding.spinnerGender.selectedItemPosition > 0) binding.spinnerGender.selectedItem.toString() else ""
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        val dob = binding.etDateOfBirth.text.toString().trim()
        val bloodGroup =
            if (binding.spinnerBloodGroups.selectedItemPosition > 0) binding.spinnerBloodGroups.selectedItem.toString() else ""
        val address = binding.etAddress.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val state = binding.etState.text.toString().trim()
        val country = binding.etCountry.text.toString().trim()
        val pincode = binding.etZipCode.text.toString().trim()
        val height = binding.etHeight.text.toString().trim()
        val weight = binding.etWeight.text.toString().trim()

        when {
            !isFromFamilyMembers && firstName.isEmpty() -> {
                binding.etFirstName.error = "First name is required"
                binding.etFirstName.requestFocus()
                return false
            }

            !isFromFamilyMembers && lastName.isEmpty() -> {
                binding.etLastName.error = "Last name is required"
                binding.etLastName.requestFocus()
                return false
            }

            isFromFamilyMembers && name.isEmpty() -> {
                binding.etName.error = "Name is required"
                binding.etName.requestFocus()
                return false
            }

            isFromFamilyMembers && relation.isEmpty() -> {
                Toast.makeText(requireContext(), "Please select relation", Toast.LENGTH_SHORT)
                    .show()
                return false
            }

            !isFromFamilyMembers && (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(
                email
            ).matches()) -> {
                binding.etEmailId.error = "Enter a valid email"
                binding.etEmailId.requestFocus()
                return false
            }

            gender.isEmpty() -> {
                Toast.makeText(requireContext(), "Please select gender", Toast.LENGTH_SHORT).show()
                return false
            }

            !isFromFamilyMembers && (phoneNumber.isEmpty() || phoneNumber.length != 10) -> {
                binding.etPhoneNumber.error = "Enter a valid phone number"
                binding.etPhoneNumber.requestFocus()
                return false
            }

            dob.isEmpty() -> {
                binding.etDateOfBirth.error = "Date of Birth is required"
                binding.etDateOfBirth.requestFocus()
                return false
            }

            bloodGroup.isEmpty() -> {
                Toast.makeText(requireContext(), "Please select a blood group", Toast.LENGTH_SHORT)
                    .show()
                return false
            }

            !isFromFamilyMembers && address.isEmpty() -> {
                binding.etAddress.error = "Address is required"
                binding.etAddress.requestFocus()
                return false
            }

            !isFromFamilyMembers && city.isEmpty() -> {
                binding.etCity.error = "City is required"
                binding.etCity.requestFocus()
                return false
            }

            !isFromFamilyMembers && state.isEmpty() -> {
                binding.etState.error = "State is required"
                binding.etState.requestFocus()
                return false
            }

            !isFromFamilyMembers && country.isEmpty() -> {
                binding.etCountry.error = "Country is required"
                binding.etCountry.requestFocus()
                return false
            }

            !isFromFamilyMembers && (pincode.isEmpty() || pincode.length < 5) -> {
                binding.etZipCode.error = "Enter a valid pincode"
                binding.etZipCode.requestFocus()
                return false
            }

            !isFromFamilyMembers && (height.isEmpty() || height.toDoubleOrNull() == null) -> {
                binding.etHeight.error = "Enter a valid height"
                binding.etHeight.requestFocus()
                return false
            }

            !isFromFamilyMembers && (weight.isEmpty() || weight.toDoubleOrNull() == null) -> {
                binding.etWeight.error = "Enter a valid weight"
                binding.etWeight.requestFocus()
                return false
            }

            else -> return true
        }
    }

    private fun saveUserProfileData() {

        if (!validateInputFields()) {
            return
        }

        val gender = if (binding.spinnerGender.selectedItemPosition > 0) {
            when (binding.spinnerGender.selectedItem.toString()) {
                "Male" -> "M"
                "Female" -> "F"
                else -> ""
            }
        } else {
            ""
        }

        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("user_gender", gender) // Save gender as a string
        editor.apply()

        val user_Id = userResId
        val id = userId
        val firstName = binding.etFirstName.text.toString()
        val lastName = binding.etLastName.text.toString()
        val email = binding.etEmailId.text.toString()
        val phoneNumber = binding.etPhoneNumber.text.toString()
        val emergencyPhone = binding.etEmergencyContactNumber.text.toString()
        val dob = binding.etDateOfBirth.text.toString()
        val bloodGroup =
            if (binding.spinnerBloodGroups.selectedItemPosition > 0) binding.spinnerBloodGroups.selectedItem.toString() else ""
        val address = binding.etAddress.text.toString()
        val city = binding.etCity.text.toString()
        val state = binding.etState.text.toString()
        val country = binding.etCountry.text.toString()
        val pincode = binding.etZipCode.text.toString()
        val height = binding.etHeight.text.toString()
        val weight = binding.etWeight.text.toString()
        val allergy = 0 // Example allergy status
        val status = 1 // Example status
        val existingDiseases = binding.etExistingDiseases.text.toString().trim()
        val existingMedications = binding.etExistingMedications.text.toString().trim()

        val imageFile = if (::imageUri.isInitialized) {
            val realPath = getRealPathFromURI(imageUri)
            if (realPath != null) {
                File(realPath) // Create a File object from the real path
            } else {
                null
            }
        } else {
            null
        }

        val repository = ProfileDataRepository()
        val call = repository.saveUserProfileData(
            user_Id, id, firstName, lastName, email, gender, phoneNumber, emergencyPhone, dob, bloodGroup,
            address, city, state, country, pincode, height, weight, allergy, status,
            existingDiseases,
            existingMedications,
            imageFile
        )

        call.enqueue(object : Callback<AddProfileDataResponse> {
            override fun onResponse(
                call: Call<AddProfileDataResponse>,
                response: Response<AddProfileDataResponse>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()?.response == 0) {
                    val addProfileResponseData = response.body()
                    fetchUserProfileData(userId)
                    Toast.makeText(
                        requireContext(),
                        addProfileResponseData?.message.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("ProfileFragment", "Add User Data: $addProfileResponseData")
                } else {
                    Log.e("ProfileFragment", "Error: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to save profile", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<AddProfileDataResponse>, t: Throwable) {
                Log.e("ProfileFragment", "Failure: ${t.message}")
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun saveNewFamilyMemberProfileData() {

        if (!validateInputFields()) {
            return
        }

        val gender = if (binding.spinnerGender.selectedItemPosition > 0) {
            when (binding.spinnerGender.selectedItem.toString()) {
                "Male" -> "M"
                "Female" -> "F"
                else -> ""
            }
        } else {
            ""
        }

        val userId = userId
        val name = binding.etName.text.toString()
        val relation =
            if (binding.spinnerRelation.selectedItemPosition > 0) binding.spinnerRelation.selectedItem.toString() else ""
        val dob = binding.etDateOfBirth.text.toString()
        val bloodGroup =
            if (binding.spinnerBloodGroups.selectedItemPosition > 0) binding.spinnerBloodGroups.selectedItem.toString() else ""
        val address = binding.etAddress.text.toString()
        val city = binding.etCity.text.toString()
        val state = binding.etState.text.toString()
        val country = binding.etCountry.text.toString()
        val pincode = binding.etZipCode.text.toString()
        val height = binding.etHeight.text.toString()
        val weight = binding.etWeight.text.toString()

        val imageFile = if (::imageUri.isInitialized) {
            val realPath = getRealPathFromURI(imageUri)
            if (realPath != null) {
                File(realPath) // Create a File object from the real path
            } else {
                null
            }
        } else {
            null
        }

        val repository = ProfileDataRepository()
        val call = repository.saveFamilyMembersData(
            userId, name, relation, gender, dob, bloodGroup,
            address, city, state, country, pincode, height, weight,
            binding.etEmergencyContactNumber.text.toString().trim(),
            binding.etExistingDiseases.text.toString().trim(),
            binding.etExistingMedications.text.toString().trim(),
            imageFile
        )

        call.enqueue(object : Callback<AddProfileDataResponse> {
            override fun onResponse(
                call: Call<AddProfileDataResponse>,
                response: Response<AddProfileDataResponse>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()?.response == 0) {
                    val addProfileResponseData = response.body()
                    Toast.makeText(
                        requireContext(),
                        addProfileResponseData?.message.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("ProfileFragment", "Add Family Members Data: $addProfileResponseData")
                    (activity as? HomeActivity)?.openFragment(
                        FamilyMembersFragment(),
                        "Family members",
                        false
                    )
                } else {
                    (activity as? HomeActivity)?.openFragment(
                        FamilyMembersFragment(),
                        "Family members",
                        false
                    )
                    Log.e("ProfileFragment", "Error: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to save profile", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<AddProfileDataResponse>, t: Throwable) {
                (activity as? HomeActivity)?.openFragment(
                    FamilyMembersFragment(),
                    "Family members",
                    false
                )
                Log.e("ProfileFragment", "Failure: ${t.message}")
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun updateFamilyMemberProfileData() {

        if (!validateInputFields()) {
            return
        }

        val gender = if (binding.spinnerGender.selectedItemPosition > 0) {
            when (binding.spinnerGender.selectedItem.toString()) {
                "Male" -> "M"
                "Female" -> "F"
                else -> ""
            }
        } else {
            ""
        }

        val userId = userId
        val dependentId = familyMemberId
        val name = binding.etName.text.toString()
        val relation =
            if (binding.spinnerRelation.selectedItemPosition > 0) binding.spinnerRelation.selectedItem.toString() else ""
        val dob = binding.etDateOfBirth.text.toString()
        val bloodGroup =
            if (binding.spinnerBloodGroups.selectedItemPosition > 0) binding.spinnerBloodGroups.selectedItem.toString() else ""
        val address = binding.etAddress.text.toString()
        val city = binding.etCity.text.toString()
        val state = binding.etState.text.toString()
        val country = binding.etCountry.text.toString()
        val pincode = binding.etZipCode.text.toString()
        val height = binding.etHeight.text.toString()
        val weight = binding.etWeight.text.toString()
        val emergencyPhone = binding.etEmergencyContactNumber.text.toString().trim()
        val existingDiseases = binding.etExistingDiseases.text.toString().trim()
        val existingMedications = binding.etExistingMedications.text.toString().trim()

        val imageFile = if (::imageUri.isInitialized) {
            val realPath = getRealPathFromURI(imageUri)
            if (realPath != null) {
                File(realPath) // Create a File object from the real path
            } else {
                null
            }
        } else {
            null
        }

        val repository = ProfileDataRepository()
        val call = repository.updateFamilyMembersData(
            userId, dependentId, name, relation, gender, dob, bloodGroup,
            address, city, state, country, pincode, height, weight, emergencyPhone,
            existingDiseases, existingMedications, imageFile
        )

        call.enqueue(object : Callback<AddProfileDataResponse> {
            override fun onResponse(
                call: Call<AddProfileDataResponse>,
                response: Response<AddProfileDataResponse>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()?.response == 0) {
                    val updateProfileResponseData = response.body()
                    Toast.makeText(
                        requireContext(),
                        updateProfileResponseData?.message.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("ProfileFragment", "Update Family Members Data: $updateProfileResponseData")
                    (activity as? HomeActivity)?.openFragment(
                        FamilyMembersFragment(),
                        "Family members",
                        false
                    )
                } else {
                    (activity as? HomeActivity)?.openFragment(
                        FamilyMembersFragment(),
                        "Family members",
                        false
                    )
                    Log.e("ProfileFragment", "Error: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to save profile", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<AddProfileDataResponse>, t: Throwable) {
                (activity as? HomeActivity)?.openFragment(
                    FamilyMembersFragment(),
                    "Family members",
                    false
                )
                Log.e("ProfileFragment", "Failure: ${t.message}")
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            cursor = requireContext().contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return cursor.getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun setupDiseasesMultiSelect() {
        val repository = SymptomsRepository()
        val call = repository.getDiseaseList()
        call.enqueue(object : Callback<List<Disease>> {
            override fun onResponse(
                call: Call<List<Disease>>,
                response: Response<List<Disease>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val diseaseList = response.body()!!.mapNotNull { it.disease_name }
                    activity?.runOnUiThread {
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, diseaseList)
                        binding.etExistingDiseases.setAdapter(adapter)
                        binding.etExistingDiseases.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
                    }
                }
            }
            override fun onFailure(call: Call<List<Disease>>, t: Throwable) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to load diseases", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

}
