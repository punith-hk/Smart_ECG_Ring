package com.smartringpro.mannaheal.api.specializations.interfaces

data class Schedules(
    val response: Int,
    val data: List<ScheduleWeek>,
    val leaves: List<String>
) {
}

data class ScheduleWeek(
    val day: String,
    val date: String,
    val month: String,
    val selectedDate:String,
    val time_slots: List<Int>,
    var selectedSlot: Int? = null,
    var bookedSlots: List<Int> = emptyList()
)