package com.smartringpro.mannaheal.api.otp

data class OtpResponse(
    val response: Int,
    val message: String?,
    val accessToken: String?,
    val user: String?,
    val email: String?,
    val role_code: String?,
    val mobile_number: String?,
    val id: Int?
)
