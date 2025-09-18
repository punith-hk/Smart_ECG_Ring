package com.smartringpro.mannaheal.api.linkedAccountData
import com.smartringpro.mannaheal.api.UserDataApiClient
import retrofit2.Call

class LinkedAccountRepository {

    private val api = UserDataApiClient.retrofit.create(LinkedAccountApiService::class.java)

    fun getLastRingData(userId: Int): Call<LastRingDataResponse> {
        return api.getLastRingData(userId)
    }

    fun getLinkedAccountData(userId: Int): Call<List<LinkedAccountInfo>> {
        return api.getLinkedAccountData(userId)
    }

    fun addLinkedAccountData(userId: Int, phoneNumber: String): Call<AddLinkedAccountResponse> {
        val body = CaretakerRequestBody(user_id = userId, phone_number = phoneNumber)
        return api.addLinkedAccountData(body)
    }

    fun verifyCaretakerOtp(request: CaretakerVerifyOtpRequestBody): Call<CaretakerVerifyOtpResponse> {
        return api.verifyCaretakerOtp(request)
    }

}