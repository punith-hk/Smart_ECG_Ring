package com.smartringpro.mannaheal.api.register

data class RegisterResponse(
    val response: Int,
    val message: Any?,
    val user_id: Int?,
    val patient_id: Int?
) {
    fun getFormattedMessage(): String {
        return when (message) {
            is String -> message
            is List<*> -> (message as List<String>).joinToString(separator = "\n")
            else -> "Unknown error"
        }
    }
}

data class FcmTokenRequest(
    val user_id: String,
    val fcm_token: String
)

data class FcmTokenResponse(
    val message: String,
    val data: FcmTokenData
)

data class FcmTokenData(
    val user_id: String,
    val fcm_token: String,
    val updated_at: String,
    val created_at: String,
    val id: Int
)
