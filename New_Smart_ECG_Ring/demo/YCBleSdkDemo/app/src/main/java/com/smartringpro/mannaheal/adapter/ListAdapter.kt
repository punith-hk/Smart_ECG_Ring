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
import com.smartringpro.mannaheal.api.specializations.interfaces.Specializations
import java.util.Locale

interface OnItemClickListener {
    fun onItemClick(position: Int)
}
class ListAdapter(
    context: Context,
    private val dataSource: List<Any>,
    private val listener: OnItemClickListener
    ) : ArrayAdapter<Any>(context, 0, dataSource) {
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView ?: inflater.inflate(R.layout.item_card_view, parent, false)

        val textView: TextView = view.findViewById(R.id.item_text)
        val subtextView: TextView = view.findViewById(R.id.item_sub_text)
        val imageView: ImageView = view.findViewById(R.id.item_icon)
        val checkAvailableDoctors: Button = view.findViewById(R.id.checkAvailableDoctors)
        imageView.setImageResource(R.drawable.ic_launcher_foreground)
        val dataList: List<Specializations> = dataSource.filterIsInstance<Specializations>()
        val department = dataList[position].description.lowercase(Locale.getDefault())
        textView.text = dataList[position].description
        val appIconPath = "https://app.mannaheal.com/app/"
        val imageUri: Uri = Uri.parse(appIconPath+dataList[position].icon)
        try {
            Glide.with(context)
                .load(imageUri)
                .into(imageView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        "Specialists in $department".also { subtextView.text = it }
        checkAvailableDoctors.setOnClickListener {
            listener.onItemClick(position)
        }
        return view
    }
}

