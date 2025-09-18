package com.smartringpro.mannaheal.api

object  ApiConstants {
    const val LOGIN_ENDPOINT = "login"
    const val REGISTER_ENDPOINT = "register"
    const val VALIDATE_OTP_ENDPOINT = "verifyotp"
    const val GET_SPECIALIZATIONS = "departments"
    const val GET_DOCTORS = "departments/{id}"
    const val GET_SCHEDULES = "doctors/{id}/schedules"

    const val GET_USER_PROFILE = "patients/{id}"
    const val POST_USER_PROFILE = "patients/{id}"


    const val GET_APPOINTMENTS = "patients/myappointments"
    const val GET_DOCTOR_APPOINTMENTS = "doctors/myappointments"
    const val BOOK_APPOINTMENT = "appointments"
    const val GET_PRESCRIPTION = "appointments/{id}/answers"

    const val GET_DEPENDANTS = "patients/{id}/dependents"
    const val GET_FAMILY_MEMBERS_DATA = "patients/{id}/dependents"
    const val UPDATE_FAMILY_MEMBERS_DATA = "patients/{id}/dependents/{dependentId}"

    const val GET_ALL_SYMPTOMS = "getAllSymptoms"
    const val SAVE_SYMPTOMS = "patients/{id}/symptoms"

    const val GET_DISEASE_LIST = "diseases/list"

    const val FCM_TOKEN = "user/fcm-token"
}