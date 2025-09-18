package com.smartringpro.mannaheal.api.linkedAccountData

data class LastRingDataResponse(
    val message: String,
    val data: List<RingDataItem>,
    val location: LocationData?
)

data class RingDataItem(
    val type: String,
    val value: String,
    val symptom: String?,
    val message: String?,
    val predictionKey: String?
)

data class LocationData(
    val id: Int,
    val user_id: Int,
    val os: Int?,
    val version: String?,
    val app_version: String?,
    val firmware_version: String?,
    val location: Int?,
    val bluetooth: Int?,
    val ring: Int?,
    val status: String?,
    val battery: Int?,
    val latitude: String?,
    val longitude: String?,
    val created_at: String?,
    val updated_at: String?
)

data class AddLinkedAccountResponse(
    val receiver_id: Int,
    val message: String
)

data class CaretakerRequestBody(
    val user_id: Int,
    val phone_number: String
)

data class CaretakerVerifyOtpRequestBody(
    val user_id: Int,
    val receiver_id: Int,
    val otp: Int,
    val relation: String
)

data class CaretakerVerifyOtpResponse(
    val message: String
)

data class LinkedAccountInfo(
    val id: Int,
    val name: String,
    val phone_number: String,
    val relation: String? = null
)
