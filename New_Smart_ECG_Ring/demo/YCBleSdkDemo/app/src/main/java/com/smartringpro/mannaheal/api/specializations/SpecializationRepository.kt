package com.smartringpro.mannaheal.api.register

import com.smartringpro.mannaheal.api.ApiClient
import com.smartringpro.mannaheal.api.specializations.SpecializationApiService
import com.smartringpro.mannaheal.api.specializations.interfaces.Answers
import com.smartringpro.mannaheal.api.specializations.interfaces.Appointments
import com.smartringpro.mannaheal.api.specializations.interfaces.Departments
import com.smartringpro.mannaheal.api.specializations.interfaces.Dependants
import com.smartringpro.mannaheal.api.specializations.interfaces.Schedules
import com.smartringpro.mannaheal.api.specializations.interfaces.Specializations
import com.smartringpro.mannaheal.api.specializations.interfaces.bookAppointmentResponse
import retrofit2.Call

class SpecializationRepository {
    private val spApi = ApiClient.retrofit.create(SpecializationApiService::class.java)

    fun getSpecialists(): Call<List<Specializations>> {
        return spApi.getSpecialists()
    }

    fun getDoctors(id: Int): Call<Departments>{
        return spApi.getDoctors(id)
    }

    fun getSchedules(id: Int): Call<Schedules>{
        return spApi.getSchedules(id)
    }

    fun getAppointments(id:Int): Call<Appointments>{
        return  spApi.getAppointments(id)
    }
    fun bookAppointments(
        appointment_date: String,
        appointment_time:String,
        doctor_id:Int,
        patient_id:Int,
        purpose:String,
        status:Int,
        dependent_id:Int,
        type:Int): Call<bookAppointmentResponse>{
        return  spApi.bookAppointments(
            appointment_date,
            appointment_time,
            doctor_id,
            patient_id,
            purpose,
            status,
            dependent_id,
            type
        )
    }

    fun getDependants(id:Int): Call<Dependants>{
        return  spApi.getDependants(id)
    }

    fun getPrescription(id: Int):  Call<List<Answers>>{
        return  spApi.getPrescription(id)
    }


    fun getDoctorAppointments(doctorId: Int): Call<Appointments> {
        return  spApi.getDoctorAppointments(doctorId)

    }

}