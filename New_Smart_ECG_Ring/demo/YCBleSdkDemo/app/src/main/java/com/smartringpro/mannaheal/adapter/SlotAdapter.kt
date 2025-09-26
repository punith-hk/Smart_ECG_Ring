package com.smartringpro.mannaheal.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.ui.fragments.SlotClickListener

class SlotAdapter(
    private val slots: List<Int>,
    private val listener: SlotClickListener,
    private val slotDate: String,
    private var selectedSlot: Int?,
    private val bookedSlot: List<Int>
) : RecyclerView.Adapter<SlotAdapter.SlotViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_slot, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        holder.bind(slots[position], listener, slotDate, position, bookedSlot)
    }

    override fun getItemCount() = slots.size

    inner class SlotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val slotButton: Button = view as Button

        fun bind(slot: Int, listener: SlotClickListener, slotDate: String, position: Int, bookedSlot: List<Int>) {
            val slotTime = formatTime(slot)
            slotButton.text = slotTime

            slotButton.setTextColor(Color.BLACK)

            if( bookedSlot.contains(slot)) {
                slotButton.setTextColor(Color.WHITE)
                slotButton.isEnabled = false
            } else {
                if (slot == selectedSlot ) {
                    slotButton.isSelected = true
                    slotButton.setTextColor(Color.WHITE)
                } else {
                    slotButton.isSelected = false
                    slotButton.isEnabled = true
                    slotButton.setTextColor(Color.BLACK)
                }
                slotButton.setOnClickListener {
                    if (selectedSlot != slot) {
                        selectedSlot = slot // ✅ Update selection
                        listener.onSlotClicked(slotTime,slotDate,slot) // ✅ Notify parent
                        notifyDataSetChanged() // ✅ Refresh UI
                    }
                }
            }
            slotButton.isSelected = (slot == selectedSlot)
            // Click Listener
        }

        private fun formatTime(slot: Int): String {
            val times = listOf(
                "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM",
                "12:00 PM", "12:30 PM", "01:00 PM", "01:30 PM", "02:00 PM", "02:30 PM",
                "03:00 PM", "03:30 PM", "04:00 PM", "04:30 PM", "05:00 PM", "05:30 PM",
                "06:00 PM", "06:30 PM", "07:00 PM", "07:30 PM", "08:00 PM", "08:30 PM", "09:00 PM"
            )
            return times.getOrNull(slot - 1) ?: "Invalid Time"
        }

    }
}

