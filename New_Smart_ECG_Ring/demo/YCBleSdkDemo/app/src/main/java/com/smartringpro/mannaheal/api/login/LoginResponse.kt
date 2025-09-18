package com.smartringpro.mannaheal.api.login

data class LoginResponse(
    val response: Int,
    val message: Any?, // Can be String or List<String>
    val user_id: Int?
) {
    fun getFormattedMessage(): String {
        return when (message) {
            is String -> message
            is List<*> -> (message as List<String>).joinToString(separator = "\n")
            else -> "An unexpected error occurred."
        }
    }
}

