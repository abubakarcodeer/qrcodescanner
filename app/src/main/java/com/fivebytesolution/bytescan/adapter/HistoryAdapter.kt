package com.fivebytesolution.bytescan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fivebytesolution.bytescan.databinding.ItemHistoryBinding
import com.fivebytesolution.bytescan.model.ScanHistory
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private var historyList: List<ScanHistory>,
    private val onItemClick: (ScanHistory) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = historyList[position]
        holder.binding.historyResultText.text = history.result
        
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        holder.binding.historyDateText.text = dateFormat.format(Date(history.timestamp))

        holder.itemView.setOnClickListener {
            onItemClick(history)
        }
    }

    override fun getItemCount(): Int = historyList.size

    fun updateData(newHistoryList: List<ScanHistory>) {
        historyList = newHistoryList
        notifyDataSetChanged()
    }
}
