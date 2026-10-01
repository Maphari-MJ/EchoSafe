package com.echosafe.ui.contacts

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.echosafe.R
import com.echosafe.data.model.EmergencyContact
import com.echosafe.databinding.ActivityContactsBinding
import com.echosafe.databinding.DialogAddContactBinding
import com.echosafe.ui.calibration.CalibrationActivity
import kotlinx.coroutines.launch

class ContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactsBinding
    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var adapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        // Fresh start on 2nd page: clear existing contacts so user starts clean with 0 contacts
        viewModel.clearAndLoadFresh()
    }

    private fun setupRecyclerView() {
        adapter = ContactsAdapter { contact ->
            viewModel.deleteContact(contact)
        }
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnPickContact.setOnClickListener {
            showAddContactDialog()
        }

        binding.btnProceed.setOnClickListener {
            val count = viewModel.contacts.value?.size ?: 0
            if (count < 3) {
                Toast.makeText(this, "Please add at least 3 emergency contacts before proceeding.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, CalibrationActivity::class.java))
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.contacts.observe(this) { list ->
            adapter.submitList(list)
            val count = list.size
            if (count >= 3) {
                binding.cardValidationBadge.strokeColor = getColor(R.color.guard_active_green)
                binding.tvBadgeIcon.text = "✓"
                binding.tvValidationMessage.text = "Minimum 3 Contacts Satisfied ($count/3 configured)"
                binding.tvValidationMessage.setTextColor(getColor(R.color.guard_active_green))
            } else {
                binding.cardValidationBadge.strokeColor = getColor(R.color.warning_yellow)
                binding.tvBadgeIcon.text = "⚠️"
                binding.tvValidationMessage.text = "Select Minimum 3 Contacts ($count/3 configured)"
                binding.tvValidationMessage.setTextColor(getColor(R.color.warning_yellow))
            }
        }
    }

    private fun showAddContactDialog() {
        val dialogBinding = DialogAddContactBinding.inflate(LayoutInflater.from(this))

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setTitle("Add Emergency Contact")
            .setPositiveButton("Search Database") { _, _ ->
                val query = dialogBinding.etSearchName.text.toString().trim()
                if (query.isEmpty()) {
                    Toast.makeText(this, "Please enter name and surname to search", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val foundContacts = viewModel.searchUsersByName(query)
                    if (foundContacts.isNotEmpty()) {
                        showContactSelectionDialog(foundContacts)
                    } else {
                        Toast.makeText(this@ContactsActivity, "No users found in database matching '$query'", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showContactSelectionDialog(contacts: List<EmergencyContact>) {
        val names = contacts.map { "${it.contactName} (${it.phoneNumber})" }.toTypedArray()
        val checkedItems = BooleanArray(contacts.size) { true } // default all checked

        AlertDialog.Builder(this)
            .setTitle("Select Emergency Contact")
            .setMultiChoiceItems(names, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Add Selected") { _, _ ->
                var addedCount = 0
                for (i in contacts.indices) {
                    if (checkedItems[i]) {
                        viewModel.addContact(contacts[i])
                        addedCount++
                    }
                }
                Toast.makeText(this, "Added $addedCount emergency contacts", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
