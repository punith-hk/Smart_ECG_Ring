package com.smartringpro.mannaheal.api.specializations.interfaces

data class Departments(
    val response: Int,
    val data: DepartmentData,
) {
}


data class DepartmentData(
    val department_id: Int,
    val description: String,
    val doctors: List<Doctor>
) {
}


data class Doctor(
    val id: Int,
    val user_id: Int,
    val doctor_name: String,
    val education: String,
    val doctor_department: String,
    val doctor_specialization: String,
    val doctor_image_url: String,
//    val first_name: Any,
//    val last_name: Any,
//    val specialization_id: Any,
//    val department_id: Any,
    val phone_number: Any,
//    val email: Any,
//    val gender: Any,
//    val dob: Any,
//    val about_me: Any,
    val address_line_1: Any,
    val address_line_2: Any,
    val city: Any,
    val state: Any,
    val country: Any,
    val pincode: Any,
//    val filepath: Any,
//    val filename: Any,
//    val status: Any,
//    val created_at: Any,
//    val updated_at: Any,
//    val deleted_at: Any,
//    val department: Any,
//    val specialization: Any


) {
}