package com.smartringpro.mannaheal.api.profile

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize


data class DependentsResponse(
    val response: Int,
    val data: List<FamilyMember>
)

@Parcelize
data class FamilyMember(
    val id: Int,
    @SerializedName("patient_id") val patientId: Int,
    val name: String,
    val relation: String,
    val gender: String,
    @SerializedName("blood_group") val bloodGroup: String,
    val allergy: Int,
    val image: String?,
    val dob: String,
    val status: Int,
    val filename: String?,
    val filepath: String?,
    val age: Int,
    val emergency_phone: String?,
    val existing_diseases: String?,
    val existing_medications: String?,
    @SerializedName("dependent_image_url") val dependentImageUrl: String?
) : Parcelable

data class AddProfileDataResponse(
    val response: Int,
    val message: String
)

data class ProfileDataResponse(
    val response: Int,
    val data: ProfileData
) {
    data class ProfileData(
        val id: Int,
        val user_id: Int,
        val first_name: String?,
        val last_name: String?,
        val dob: String?,
        val gender: String?,
        val address: String?,
        val city: String?,
        val state: String?,
        val country: String?,
        val pincode: String?,
        val phone_number: String,
        val emergency_phone: String?,
        val email: String?,
        val id_proof_description: String?,
        val id_proof_file: String?,
        val blood_group: String?,
        val allergy: Int,
        val filepath: String?,
        val status: Int,
        val height: String?,
        val existing_diseases: String?,
        val existing_medications: String?,
        val weight: String?,
        val patient_name: String,
        val patient_code: String,
        val age: Int,
        val patient_image_url: String?,
        val vittals: List<Any> // Adjust type if needed (e.g., List<VitalData> if structured)
    )
}
