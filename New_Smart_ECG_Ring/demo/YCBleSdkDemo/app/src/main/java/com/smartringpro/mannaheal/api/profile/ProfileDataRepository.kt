package com.smartringpro.mannaheal.api.profile

import android.telephony.emergency.EmergencyNumber
import com.smartringpro.mannaheal.api.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import java.io.File

class ProfileDataRepository {

    private val profileDataApi = ApiClient.retrofit.create(ProfileDataApiService::class.java)

    fun getDependentsData(userId: Int): Call<DependentsResponse> {
        return profileDataApi.getDependents(userId)
    }

    fun deleteDependentById(userId: Int, dependentId: Int): Call<AddProfileDataResponse> {
        return  profileDataApi.deleteDependentById(userId, dependentId)
    }

    fun getUserProfileData(userId: Int): Call<ProfileDataResponse> {
        return profileDataApi.getUserProfileData(userId)
    }

    fun saveUserProfileData(
        userId: Int,
        id: Int,
        firstName: String,
        lastName: String,
        email: String,
        gender: String,
        phoneNumber: String,
        emergencyPhone: String,
        dob: String,
        bloodGroup: String,
        address: String,
        city: String,
        state: String,
        country: String,
        pincode: String,
        height: String,
        weight: String,
        allergy: Int,
        status: Int,
        existingDiseases : String,
        existingMedications: String,
        profileImageFile: File?
    ): Call<AddProfileDataResponse> {
        val requestBody = { value: String -> RequestBody.create("text/plain".toMediaTypeOrNull(), value) }

        val profileImagePart = profileImageFile?.let {
            val requestFile = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", it.name, requestFile)
        }

        return profileDataApi.saveUserProfileData(
            id,
            requestBody(userId.toString()),
            requestBody(id.toString()),
            requestBody(firstName),
            requestBody(lastName),
            requestBody(email),
            requestBody(gender),
            requestBody(phoneNumber),
            requestBody(emergencyPhone),
            requestBody(dob),
            requestBody(bloodGroup),
            requestBody(address),
            requestBody(city),
            requestBody(state),
            requestBody(country),
            requestBody(pincode),
            requestBody(height),
            requestBody(weight),
            requestBody(allergy.toString()),
            requestBody(status.toString()),
            requestBody(existingDiseases),
            requestBody(existingMedications),
            profileImagePart
        )
    }

    fun saveFamilyMembersData(
        userId: Int,
        name: String,
        relation: String,
        gender: String,
        dob: String,
        bloodGroup: String,
        address: String?,
        city: String?,
        state: String?,
        country: String?,
        pincode: String?,
        height: String?,
        weight: String?,
        emergencyPhone: String,
        existingDiseases : String,
        existingMedications: String,
        profileImageFile: File?
    ): Call<AddProfileDataResponse> {
        val requestBody = { value: String -> RequestBody.create("text/plain".toMediaTypeOrNull(), value) }

        val profileImagePart = profileImageFile?.let {
            val requestFile = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", it.name, requestFile)
        }

        return profileDataApi.saveFamilyMemberData(
            userId,
            requestBody(name),
            requestBody(relation),
            requestBody(gender),
            requestBody(dob),
            requestBody(bloodGroup),
            address?.let { requestBody(it) },
            city?.let { requestBody(it) },
            state?.let { requestBody(it) },
            country?.let { requestBody(it) },
            pincode?.let { requestBody(it) },
            height?.let { requestBody(it) },
            weight?.let { requestBody(it) },
            requestBody(emergencyPhone),
            requestBody(existingDiseases),
            requestBody(existingMedications),
            profileImagePart
        )
    }

    fun updateFamilyMembersData(
        userId: Int,
        dependentId: Int,
        name: String,
        relation: String,
        gender: String,
        dob: String,
        bloodGroup: String,
        address: String?,
        city: String?,
        state: String?,
        country: String?,
        pincode: String?,
        height: String?,
        weight: String?,
        emergencyPhone: String,
        existingDiseases : String,
        existingMedications: String,
        profileImageFile: File?
    ): Call<AddProfileDataResponse> {
        val requestBody = { value: String -> RequestBody.create("text/plain".toMediaTypeOrNull(), value) }

        val profileImagePart = profileImageFile?.let {
            val requestFile = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", it.name, requestFile)
        }

        return profileDataApi.updateFamilyMemberData(
            userId,
            dependentId,
            requestBody(name),
            requestBody(relation),
            requestBody(gender),
            requestBody(dob),
            requestBody(bloodGroup),
            address?.let { requestBody(it) },
            city?.let { requestBody(it) },
            state?.let { requestBody(it) },
            country?.let { requestBody(it) },
            pincode?.let { requestBody(it) },
            height?.let { requestBody(it) },
            weight?.let { requestBody(it) },
            requestBody(emergencyPhone),
            requestBody(existingDiseases),
            requestBody(existingMedications),
            profileImagePart
        )
    }
}