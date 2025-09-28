package com.smartringpro.mannaheal.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
//import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.profile.AddProfileDataResponse
import com.smartringpro.mannaheal.api.profile.ProfileDataRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class RegisterProfileActivity : AppCompatActivity() {
    private lateinit var tvTitle: TextView
    private lateinit var npDay: NumberPicker
    private lateinit var npMonth: NumberPicker
    private lateinit var npYear: NumberPicker
    private lateinit var npGender: NumberPicker
    private lateinit var npFeet: NumberPicker
    private lateinit var npInches: NumberPicker
    private lateinit var npTemperatureUnit: NumberPicker
    private lateinit var tvHeight: LinearLayout
    private lateinit var btnSubmit: TextView
    private lateinit var tvSkip: TextView
    private var isDobSelected = true
    private var isGenderSelected = false
    private var isHeightSelected = false
    private var isUnitSelected = false

    private var dob: String = ""
    private var gender: String = ""
    private var height: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_register_profile)

        // Find Views
        tvTitle = findViewById(R.id.tvTitle)
        npDay = findViewById(R.id.npDay)
        npMonth = findViewById(R.id.npMonth)
        npYear = findViewById(R.id.npYear)
        npGender = findViewById(R.id.npGender)
        npFeet = findViewById(R.id.npFeet)
        npInches = findViewById(R.id.npInches)
        npTemperatureUnit = findViewById(R.id.npTemperatureUnit)

        tvHeight = findViewById(R.id.tvHeight)
        btnSubmit = findViewById(R.id.btnSubmit)
        tvSkip = findViewById(R.id.tvSkip)

        setupDateOfBirthPicker()

        tvSkip.setOnClickListener {
            val intent = Intent(this@RegisterProfileActivity, HomeActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        btnSubmit.setOnClickListener {
            if (isDobSelected) {
                // Capture DOB
                val selectedDay = npDay.value
                val months = arrayOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )
                val selectedMonth = months[npMonth.value - 1]
                val selectedYear = npYear.value

                val dob = "$selectedDay $selectedMonth $selectedYear"

                this.dob = dob

                Toast.makeText(this, "DOB: $dob", Toast.LENGTH_SHORT).show()
                // Switch to Gender Selection
                setupGenderPicker()
            } else if (isGenderSelected) {
                // Capture Gender
                val genders = arrayOf("Male", "Female")
                val selectedGender = genders[npGender.value]

                this.gender = selectedGender

                Toast.makeText(this, "Gender: $selectedGender", Toast.LENGTH_SHORT).show()

                setupHeightPicker()
            } else if (isHeightSelected) {
                val selectedFeet = npFeet.value
                val selectedInches = npInches.value

                val height = "$selectedFeet.$selectedInches"

                // Save height to a variable
                this.height = height

                Toast.makeText(this, "Height: $height", Toast.LENGTH_SHORT).show()
                setupTemperatureUnitPicker()
            } else {
                val temperatureUnits = arrayOf("Fahrenheit", "Celsius degrees")
                val selectedTemperatureUnit = temperatureUnits[npTemperatureUnit.value]

                saveTemperatureUnit(selectedTemperatureUnit)

                Toast.makeText(this, "Temperature Unit: $selectedTemperatureUnit", Toast.LENGTH_SHORT).show()

                val intent = Intent(this@RegisterProfileActivity, HomeActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()

            }
        }
    }

    private fun saveTemperatureUnit(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("temperature_unit", unit)
        editor.apply()
        saveUserProfileData()
    }

    private fun setupDateOfBirthPicker() {
        tvTitle.text = "Date of Birth"
        npDay.apply {
            minValue = 1
            maxValue = 31
            visibility = NumberPicker.VISIBLE
        }
        npMonth.apply {
            minValue = 1
            maxValue = 12
            displayedValues = arrayOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            visibility = NumberPicker.VISIBLE
        }
        npYear.apply {
            minValue = 1950
            maxValue = 2025
            value = 1990
            visibility = NumberPicker.VISIBLE
        }
        npGender.visibility = NumberPicker.GONE // Hide gender selection initially
        tvHeight.visibility = View.GONE
    }

    private fun setupGenderPicker() {
        isDobSelected = false
        isGenderSelected = true
        tvTitle.text = "Select Gender"

        // Hide DOB pickers
        npDay.visibility = NumberPicker.GONE
        npMonth.visibility = NumberPicker.GONE
        npYear.visibility = NumberPicker.GONE
        tvHeight.visibility = View.GONE

        // Show gender picker
        npGender.apply {
            minValue = 0
            maxValue = 1
            displayedValues = arrayOf("Male", "Female")
            visibility = NumberPicker.VISIBLE
        }
    }

    private fun setupHeightPicker() {
        isGenderSelected = false
        isHeightSelected = true
        tvTitle.text = "Select Height"

        // Hide DOB and Gender pickers
        npDay.visibility = NumberPicker.GONE
        npMonth.visibility = NumberPicker.GONE
        npYear.visibility = NumberPicker.GONE
        npGender.visibility = NumberPicker.GONE

        // Show height picker
        tvHeight.visibility = View.VISIBLE

        // Configure feet picker (e.g., 4 to 7 feet)
        npFeet.minValue = 3
        npFeet.maxValue = 7
        npFeet.value = 5 // Default value

        // Configure inches picker (e.g., 0 to 11 inches)
        npInches.minValue = 0
        npInches.maxValue = 11
        npInches.value = 6 // Default value
    }

    private fun setupTemperatureUnitPicker() {
        isHeightSelected = false
        tvTitle.text = "Select Temperature Unit"

        btnSubmit.text = "Finish"

        // Hide DOB, Gender, and Height pickers
        npDay.visibility = NumberPicker.GONE
        npMonth.visibility = NumberPicker.GONE
        npYear.visibility = NumberPicker.GONE
        npGender.visibility = NumberPicker.GONE
        tvHeight.visibility = View.GONE

        // Show temperature unit picker
        npTemperatureUnit.visibility = NumberPicker.VISIBLE

        // Configure temperature unit picker
        npTemperatureUnit.minValue = 0
        npTemperatureUnit.maxValue = 1
        npTemperatureUnit.displayedValues = arrayOf("Fahrenheit", "Celsius degrees")
    }

    private fun saveUserProfileData() {
        // Example values for fields not available in this activity
        val user_Id = 123 // Replace with actual user ID
        val id = 456 // Replace with actual ID
        val firstName = "" // Not available in this activity
        val lastName = "" // Not available in this activity
        val email = "" // Not available in this activity
        val phoneNumber = "" // Not available in this activity
        val emergencyPhone= "" // Not available in this activity
        val bloodGroup = "" // Not available in this activity
        val address = "" // Not available in this activity
        val city = "" // Not available in this activity
        val state = "" // Not available in this activity
        val country = "" // Not available in this activity
        val pincode = "" // Not available in this activity
        val weight = "" // Not available in this activity
        val allergy = 0 // Example allergy status
        val status = 1 // Example status
        val existingDiseases = "" // Not available in this activity
        val existingMedications = "" // Not available in this activity
        val imageFile: File? = null // No image in this activity

        // Call the repository method to save the profile data
        val repository = ProfileDataRepository()
        val call = repository.saveUserProfileData(
            userId = user_Id,
            id = id,
            firstName = firstName,
            lastName = lastName,
            email = email,
            gender = gender,
            phoneNumber = phoneNumber,
            emergencyPhone = emergencyPhone,
            dob = dob,
            bloodGroup = bloodGroup,
            address = address,
            city = city,
            state = state,
            country = country,
            pincode = pincode,
            height = height,
            weight = weight,
            allergy = allergy,
            status = status,
            existingDiseases = existingDiseases,
            existingMedications = existingMedications,
            profileImageFile = imageFile
        )

        // Enqueue the API call
        call.enqueue(object : Callback<AddProfileDataResponse> {
            override fun onResponse(
                call: Call<AddProfileDataResponse>,
                response: Response<AddProfileDataResponse>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()?.response == 0) {
                    val addProfileResponseData = response.body()
                    Toast.makeText(
                        this@RegisterProfileActivity,
                        addProfileResponseData?.message.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("RegisterProfileActivity", "Add User Data: $addProfileResponseData")
                } else {
                    Log.e("RegisterProfileActivity", "Error: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        this@RegisterProfileActivity,
                        "Failed to save profile",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<AddProfileDataResponse>, t: Throwable) {
                Log.e("RegisterProfileActivity", "Failure: ${t.message}")
                Toast.makeText(
                    this@RegisterProfileActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}
