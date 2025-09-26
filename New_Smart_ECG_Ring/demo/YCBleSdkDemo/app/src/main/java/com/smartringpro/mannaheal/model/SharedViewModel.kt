package com.smartringpro.mannaheal.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.smartringpro.mannaheal.api.specializations.interfaces.Doctor

class SharedViewModel: ViewModel() {
    private val _sharedData = MutableLiveData<String>()
    private val _sharedDoctorData = MutableLiveData<Doctor>()
    private val _sharedRingData = MutableLiveData<Long>()
//    val sharedData: LiveData<String> get() = _sharedData
    val doctorData: LiveData<Doctor> get() = _sharedDoctorData
    val readRingData: LiveData<Long> get() = _sharedRingData

//    fun setData(data: String) {
//        _sharedData.value = data
//    }

    fun setDoctorData(data: Doctor) {
        _sharedDoctorData.value = data
    }

    fun setRingData(data: Long) {
        _sharedRingData.value = data
    }
}