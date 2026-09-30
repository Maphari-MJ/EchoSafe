package com.echosafe.ui.vault

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.echosafe.R
import com.echosafe.data.model.EvidenceItem
import com.echosafe.databinding.ItemEvidenceBinding
import java.text.SimpleDateFormat
import java.util.Locale

class VaultAdapter(
    private val onVerifyClicked: (EvidenceItem) -> Unit
) : RecyclerView.Adapter<VaultAdapter.EvidenceViewHolder>() {

    private val items = mutableListOf<EvidenceItem>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun submitList(list: List<EvidenceItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvidenceViewHolder {
        val binding = ItemEvidenceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EvidenceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EvidenceViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class EvidenceViewHolder(private val binding: ItemEvidenceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EvidenceItem) {
            val context = binding.root.context
            binding.tvMediaTitle.text = "${item.mediaType.replace('_', ' ')} Evidence"
            binding.tvCapturedTime.text = "Captured: ${dateFormat.format(item.capturedAt.toDate())}"
            binding.tvSha256.text = item.sha256Hash

            if (item.isUploaded) {
                binding.chipSyncStatus.text = "Cloud Synced"
                binding.chipSyncStatus.setTextColor(context.getColor(R.color.guard_active_green))
                binding.chipSyncStatus.chipStrokeColor = android.content.res.ColorStateList.valueOf(context.getColor(R.color.guard_active_green))
            } else {
                binding.chipSyncStatus.text = "Queued (Offline)"
                binding.chipSyncStatus.setTextColor(context.getColor(R.color.warning_yellow))
                binding.chipSyncStatus.chipStrokeColor = android.content.res.ColorStateList.valueOf(context.getColor(R.color.warning_yellow))
            }

            when (item.mediaType) {
                "AUDIO_CLIP" -> binding.ivMediaType.setImageResource(R.drawable.ic_mic)
                "CAMERA_SNAPSHOT" -> binding.ivMediaType.setImageResource(R.drawable.ic_shield)
                else -> binding.ivMediaType.setImageResource(R.drawable.ic_vault)
            }

            binding.tvVerifyAction.setOnClickListener {
                onVerifyClicked(item)
            }
        }
    }
}
