package com.smartringpro.mannaheal.api.specializations.interfaces

data class Appointments (
    val response: Int,
    val data: List<AppointmentData>,
) {
}

data class AppointmentData (
    val appt_id: Int,
    val doctor_name: String,
    val appt_date: String,
    val appt_time : String,
    val status : Int,
    val doctor_image_url: String,
    val purpose: String,
    val patient_name: String,
    val patient_code: String,
    val patient_image_url: String,
    val patient_phone: String
)