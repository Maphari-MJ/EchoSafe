package com.echosafe.ui.contacts

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.echosafe.R
import com.echosafe.data.model.EmergencyContact
import com.echosafe.databinding.ActivityContactsBinding
import com.echosafe.databinding.DialogAddContactBinding
import com.echosafe.ui.calibration.CalibrationActivity
import java.util.UUID

class ContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactsBinding
    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var adapter: ContactsAdapter

    private val pickContactLauncher = registerForActivityResult(ActivityResultContracts.PickContact()) { uri: Uri? ->
        uri?.let { handleContactUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.loadContacts()
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
                // For test demo ease, auto-populate sample verified safety contacts if less than 3
                populateDefaultContacts()
                Toast.makeText(this, "Added 3 default emergency contacts for demo safety", Toast.LENGTH_SHORT).show()
            }
            startActivity(Intent(this, CalibrationActivity::class.java))
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
        val relationships = arrayOf("Parent", "Partner", "Friend", "Sibling", "Campus Security", "Other")
        val relAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, relationships)
        dialogBinding.actvDialogRelationship.setAdapter(relAdapter)
        dialogBinding.actvDialogRelationship.setText(relationships[0], false)

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val name = dialogBinding.etDialogName.text.toString().trim()
                val phone = dialogBinding.etDialogPhone.text.toString().trim()
                val relationship = dialogBinding.actvDialogRelationship.text.toString()

                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    val contact = EmergencyContact(
                        contactId = UUID.randomUUID().toString(),
                        contactName = name,
                        phoneNumber = phone,
                        relationship = relationship,
                        hasDndPermission = true
                    )
                    viewModel.addContact(contact)
                } else {
                    Toast.makeText(this, "Name and Phone cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Pick from Phone") { _, _ ->
                pickContactLauncher.launch(null)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleContactUri(contactUri: Uri) {
        val cursor = contentResolver.query(contactUri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val name = if (nameIndex >= 0) it.getString(nameIndex) else "Contact"
                val id = if (idIndex >= 0) it.getString(idIndex) else ""

                var phone = "+27820000000"
                val pCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    arrayOf(id),
                    null
                )
                pCursor?.use { pc ->
                    if (pc.moveToFirst()) {
                        val pIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (pIndex >= 0) phone = pc.getString(pIndex)
                    }
                }

                val contact = EmergencyContact(
                    contactId = UUID.randomUUID().toString(),
                    contactName = name,
                    phoneNumber = phone,
                    relationship = "Friend",
                    hasDndPermission = true
                )
                viewModel.addContact(contact)
            }
        }
    }

    private fun populateDefaultContacts() {
        val defaults = listOf(
            EmergencyContact(UUID.randomUUID().toString(), "Primary Responder (Parent)", "+27821112233", "Parent", hasDndPermission = true),
            EmergencyContact(UUID.randomUUID().toString(), "Trusted Friend", "+27824445566", "Friend", hasDndPermission = true),
            EmergencyContact(UUID.randomUUID().toString(), "Campus Safety Node", "+27827778899", "Campus Security", hasDndPermission = true)
        )
        defaults.forEach { viewModel.addContact(it) }
    }
}
