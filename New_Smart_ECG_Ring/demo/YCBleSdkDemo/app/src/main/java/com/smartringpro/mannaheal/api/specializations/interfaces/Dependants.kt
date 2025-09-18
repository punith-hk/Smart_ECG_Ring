package com.smartringpro.mannaheal.api.specializations.interfaces


data class Dependants (
    val response: Int,
    val data: List<Dependant>,
) {
}

data class Dependant (
    val id : Int,
    val patient_id : Int,
    val name: String,
    val relation: String,
    val gender : String,
    val blood_group: String,
    val allergy: Int?,
    val image: String?,
    val dob: String?,
    val status: Int?,
    val age: Int?,
    val filename: String?,
    val filepath: String?,
    val dependent_image_url: String?
)

data class bookAppointmentResponse (
    val message: String
)