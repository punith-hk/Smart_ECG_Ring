package com.smartringpro.mannaheal.api.symptoms

import com.smartringpro.mannaheal.api.ApiConstants
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface SymptomsApiService {

    @GET(ApiConstants.GET_ALL_SYMPTOMS)
    fun getAllSymptoms(): Call<SymptomsResponse>

    @GET(ApiConstants.GET_DISEASE_LIST)
    fun getDiseaseList(): Call<List<Disease>>

    @Multipart
    @POST(ApiConstants.SAVE_SYMPTOMS)
    fun saveSymptomsData(
        @Path("id") userId: Int,
        @Part("dependent_id") dependentId: RequestBody,
        @Part("appt_time") appt_time: RequestBody,
        @Part("symptom") symptoms: RequestBody
    ): Call<SaveSymptomsResponse>

}