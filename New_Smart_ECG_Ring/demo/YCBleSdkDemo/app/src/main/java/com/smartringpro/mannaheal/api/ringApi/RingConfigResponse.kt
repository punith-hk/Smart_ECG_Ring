package com.smartringpro.mannaheal.api.ringApi

data class RingConfigResponse(
    val message: String,
    val data: List<RingConfig>
) {
}

data class RingConfig(
    val type:String,
    val code: String,
    val interval: Int
) {
}