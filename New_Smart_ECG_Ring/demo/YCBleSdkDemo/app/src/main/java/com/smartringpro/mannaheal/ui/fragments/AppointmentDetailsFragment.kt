package com.smartringpro.mannaheal.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.register.SpecializationRepository
import com.smartringpro.mannaheal.api.specializations.interfaces.Answers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.smartringpro.mannaheal.databinding.FragmentAppointmentDetailsBinding


class AppointmentDetailsFragment : Fragment() {
    private var _binding: FragmentAppointmentDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getPrescriptionList()
    }

    fun getPrescriptionList() {
        val appointmentId = arguments?.getInt("appointmentId")
//        val appointmentId = 38
        SpecializationRepository().getPrescription(appointmentId!!).enqueue(object: Callback<List<Answers>> {
            override fun onResponse(
                call: Call<List<Answers>>,
                response: Response<List<Answers>>
            ) {
                val apiResp = response.body()?.get(0)
                if (apiResp != null) {
                    //Bind medicine
                    binding.dName.text = apiResp.doctor_name
                    binding.dPhone.text = apiResp.doctor_department
//                    binding.dAddress.text = apiResp.purpose
                    binding.pid.text
                    "Patient ID : ${apiResp.patient_code}".also { binding.pid.text = it }
                    apiResp.patient_name.also { binding.pName.text = it }
                    apiResp.appt_date.also { binding.aDate.text = it }
                    apiResp.appt_time.also { binding.aTime.text = it }
                    binding.textDiagnosis.text = apiResp.disease_name

                    //bind remarks
                    binding.textRemarks.text = apiResp.remarks
                    for (i in 0 until apiResp.prescriptions.count()) {
                        val tableRow = TableRow(requireContext())
                        tableRow.layoutParams = TableRow.LayoutParams(
                            TableRow.LayoutParams.MATCH_PARENT,
                            TableRow.LayoutParams.WRAP_CONTENT
                        )

                        val medicinename = TextView(requireContext())
                        medicinename.text = apiResp.prescriptions[i].medicine
                        medicinename.setTextColor(Color.WHITE)
                        medicinename.setPadding(16, 8, 16, 8)
                        medicinename.layoutParams = TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT
                        )

                        val dosage = TextView(requireContext())
                        dosage.text = apiResp.prescriptions[i].notes
                        dosage.setTextColor(Color.WHITE)
                        dosage.setPadding(16, 8, 16, 8)
                        dosage.layoutParams = TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT
                        )

                        val duration = TextView(requireContext())
                        duration.text = apiResp.prescriptions[i].duration
                        duration.setTextColor(Color.WHITE)
                        duration.setPadding(16, 8, 16, 8)
                        duration.layoutParams = TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT
                        )

                        tableRow.addView(medicinename)
                        tableRow.addView(dosage)
                        tableRow.addView(duration)

                        binding.medicineTable.addView(tableRow)

                    }
                    //Bind Vital

                    var addedVitals = mutableListOf<String>()
                    for (i in 0 until apiResp.vittals.count()) {
                        val vQuestion = apiResp.vittals[i].vittal_question
                        if(!addedVitals.contains(vQuestion)) {
                            addedVitals.add(vQuestion)
                            val vitaltr = TableRow(requireContext())
                            vitaltr.layoutParams = TableRow.LayoutParams(
                                TableRow.LayoutParams.MATCH_PARENT,
                                TableRow.LayoutParams.WRAP_CONTENT
                            )
                            val vitalQuestion = TextView(requireContext())
                            vitalQuestion.text = vQuestion
                            vitalQuestion.setTextColor(Color.WHITE)
                            vitalQuestion.setPadding(16, 8, 16, 8)
                            vitalQuestion.layoutParams = TableRow.LayoutParams(
                                TableRow.LayoutParams.WRAP_CONTENT,
                                TableRow.LayoutParams.WRAP_CONTENT
                            )
                            val vitalValue = TextView(requireContext())
                            "${apiResp.vittals[i].value} ${apiResp.vittals[i].unit}".also { vitalValue.text = it }
                            vitalValue.setTextColor(Color.WHITE)
                            vitalValue.setPadding(16, 8, 16, 8)
                            vitalValue.layoutParams = TableRow.LayoutParams(
                                TableRow.LayoutParams.WRAP_CONTENT,
                                TableRow.LayoutParams.WRAP_CONTENT
                            )

                            vitaltr.addView(vitalQuestion)
                            vitaltr.addView(vitalValue)
                            binding.VitalTable.addView(vitaltr)
                        }
                    }

                    // Filter the diseases list to include only those with answer 1
                    val filteredDiseases = apiResp.diseases.filter { it.answer == 1 }
                    for (i in 0 until filteredDiseases.count()) {
                        val diseasetr = TableRow(requireContext())
                        diseasetr.layoutParams = TableRow.LayoutParams(
                            TableRow.LayoutParams.MATCH_PARENT,
                            TableRow.LayoutParams.WRAP_CONTENT
                        )
                        val diseaseQuestion = TextView(requireContext())
                        diseaseQuestion.text = filteredDiseases[i].disease_question
                        diseaseQuestion.setTextColor(Color.WHITE)
                        diseaseQuestion.layoutParams = TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT
                        )

                        val isDisease = TextView(requireContext())
                        isDisease.text = if ( filteredDiseases[i].answer == 1) "Yes" else "No"
                        isDisease.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_btn11))
                        isDisease.setTextColor(Color.WHITE)
                        isDisease.setPadding(16, 8, 16, 8)
                        isDisease.layoutParams = TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT
                        )

                        diseasetr.addView(diseaseQuestion)
                        diseasetr.addView(isDisease)
                        binding.symptomsTable.addView(diseasetr)
                    }

                    // Bind Diagnosis
                    Log.i("Answers", "onResponse: $apiResp")

                }
            }

            override fun onFailure(call: Call<List<Answers>>, t: Throwable) {
                TODO("Not yet implemented")
            }
        })

    }

}