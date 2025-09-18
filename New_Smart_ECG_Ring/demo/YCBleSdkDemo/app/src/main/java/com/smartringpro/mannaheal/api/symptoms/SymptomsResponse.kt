package com.smartringpro.mannaheal.api.symptoms

data class SymptomsResponse(
    val response: String,
    val data: SymptomsData
) {
    data class SymptomsData(
        val male: Map<String, List<Symptom>>,
        val female: Map<String, List<Symptom>>
    )

    data class Symptom(
        val id: Int,
        val title: String,
        val question_id: String?,
        val body_part: String,
        val gender: String,
        val created_at: String
    )
}

data class SaveSymptomsResponse(
    val response: String,
    val message: String,
    val data: SymptomsData
) {
    data class SymptomsData(
        val symptoms: String
    )
}


data class Disease(
    val id: Int,
    val disease_name: String
)

