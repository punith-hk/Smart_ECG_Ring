package com.smartringpro.mannaheal.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.specializations.interfaces.AppointmentData
import com.smartringpro.mannaheal.util.Constants

class AppointmentListAdapter(
    context: Context,
    private val dataList: List<AppointmentData>,
    private val listener: OnItemClickListener
    ) : ArrayAdapter<Any>(context, 0, dataList) {
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_appointment_view, parent, false)

        val doctorName: TextView = view.findViewById(R.id.doctorName)
        val aDateValue: TextView = view.findViewById(R.id.aDateValue)
        val aTimeValue: TextView = view.findViewById(R.id.aTimeValue)
        val aStatusValue: TextView = view.findViewById(R.id.aStatusValue)
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val appointmentView: CardView = view.findViewById(R.id.appointmentView)
        val listView: ImageView = view.findViewById(R.id.fwdArrow)

        doctorName.text = dataList[position].doctor_name
        aDateValue.text = dataList[position].appt_date
        aTimeValue.text = dataList[position].appt_time
        val imageUri: Uri = Uri.parse(dataList[position].doctor_image_url)
        try {
            Glide.with(context)
                .load(imageUri)
                .circleCrop()
                .into(profileImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        when (dataList[position].status) {
            1 -> {
                aStatusValue.text = Constants.APPOINTMENTSTATUS[0]
                aStatusValue.setTextColor(Color.GREEN)
                aStatusValue.setTypeface(aStatusValue.typeface, Typeface.BOLD)
                listView.visibility = View.GONE
            }

            2 -> {
                aStatusValue.text = Constants.APPOINTMENTSTATUS[1]
                aStatusValue.setTextColor(Color.RED)
                aStatusValue.setTypeface(aStatusValue.typeface, Typeface.BOLD)
                listView.visibility = View.GONE
            }

            3 -> {
                listView.visibility = View.VISIBLE
                aStatusValue.text = Constants.APPOINTMENTSTATUS[2]
                aStatusValue.setTextColor(Color.BLUE)
                aStatusValue.setTypeface(aStatusValue.typeface, Typeface.BOLD)

                appointmentView.setOnClickListener {
                    listener.onItemClick(dataList[position].appt_id)
                }
            }
        }


        return view
    }
}
