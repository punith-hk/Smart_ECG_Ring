package com.smartringpro.mannaheal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.ui.fragments.LinkedAccount

class LinkedAccountAdapter(
    private val accounts: List<LinkedAccount>,
    private val onItemClick: (LinkedAccount) -> Unit
) : RecyclerView.Adapter<LinkedAccountAdapter.LinkedAccountViewHolder>() {

    inner class LinkedAccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.linkedAccNameTextView)
        val relationTextView: TextView = itemView.findViewById(R.id.linkedAccRelationTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(accounts[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkedAccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_linked_accounts, parent, false)
        return LinkedAccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: LinkedAccountViewHolder, position: Int) {
        val account = accounts[position]
        holder.nameTextView.text = account.name
        holder.relationTextView.text = account.relation
    }

    override fun getItemCount(): Int = accounts.size
}

