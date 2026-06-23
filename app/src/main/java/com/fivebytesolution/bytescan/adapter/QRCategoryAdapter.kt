package com.fivebytesolution.bytescan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

data class QRCategory(val name: String, val iconRes: Int, val type: QRType)

enum class QRType {
    TEXT, WEBSITE, WIFI, EVENT, CONTACT, BUSINESS, LOCATION, WHATSAPP, EMAIL, TWITTER, INSTAGRAM, TELEPHONE
}

class QRCategoryAdapter(
    private val categories: List<QRCategory>,
    private val onItemClick: (QRCategory) -> Unit
) : RecyclerView.Adapter<QRCategoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = com.fivebytesolution.bytescan.databinding.ItemQrCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.itemBinding.categoryName.text = category.name
        holder.itemBinding.categoryIcon.setImageResource(category.iconRes)
        holder.itemBinding.root.setOnClickListener {
            onItemClick(category)
        }
    }

    override fun getItemCount(): Int = categories.size

    class ViewHolder(val itemBinding: com.fivebytesolution.bytescan.databinding.ItemQrCategoryBinding) : 
        RecyclerView.ViewHolder(itemBinding.root)
}
