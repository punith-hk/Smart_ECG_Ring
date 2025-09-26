package com.smartringpro.mannaheal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.specializations.interfaces.ScheduleWeek
import com.smartringpro.mannaheal.ui.fragments.SlotClickListener


class WeekPagerAdapter(
    private var weeks: List<List<ScheduleWeek>>,
    private val listener: SlotClickListener
) : RecyclerView.Adapter<WeekPagerAdapter.WeekViewHolder>() {

    override fun getItemCount() = weeks.size // Ensure multiple pages exist

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_week, parent, false)
        return WeekViewHolder(view)
    }

    fun updateData(newWeeks: List<List<ScheduleWeek>>, date: String, slot: Int) { // ✅ Refresh dataset
        weeks = newWeeks.map { week ->
            week.map { day ->
                if(day.date != date && day.selectedSlot != slot) {
                    day.copy(selectedSlot = null) // ✅ Reset selectedSlot
                } else {
                    day
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
        holder.bind(weeks[position],listener, weeks)
    }

    inner class WeekViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val rvSlots: RecyclerView = view.findViewById(R.id.rvSlots)

        fun bind(days: List<ScheduleWeek>, listener: SlotClickListener, weeks:List<List<ScheduleWeek>>) {
            rvSlots.adapter = DayAdapter(days,object : SlotClickListener {
                override fun onSlotClicked(time: String, date: String, slot: Int) {
                    updateData(weeks, date, slot)
                    listener.onSlotClicked(time,date,slot)
                }
            })
            rvSlots.layoutManager =
                LinearLayoutManager(rvSlots.context, LinearLayoutManager.HORIZONTAL, false)
        }
    }
}
