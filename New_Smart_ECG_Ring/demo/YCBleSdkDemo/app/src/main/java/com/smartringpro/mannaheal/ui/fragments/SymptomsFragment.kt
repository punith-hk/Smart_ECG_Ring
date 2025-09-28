package com.smartringpro.mannaheal.ui.fragments

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.symptoms.SymptomsRepository
import com.smartringpro.mannaheal.api.symptoms.SymptomsResponse
import com.smartringpro.mannaheal.model.SharedViewModel
import com.smartringpro.mannaheal.ui.activities.HomeActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SymptomsFragment : Fragment() {

    private var userId: Int = -1

    private var maleSymptoms: Map<String, List<SymptomsResponse.Symptom>> = emptyMap()
    private var femaleSymptoms: Map<String, List<SymptomsResponse.Symptom>> = emptyMap()

    private lateinit var tabMale: TextView
    private lateinit var tabFemale: TextView
    private lateinit var tabContainer: LinearLayout

    private lateinit var fShapeHair: View
    private lateinit var shapeHead: View
    private lateinit var shapeNeck: View
    private lateinit var shapeLeftHand: View
    private lateinit var shapeRightHand: View
    private lateinit var mShapeUpperBody: View
    private lateinit var mShapeMiddleBody: View
    private lateinit var fShapeUpperBody: View
    private lateinit var fShapeMiddleBody: View
    private lateinit var shapeCenterBody: View
    private lateinit var shapeLeftBottom: View
    private lateinit var shapeLeftLeg: View
    private lateinit var shapeRightBottom: View
    private lateinit var shapeRightLeg: View
    private lateinit var otherSymptoms: View

    private lateinit var scrollView: ScrollView

    private lateinit var btnSaveSymptomsData: Button

    private lateinit var symptomsCard: CardView

    private var selectedSymptomsMap = mutableMapOf<String, MutableSet<String>>()
    private var selectedBodyPartsMap = mutableMapOf<String, Boolean>()
    private val bodyPartViews = mutableListOf<View>()

    private lateinit var doctorImageIcon: ImageView
    private lateinit var doctorName: TextView
    private lateinit var doctorsDegree: TextView
    private lateinit var doctorSpecialization: TextView
    private lateinit var symptomsContainer: LinearLayout
    private lateinit var textViewSymptomsTitle: TextView

    private var maxSymptomSelection = 8

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_symptoms, container, false)

        tabMale = view.findViewById(R.id.tabMale)
        tabFemale = view.findViewById(R.id.tabFemale)
        tabContainer = view.findViewById(R.id.tabContainer)

        fShapeHair = view.findViewById(R.id.fShape_hair)
        shapeHead = view.findViewById(R.id.shape_head)
        shapeNeck = view.findViewById(R.id.shape_neck)
        shapeLeftHand = view.findViewById(R.id.shape_left_hand)
        shapeRightHand = view.findViewById(R.id.shape_right_hand)
        mShapeUpperBody = view.findViewById(R.id.mShape_upper_body)
        mShapeMiddleBody = view.findViewById(R.id.mShape_middle_body)
        fShapeUpperBody = view.findViewById(R.id.fShape_upper_body)
        fShapeMiddleBody = view.findViewById(R.id.fShape_middle_body)
        shapeCenterBody = view.findViewById(R.id.shape_center_body)
        shapeLeftBottom = view.findViewById(R.id.shape_left_bottom)
        shapeLeftLeg = view.findViewById(R.id.shape_left_leg)
        shapeRightBottom = view.findViewById(R.id.shape_right_bottom)
        shapeRightLeg = view.findViewById(R.id.shape_right_leg)
        otherSymptoms = view.findViewById(R.id.other_symptoms)

        btnSaveSymptomsData = view.findViewById(R.id.btnSaveSymptomsData)

        symptomsCard = view.findViewById(R.id.symptoms_card)

        scrollView = view.findViewById(R.id.mainScroll)

        doctorImageIcon = view.findViewById(R.id.doctor_image_icon)
        doctorName = view.findViewById(R.id.doctor_name)
        doctorsDegree = view.findViewById(R.id.doctors_degree)
        doctorSpecialization = view.findViewById(R.id.doctors_specialization)
        symptomsContainer = view.findViewById(R.id.symptomsContainer)
        textViewSymptomsTitle = view.findViewById(R.id.textViewSymptomsTitle)

        bodyPartViews.addAll(
            listOf(
                shapeHead,
                shapeNeck,
                shapeLeftHand,
                shapeRightHand,
                mShapeUpperBody,
                mShapeMiddleBody,
                fShapeUpperBody,
                fShapeMiddleBody,
                shapeCenterBody,
                shapeLeftLeg,
                shapeRightLeg,
                otherSymptoms
            )
        )

        getAllSymptoms()
        setDoctorDetails()

        val tabs = listOf(tabMale, tabFemale)

        // Set initial selection to "Day"
        selectTab(tabMale, tabs)

        // Set click listeners
        tabMale.setOnClickListener { selectTab(tabMale, tabs) }
        tabFemale.setOnClickListener { selectTab(tabFemale, tabs) }

        shapeHead.setOnClickListener {
            showDiseasePopup("Head", shapeHead)
        }
        shapeNeck.setOnClickListener {
            showDiseasePopup("Throat", shapeNeck)
        }
        shapeLeftHand.setOnClickListener {
            showDiseasePopup("Left Hand", shapeLeftHand)
        }
        shapeRightHand.setOnClickListener {
            showDiseasePopup("Right Hand", shapeRightHand)
        }
        shapeLeftLeg.setOnClickListener {
            showDiseasePopup("Left Leg", shapeLeftLeg)
        }
        shapeRightLeg.setOnClickListener {
            showDiseasePopup("Right Leg", shapeRightLeg)
        }
        mShapeUpperBody.setOnClickListener {
            showDiseasePopup("Chest", mShapeUpperBody)
        }
        fShapeUpperBody.setOnClickListener {
            showDiseasePopup("Chest", fShapeUpperBody)
        }
        mShapeMiddleBody.setOnClickListener {
            showDiseasePopup("Stomach", mShapeMiddleBody)
        }
        fShapeMiddleBody.setOnClickListener {
            showDiseasePopup("Stomach", fShapeMiddleBody)
        }
        shapeCenterBody.setOnClickListener {
            showDiseasePopup("Pelvis", shapeCenterBody)
        }
        otherSymptoms.setOnClickListener {
            showDiseasePopup("Other Symptoms", otherSymptoms)
        }

        btnSaveSymptomsData.setOnClickListener {
            saveSelectedSymptomsData()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        symptomsCard.visibility = View.GONE

        // Retrieve gender from SharedPreferences
        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val userGender =
            sharedPreferences.getString("user_gender", null) // Default to null if not found
        userId = sharedPreferences.getInt("id", -1)

        val sharedSymptomsPreferences = requireContext().getSharedPreferences("SymptomsPrefs", Context.MODE_PRIVATE)
        val clearSymptomsData = sharedSymptomsPreferences.getBoolean("clearSymptomsData", false)

        // Use the gender in your fragment
        if (userGender != null) {
            // Gender is available, hide the tabs
            tabContainer.visibility = View.GONE

            // Set the body view based on the gender
            when (userGender) {
                "M" -> showMaleBody()
                "F" -> showFemaleBody()
                else -> showMaleBody()
            }
        } else {
            // Gender is not available, show the tabs and default to male
            tabContainer.visibility = View.VISIBLE
            selectTab(tabMale, listOf(tabMale, tabFemale)) // Default to male tab
        }

        if (clearSymptomsData) {
            // Clear data if the flag is set
            clearSelectedSymptomsLocally()
            clearBodyPartsLocally()
        } else {
            loadSymptomsMapLocally()
            loadBodyPartsLocally()
            applyBodyPartOverlays()

            if (selectedSymptomsMap.isNotEmpty()) {
                updateTable()
            } else {
                symptomsCard.visibility = View.GONE
            }
        }

    }

    private fun selectTab(selectedTab: TextView, allTabs: List<TextView>) {
        allTabs.forEach { it.isSelected = false }
        selectedTab.isSelected = true
        if (selectedTab == tabMale) {
            showMaleBody()
        } else {
            showFemaleBody()
        }
    }

    private fun showMaleBody() {
        mShapeUpperBody.visibility = View.VISIBLE
        mShapeMiddleBody.visibility = View.VISIBLE
        fShapeUpperBody.visibility = View.GONE
        fShapeMiddleBody.visibility = View.GONE
        fShapeHair.visibility = View.GONE
        resetAllBodyPartBackgrounds()
    }

    private fun showFemaleBody() {
        mShapeUpperBody.visibility = View.GONE
        mShapeMiddleBody.visibility = View.GONE
        fShapeUpperBody.visibility = View.VISIBLE
        fShapeMiddleBody.visibility = View.VISIBLE
        fShapeHair.visibility = View.VISIBLE
        resetAllBodyPartBackgrounds()
    }

    private fun showDiseasePopup(bodyPart: String, bodyPartView: View) {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_disease_selection, null)
        val listView = dialogView.findViewById<ListView>(R.id.listViewDiseases)
        val okButton = dialogView.findViewById<Button>(R.id.btnOk)
        val cancelButton = dialogView.findViewById<Button>(R.id.btnCancel)
        val titleText = dialogView.findViewById<TextView>(R.id.dialogTitle)
        titleText.text = bodyPart

        // Determine which data to use based on the selected gender
        val symptomsMap = if (tabMale.isSelected) maleSymptoms else femaleSymptoms
        val diseases = symptomsMap[bodyPart]?.map { it.title } ?: emptyList()

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_multiple_choice,
            diseases
        )
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        val previouslySelected = selectedSymptomsMap[bodyPart] ?: mutableSetOf()
        val selectedItems = mutableSetOf<String>().apply { addAll(previouslySelected) }

        // Pre-select previously chosen symptoms
        for (i in diseases.indices) {
            if (previouslySelected.contains(diseases[i])) {
                listView.setItemChecked(i, true)
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedDisease = diseases[position]

            val alreadySelectedCount = selectedSymptomsMap.values.sumOf { it.size }
            val previouslySelectedInMap = selectedSymptomsMap[bodyPart]?.size ?: 0
            val currentSelectedCount = selectedItems.size
            val futureTotal = alreadySelectedCount - previouslySelectedInMap + currentSelectedCount

            val isAlreadySelected = selectedItems.contains(selectedDisease)

            if (isAlreadySelected) {
                selectedItems.remove(selectedDisease)
            } else {
                if (futureTotal >= maxSymptomSelection) {
                    Toast.makeText(
                        requireContext(),
                        "You can select a maximum of $maxSymptomSelection symptoms.",
                        Toast.LENGTH_SHORT
                    ).show()
                    listView.setItemChecked(position, false)
                    return@setOnItemClickListener
                } else {
                    selectedItems.add(selectedDisease)
                }
            }

            updateListEnabledState(listView, diseases, selectedItems, bodyPart)
        }



        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        okButton.setOnClickListener {
            if (selectedItems.isNotEmpty()) {
                selectedSymptomsMap[bodyPart] = selectedItems // Update map
                selectedBodyPartsMap[bodyPart] = true
                applyRedOverlay(bodyPartView, bodyPart)
            } else {
                selectedSymptomsMap.remove(bodyPart) // Remove empty selections
                selectedBodyPartsMap[bodyPart] = false
                resetBodyPartBackground(bodyPartView)
            }
            saveBodyPartsLocally()
            saveSelectedSymptomsLocally()
            updateTable()
            alertDialog.dismiss()

            scrollView.post {
                scrollView.smoothScrollTo(0, 0)
            }
        }

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun updateListEnabledState(
        listView: ListView,
        diseases: List<String>,
        selectedItems: Set<String>,
        bodyPart: String
    ) {
        val totalAlreadySelected = selectedSymptomsMap.values.sumOf { it.size }
        val previousCountForThisPart = selectedSymptomsMap[bodyPart]?.size ?: 0
        val futureTotal = totalAlreadySelected - previousCountForThisPart + selectedItems.size

        val disableOthers = futureTotal >= maxSymptomSelection

        for (i in diseases.indices) {
            val view = listView.getChildAt(i)
            if (view != null) {
                val isChecked = selectedItems.contains(diseases[i])
                view.isEnabled = isChecked || !disableOthers
                view.alpha = if (view.isEnabled) 1.0f else 0.5f // Optional: visual disabled look
            }
        }
    }


    private fun updateTable() {
        symptomsContainer.removeAllViews() // Clear previous views

        val totalSelected = selectedSymptomsMap.values.sumOf { it.size }

        textViewSymptomsTitle.text = if (totalSelected > 0) {
            "Symptoms ($totalSelected/$maxSymptomSelection)"
        } else {
            "Symptoms"
        }

        for ((bodyPart, symptoms) in selectedSymptomsMap) {
            if (symptoms.isNotEmpty()) {
                val fullText = "$bodyPart: ${symptoms.joinToString(", ")}"
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

        symptomsCard.visibility = if (selectedSymptomsMap.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun saveSelectedSymptomsLocally() {
        val sharedPreferences = requireContext().getSharedPreferences("SymptomsPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Serialize selectedSymptomsMap to JSON string
        val gson = Gson()
        val json = gson.toJson(selectedSymptomsMap)

        // Save it to SharedPreferences
        editor.putString("selectedSymptomsMap", json)
        editor.apply()
    }

    private fun saveBodyPartsLocally() {
        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        selectedBodyPartsMap.forEach { (bodyPart, isSelected) ->
            editor.putBoolean(bodyPart, isSelected)
        }

        editor.apply()
    }

    private fun loadSymptomsMapLocally() {
        val sharedPreferences = requireContext().getSharedPreferences("SymptomsPrefs", Context.MODE_PRIVATE)

        // Retrieve the saved JSON string
        val json = sharedPreferences.getString("selectedSymptomsMap", null)

        if (json != null) {
            // Deserialize the JSON string to a map
            val gson = Gson()
            val type = object : TypeToken<Map<String, Set<String>>>() {}.type
            selectedSymptomsMap = gson.fromJson(json, type)

            Log.d("symptoms fragment", "Symptoms map loaded: $selectedSymptomsMap")
        } else {
            Log.d("symptoms fragment", "No symptoms map found in SharedPreferences")
        }
    }

    private fun loadBodyPartsLocally() {
        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        selectedBodyPartsMap = mutableMapOf(
            "Head" to sharedPreferences.getBoolean("Head", false),
            "Throat" to sharedPreferences.getBoolean("Throat", false),
            "Left Hand" to sharedPreferences.getBoolean("Left Hand", false),
            "Right Hand" to sharedPreferences.getBoolean("Right Hand", false),
            "Left Leg" to sharedPreferences.getBoolean("Left Leg", false),
            "Right Leg" to sharedPreferences.getBoolean("Right Leg", false),
            "Chest" to sharedPreferences.getBoolean("Chest", false),
            "Stomach" to sharedPreferences.getBoolean("Stomach", false),
            "Pelvis" to sharedPreferences.getBoolean("Pelvis", false),
            "Other Symptoms" to sharedPreferences.getBoolean("Other Symptoms", false)
        )
    }

    private fun clearSelectedSymptomsLocally() {
        val sharedPreferences = requireContext().getSharedPreferences("SymptomsPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("selectedSymptomsMap")  // Remove saved symptoms map
        editor.apply()

        selectedSymptomsMap.clear()  // Make sure you also clear the map here if you are using one
    }

    private fun clearBodyPartsLocally() {
        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Clear each body part from SharedPreferences
        editor.remove("Head")
        editor.remove("Throat")
        editor.remove("Left Hand")
        editor.remove("Right Hand")
        editor.remove("Left Leg")
        editor.remove("Right Leg")
        editor.remove("Chest")
        editor.remove("Stomach")
        editor.remove("Pelvis")
        editor.remove("Other Symptoms")

        editor.apply()

    }

    private fun applyBodyPartOverlays() {
        // For each body part, check if it's selected and apply the red overlay
        if (selectedBodyPartsMap["Head"] == true) {
            applyRedOverlay(shapeHead, "Head")
        } else {
            resetBodyPartBackground(shapeHead)
        }

        if (selectedBodyPartsMap["Throat"] == true) {
            applyRedOverlay(shapeNeck, "Throat")
        } else {
            resetBodyPartBackground(shapeNeck)
        }

        if (selectedBodyPartsMap["Left Hand"] == true) {
            applyRedOverlay(shapeLeftHand, "Left Hand")
        } else {
            resetBodyPartBackground(shapeLeftHand)
        }

        if (selectedBodyPartsMap["Right Hand"] == true) {
            applyRedOverlay(shapeRightHand, "Right Hand")
        } else {
            resetBodyPartBackground(shapeRightHand)
        }

        if (selectedBodyPartsMap["Left Leg"] == true) {
            applyRedOverlay(shapeLeftLeg, "Left Leg")
        } else {
            resetBodyPartBackground(shapeLeftLeg)
        }

        if (selectedBodyPartsMap["Right Leg"] == true) {
            applyRedOverlay(shapeRightLeg, "Right Leg")
        } else {
            resetBodyPartBackground(shapeRightLeg)
        }

        if (selectedBodyPartsMap["Chest"] == true) {
            applyRedOverlay(mShapeUpperBody, "Chest")
            applyRedOverlay(fShapeUpperBody, "Chest")
        } else {
            resetBodyPartBackground(mShapeUpperBody)
            resetBodyPartBackground(fShapeUpperBody)
        }

        if (selectedBodyPartsMap["Stomach"] == true) {
            applyRedOverlay(mShapeMiddleBody, "Stomach")
            applyRedOverlay(fShapeMiddleBody, "Stomach")
        } else {
            resetBodyPartBackground(mShapeMiddleBody)
            resetBodyPartBackground(fShapeMiddleBody)
        }

        if (selectedBodyPartsMap["Pelvis"] == true) {
            applyRedOverlay(shapeCenterBody, "Pelvis")
        } else {
            resetBodyPartBackground(shapeCenterBody)
        }

        if (selectedBodyPartsMap["Other Symptoms"] == true) {
            applyRedOverlay(otherSymptoms, "Other Symptoms")
        } else {
            resetBodyPartBackground(otherSymptoms)
        }
    }

    // Extension function to convert dp to pixels
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun applyRedOverlay(bodyPartView: View, bodyPart: String) {
        if (selectedBodyPartsMap[bodyPart] == true) {
            val drawable = bodyPartView.background.mutate() // Create a mutable copy of the drawable
            drawable.colorFilter = PorterDuffColorFilter(
                Color.parseColor("#F56767"),
                PorterDuff.Mode.SRC_IN
            ) // Apply red color filter
            bodyPartView.background = drawable
        } else {
            // Optionally, reset the background if not selected
            resetBodyPartBackground(bodyPartView)
        }
    }

    private fun resetBodyPartBackground(bodyPartView: View) {
        val drawable = bodyPartView.background.mutate() // Create a mutable copy of the drawable
        drawable.clearColorFilter() // Remove the color filter
        bodyPartView.background = drawable // Set the original drawable back
    }

    private fun resetAllBodyPartBackgrounds() {
        selectedSymptomsMap.clear()
        for (bodyPartView in bodyPartViews) {
            resetBodyPartBackground(bodyPartView)
        }
        updateTable()
    }

    private fun getAllSymptoms() {
        val repository = SymptomsRepository()
        val call = repository.getSymptomsData()

        call.enqueue(object : Callback<SymptomsResponse> {
            override fun onResponse(
                call: Call<SymptomsResponse>,
                response: Response<SymptomsResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val symptomsResponse = response.body()
                    val symptomsData = symptomsResponse?.data

                    if (symptomsData != null) {

                        maleSymptoms = symptomsData.male
                        femaleSymptoms = symptomsData.female

                    } else {
                        Log.e("symptoms fragment", "No data found in the response")
                        Toast.makeText(requireContext(), "No data found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("symptoms fragment", "API Error: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<SymptomsResponse>, t: Throwable) {
                Log.e("symptoms fragment", "API Failure: ${t.message}")
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun saveSelectedSymptomsData() {
        if (selectedSymptomsMap.isEmpty()) {
            Log.d("symptoms fragment", "No symptoms selected.")
            Toast.makeText(requireContext(), "No symptoms selected.", Toast.LENGTH_SHORT).show()
            return
        }

        // Count total selected symptoms
        val totalSelectedSymptoms = selectedSymptomsMap.values.sumOf { it.size }
        if (totalSelectedSymptoms < 4) {
            Toast.makeText(requireContext(), "Please select at least 4 symptoms to continue.", Toast.LENGTH_SHORT).show()
            return
        }

        val symptomsMap = if (tabMale.isSelected) maleSymptoms else femaleSymptoms
        val gender = if (tabMale.isSelected) "M" else "F"

        val simplifiedSymptomsMap = mutableMapOf<String, String>()
        val structuredSymptomsMap = mutableMapOf<String, List<String>>()

        for ((bodyPart, symptoms) in selectedSymptomsMap) {
            val symptomsList = symptomsMap[bodyPart]?.filter { symptom ->
                symptoms.contains(symptom.title) && symptom.gender == gender
            }?.map { it.title } ?: emptyList()

            if (symptomsList.isNotEmpty()) {
                simplifiedSymptomsMap[bodyPart] = symptomsList.joinToString(", ")
                structuredSymptomsMap[bodyPart] = symptomsList
            }
        }

        saveSymptomsDataToApi(structuredSymptomsMap)
    }

    private fun saveSymptomsDataToApi(structuredSymptomsMap: MutableMap<String, List<String>>) {

        val sharedPreferences = requireContext().getSharedPreferences("SymptomsPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("clearSymptomsData", false)
        editor.apply()

        val bundle = Bundle().apply {
            for ((key, value) in structuredSymptomsMap) {
                putStringArrayList(key, ArrayList(value))
            }
        }
        val fragment = BookSlotFragment().apply {
            arguments = bundle
        }
        (activity as HomeActivity).openFragment(fragment, "Book an Appointment", true)

    }

    private fun setDoctorDetails() {
        sharedViewModel.doctorData.observe(viewLifecycleOwner, Observer { data ->
            // Update your UI with the shared data
            doctorName.text = data.doctor_name
            doctorsDegree.text = data.education
            doctorSpecialization.text = data.doctor_department
            val textView = TextView(context)
            TextViewCompat.setTextAppearance(textView, R.style.bind_the_de)
            textView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val imageUri: Uri = Uri.parse(data.doctor_image_url)
            try {
                Glide.with(requireContext())
                    .load(imageUri)
                    .into(doctorImageIcon)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })

    }
}