package com.smartringpro.mannaheal.api.specializations.interfaces

data class Answers (
    val id: Int,
    val appt_date: String,
    val appt_time: String,
    val patient_id: Int,
    val doctor_id: Int,
    val purpose: String,
    val dependent_id: Int,
    val slot: String,
    val remarks: String,
    val disease_name: String,
    val patient_name: String,
    val patient_code: String,
    val patient_type: String,
    val patient_image_url: String,
    val patient_status: Int,
    val doctor_name: String,
    val doctor_department: String,
    val doctor_specialization: String,
    val doctor_image_url: String,
    val vittals: List<Vital>,
    val diseases: List<Disease>,
    val prescriptions: List<Prescription>,
    val dependent: Dependant
) {
}

data class Vital (
    val id: Int,
    val appointment_id: Int,
    val question_vittal_id: Int,
    val value: String,
    val vittal_question: String,
    val low_value: String,
    val high_value: String,
    val normal_value: String,
    val unit: String){
}

data class Disease(
    val id: Int,
    val appointment_id: Int,
    val question_disease_id: Int,
    val question: String,
    val answer: Int,
    val disease_question: String

) {

}

data class Prescription (
    val id: Int,
    val appointment_id: Int,
    val medicine: String,
    val notes: String,
    val duration: String
)