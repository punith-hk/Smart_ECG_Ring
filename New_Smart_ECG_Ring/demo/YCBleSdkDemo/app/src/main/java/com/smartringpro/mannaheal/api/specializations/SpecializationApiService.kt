package com.smartringpro.mannaheal.api.specializations

import com.smartringpro.mannaheal.api.ApiConstants
import com.smartringpro.mannaheal.api.specializations.interfaces.Answers
import com.smartringpro.mannaheal.api.specializations.interfaces.Appointments
import com.smartringpro.mannaheal.api.specializations.interfaces.Departments
import com.smartringpro.mannaheal.api.specializations.interfaces.Dependants
import com.smartringpro.mannaheal.api.specializations.interfaces.Schedules
import com.smartringpro.mannaheal.api.specializations.interfaces.Specializations
import com.smartringpro.mannaheal.api.specializations.interfaces.bookAppointmentResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SpecializationApiService {
    @GET(ApiConstants.GET_SPECIALIZATIONS)
    fun getSpecialists(): Call<List<Specializations>>

    @GET(ApiConstants.GET_DOCTORS)
    fun getDoctors(@Path("id") id: Int): Call<Departments>

    @GET(ApiConstants.GET_SCHEDULES)
    fun getSchedules(@Path("id") id: Int): Call<Schedules>

    @GET(ApiConstants.GET_APPOINTMENTS)
    fun getAppointments(@Query("patient_id") id: Int): Call<Appointments>

    @GET(ApiConstants.GET_DEPENDANTS)
    fun getDependants(@Path("id") id: Int): Call<Dependants>

    @GET(ApiConstants.GET_PRESCRIPTION)
    fun getPrescription(@Path("id") id: Int): Call<List<Answers>>

    @FormUrlEncoded
    @POST(ApiConstants.BOOK_APPOINTMENT)
    fun bookAppointments(
        @Field("appointment_date") appointment_date: String,
        @Field("appointment_time") appointment_time: String,
        @Field("doctor_id") doctor_id: Int,
        @Field("patient_id") patient_id: Int,
        @Field("purpose") purpose: String,
        @Field("status") status: Int,
        @Field("dependent_id") dependent_id: Int,
        @Field("type") type: Int
    ): Call<bookAppointmentResponse>

    @GET(ApiConstants.GET_DOCTOR_APPOINTMENTS)
    fun getDoctorAppointments(@Query("doctor_id") id: Int): Call<Appointments>

}