package com.smartringpro.mannaheal.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.specializations.interfaces.AppointmentData

class AppointmentSummaryAdapter(
    context: Context,
    private val dataList: List<AppointmentData>,
    private val listener: OnItemClickListener
    ) : ArrayAdapter<Any>(context, 0, dataList) {
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_appointment_summary_view, parent, false)

        val doctorName: TextView = view.findViewById(R.id.doctorName)
        val patientName: TextView = view.findViewById(R.id.patientName)
        val aTimeValue: TextView = view.findViewById(R.id.aTimeValue)
        val appointmentView: CardView = view.findViewById(R.id.appointmentView)

        doctorName.text = dataList[position].doctor_name
        patientName.text = dataList[position].patient_name
        "${dataList[position].appt_date} ${dataList[position].appt_time}".also { aTimeValue.text = it }

            appointmentView.setOnClickListener {
                listener.onItemClick(dataList[position].appt_id)
            }

        return view
    }


//    fun convertTime(time: String): String {
//        val inputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
//        val outputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
//        val date = inputFormat.parse(time)
//        return outputFormat.format(date!!)
//    }
//
//
//    fun convertDate(time: String): String {
//        val inputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
//        val outputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
//        val date = inputFormat.parse(time)
//        return outputFormat.format(date!!)
//    }
}
