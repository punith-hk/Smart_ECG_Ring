package com.smartringpro.mannaheal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.profile.FamilyMember

class FamilyMembersAdapter(
    private val familyMembers: List<FamilyMember>,
    private val onEditClick: (FamilyMember) -> Unit,
    private val onDeleteClick: (FamilyMember) -> Unit
) : RecyclerView.Adapter<FamilyMembersAdapter.FamilyMemberViewHolder>() {

    inner class FamilyMemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImageView: ImageView = itemView.findViewById(R.id.profileImageView)
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val relationshipTextView: TextView = itemView.findViewById(R.id.relationshipTextView)
        private val bloodGroupTextView: TextView = itemView.findViewById(R.id.bloodGroupTextView)
        private val genderTextView: TextView = itemView.findViewById(R.id.genderTextView)
        private val dobTextView: TextView = itemView.findViewById(R.id.dobTextView)
        private val editIcon: ImageView = itemView.findViewById(R.id.editIcon)
        private val deleteIcon: ImageView = itemView.findViewById(R.id.deleteIcon)

        fun bind(familyMember: FamilyMember) {

            if (!familyMember.dependentImageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(familyMember.dependentImageUrl)
                    .circleCrop()
                    .into(profileImageView)
            } else {
                // If no image URL, show a placeholder
                profileImageView.setImageResource(R.drawable.baseline_account_circle_24)
            }

            val genderFullForm = when (familyMember.gender) {
                "M" -> "Male"
                "F" -> "Female"
                else -> "Unknown"
            }

            nameTextView.text = familyMember.name
            relationshipTextView.text = familyMember.relation
            bloodGroupTextView.text = familyMember.bloodGroup
            genderTextView.text = genderFullForm
            dobTextView.text = familyMember.dob

            // Set click listeners for edit and delete icons
            editIcon.setOnClickListener { onEditClick(familyMember) }
            deleteIcon.setOnClickListener { onDeleteClick(familyMember) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FamilyMemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_family_member, parent, false)
        return FamilyMemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: FamilyMemberViewHolder, position: Int) {
        holder.bind(familyMembers[position])
    }

    override fun getItemCount(): Int = familyMembers.size
}