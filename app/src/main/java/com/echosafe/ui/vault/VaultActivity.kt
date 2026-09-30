package com.echosafe.ui.vault

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.echosafe.R
import com.echosafe.data.model.EvidenceItem
import com.echosafe.databinding.ActivityVaultBinding

class VaultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVaultBinding
    private val viewModel: VaultViewModel by viewModels()
    private lateinit var adapter: VaultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.loadEvidence()
    }

    private fun setupRecyclerView() {
        adapter = VaultAdapter { item ->
            showHashVerificationDialog(item)
        }
        binding.rvEvidence.layoutManager = LinearLayoutManager(this)
        binding.rvEvidence.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnSimulateCapture.setOnClickListener {
            viewModel.captureNewEvidencePackage()
            Toast.makeText(this, "Evidentiary Package Captured & SHA-256 Generated", Toast.LENGTH_SHORT).show()
            binding.rvEvidence.postDelayed({
                viewModel.loadEvidence()
            }, 500)
        }

        binding.btnSyncNow.setOnClickListener {
            viewModel.syncNow()
            Toast.makeText(this, "WorkManager Resiliency Sync Scheduled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.evidenceList.observe(this) { list ->
            adapter.submitList(list)
            if (list.isEmpty()) {
                // Auto-generate initial evidentiary custody sample if empty for immediate inspection
                viewModel.captureNewEvidencePackage()
            }
        }
    }

    private fun showHashVerificationDialog(item: EvidenceItem) {
        val isValid = viewModel.verifyEvidenceIntegrity(item)
        val statusText = if (isValid) "VALID ✓ (Untampered Legal Chain of Custody)" else "INVALID / TAMPERED"

        AlertDialog.Builder(this)
            .setTitle("Cryptographic Hash Verification")
            .setMessage(
                "Evidence ID: ${item.evidenceId}\n" +
                "Media Type: ${item.mediaType}\n\n" +
                "Calculated SHA-256 Digest:\n${item.sha256Hash}\n\n" +
                "Chain of Custody Status:\n$statusText"
            )
            .setPositiveButton("OK", null)
            .show()
    }
}
