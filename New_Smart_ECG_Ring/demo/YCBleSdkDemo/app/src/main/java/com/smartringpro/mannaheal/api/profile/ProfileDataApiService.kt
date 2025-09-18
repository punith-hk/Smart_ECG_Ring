package com.smartringpro.mannaheal.api.profile

import com.smartringpro.mannaheal.api.ApiConstants
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ProfileDataApiService {

    @GET(ApiConstants.GET_FAMILY_MEMBERS_DATA)
    fun getDependents(
        @Path("id") userId: Int
    ): Call<DependentsResponse>

    @DELETE(ApiConstants.UPDATE_FAMILY_MEMBERS_DATA)
    fun deleteDependentById(
        @Path("id") userId: Int,
        @Path("dependentId") dependentId: Int,
    ): Call<AddProfileDataResponse>

    @GET(ApiConstants.GET_USER_PROFILE)
    fun getUserProfileData(
        @Path("id") userId: Int
    ): Call<ProfileDataResponse>

    @Multipart
    @POST(ApiConstants.POST_USER_PROFILE)
    fun saveUserProfileData(
        @Path("id") userId: Int,
        @Part("user_id") userIdPart: RequestBody,
        @Part("id") id: RequestBody,
        @Part("first_name") firstName: RequestBody?,
        @Part("last_name") lastName: RequestBody?,
        @Part("email") email: RequestBody?,
        @Part("gender") gender: RequestBody,
        @Part("phone_number") phoneNumber: RequestBody?,
        @Part("emergency_phone") emergencyPhone: RequestBody?,
        @Part("dob") dob: RequestBody,
        @Part("blood_group") bloodGroup: RequestBody?,
        @Part("address") address: RequestBody?,
        @Part("city") city: RequestBody?,
        @Part("state") state: RequestBody?,
        @Part("country") country: RequestBody?,
        @Part("pincode") pincode: RequestBody?,
        @Part("height") height: RequestBody,
        @Part("weight") weight: RequestBody?,
        @Part("allergy") allergy: RequestBody,
        @Part("status") status: RequestBody,
        @Part("existing_diseases") existing_diseases: RequestBody,
        @Part("existing_medications") existing_medications: RequestBody,
        @Part profileImage: MultipartBody.Part?
    ): Call<AddProfileDataResponse>

    @Multipart
    @POST(ApiConstants.GET_FAMILY_MEMBERS_DATA)
    fun saveFamilyMemberData(
        @Path("id") userId: Int,
        @Part("name") name: RequestBody,
        @Part("relation") relation: RequestBody,
        @Part("gender") gender: RequestBody,
        @Part("dob") dob: RequestBody,
        @Part("blood_group") bloodGroup: RequestBody,
        @Part("address") address: RequestBody?,
        @Part("city") city: RequestBody?,
        @Part("state") state: RequestBody?,
        @Part("country") country: RequestBody?,
        @Part("pincode") pincode: RequestBody?,
        @Part("height") height: RequestBody?,
        @Part("weight") weight: RequestBody?,
        @Part("emergency_phone") emergencyPhone: RequestBody?,
        @Part("existing_diseases") existing_diseases: RequestBody,
        @Part("existing_medications") existing_medications: RequestBody,
        @Part profileImage: MultipartBody.Part?
    ): Call<AddProfileDataResponse>

    @Multipart
    @POST(ApiConstants.UPDATE_FAMILY_MEMBERS_DATA)
    fun updateFamilyMemberData(
        @Path("id") userId: Int,
        @Path("dependentId") dependentId: Int,
        @Part("name") name: RequestBody,
        @Part("relation") relation: RequestBody,
        @Part("gender") gender: RequestBody,
        @Part("dob") dob: RequestBody,
        @Part("blood_group") bloodGroup: RequestBody,
        @Part("address") address: RequestBody?,
        @Part("city") city: RequestBody?,
        @Part("state") state: RequestBody?,
        @Part("country") country: RequestBody?,
        @Part("pincode") pincode: RequestBody?,
        @Part("height") height: RequestBody?,
        @Part("weight") weight: RequestBody?,
        @Part("emergency_phone") emergencyPhone: RequestBody?,
        @Part("existing_diseases") existing_diseases: RequestBody,
        @Part("existing_medications") existing_medications: RequestBody,
        @Part profileImage: MultipartBody.Part?
    ): Call<AddProfileDataResponse>

}