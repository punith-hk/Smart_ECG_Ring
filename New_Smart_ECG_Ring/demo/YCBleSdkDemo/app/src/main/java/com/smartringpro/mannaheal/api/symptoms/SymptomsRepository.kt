package com.smartringpro.mannaheal.api.symptoms

import com.smartringpro.mannaheal.api.UserDataApiClient
import com.smartringpro.mannaheal.api.profile.ProfileDataResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import retrofit2.Call

class SymptomsRepository {

    private val symptomsDataApi = UserDataApiClient.retrofit.create(SymptomsApiService::class.java)

    fun getSymptomsData(): Call<SymptomsResponse> {
        return symptomsDataApi.getAllSymptoms()
    }


    fun getDiseaseList(): Call<List<Disease>> {
        return symptomsDataApi.getDiseaseList()
    }

    fun saveSymptomsData(
        userId: Int,
        dependentId: Int,
        appt_time: String,
        symptoms: String
    ): Call<SaveSymptomsResponse> {
        val requestBody = { value: String -> RequestBody.create("text/plain".toMediaTypeOrNull(), value) }

        return symptomsDataApi.saveSymptomsData(
            userId,
            requestBody(dependentId.toString()),
            requestBody(appt_time),
            requestBody(symptoms)
        )
    }
}