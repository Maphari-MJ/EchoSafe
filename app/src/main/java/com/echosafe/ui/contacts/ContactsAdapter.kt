package com.echosafe.ui.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.echosafe.data.model.EmergencyContact
import com.echosafe.databinding.ItemContactBinding

class ContactsAdapter(
    private val onDeleteClicked: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    private val items = mutableListOf<EmergencyContact>()

    fun submitList(list: List<EmergencyContact>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: EmergencyContact) {
            binding.tvContactName.text = contact.contactName
            binding.tvPhoneNumber.text = contact.phoneNumber
            binding.chipRelationship.text = contact.relationship
            binding.tvDndStatus.text = if (contact.hasDndPermission) "● DND Bypass Active" else "○ Standard Push"

            binding.btnDelete.setOnClickListener {
                onDeleteClicked(contact)
            }
        }
    }
}
