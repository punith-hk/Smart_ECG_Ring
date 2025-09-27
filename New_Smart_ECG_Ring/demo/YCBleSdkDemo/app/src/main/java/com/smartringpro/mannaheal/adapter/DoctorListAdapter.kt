package com.smartringpro.mannaheal.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.specializations.interfaces.Doctor

class DoctorListAdapter(
    context: Context,
    private val dataSource: List<Any>,
    private val listener: OnItemClickListener
    ) : ArrayAdapter<Any>(context, 0, dataSource) {
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView ?: inflater.inflate(R.layout.item_book_new_view, parent, false)

        val doctorName: TextView = view.findViewById(R.id.doctor_name)
        val doctorDegree: TextView = view.findViewById(R.id.doctors_degree)
        val doctorSpecilization: TextView = view.findViewById(R.id.doctors_specialization)
        val imageView: ImageView = view.findViewById(R.id.doctor_image_icon)
        val bookNow: Button = view.findViewById(R.id.bookNow)
        imageView.setImageResource(R.drawable.ic_launcher_foreground)

        val dataList: List<Doctor> = dataSource.filterIsInstance<Doctor>()
        doctorName.text = dataList[position].doctor_name
        doctorDegree.text = dataList[position].education
        doctorSpecilization.text = dataList[position].doctor_department
        val imageUri: Uri = Uri.parse(dataList[position].doctor_image_url)
        try {
            Glide.with(context)
                .load(imageUri)
                .into(imageView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bookNow.setOnClickListener {
            listener.onItemClick(position)
        }
        return view
    }
}

