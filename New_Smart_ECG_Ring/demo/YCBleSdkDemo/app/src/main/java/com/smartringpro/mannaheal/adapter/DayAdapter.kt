package com.smartringpro.mannaheal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.specializations.interfaces.ScheduleWeek
import com.smartringpro.mannaheal.ui.fragments.SlotClickListener

class DayAdapter(private val days: List<ScheduleWeek>,
                 private val listener: SlotClickListener
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position],listener)
    }

    override fun getItemCount() = days.size

    inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val rvDaySlots: RecyclerView = view.findViewById(R.id.rvSlots)
        private val tvDayHeader: TextView = view.findViewById(R.id.tvDayHeader)
        private val tvDateHeader: TextView = view.findViewById(R.id.tvDateHeader)
        private val noSlots: TextView = view.findViewById(R.id.noSlots)
        fun bind(day: ScheduleWeek, listener: SlotClickListener) {
            tvDayHeader.text = day.day
            tvDateHeader.text = day.date
            if(day.time_slots.isNotEmpty()) {
                noSlots.visibility = GONE
                rvDaySlots.visibility = VISIBLE
                val slotAdapter = SlotAdapter(day.time_slots, object : SlotClickListener {
                    override fun onSlotClicked(time: String, date: String, slot: Int) {
                        day.selectedSlot = slot
                        notifyDataSetChanged()
                        listener.onSlotClicked(time,date,slot)
                    }
                },day.selectedDate , day.selectedSlot, day.bookedSlots)
                rvDaySlots.adapter = slotAdapter
                rvDaySlots.layoutManager = LinearLayoutManager(rvDaySlots.context)
            } else {
                noSlots.visibility = VISIBLE
                rvDaySlots.visibility = GONE
            }
        }
    }
}