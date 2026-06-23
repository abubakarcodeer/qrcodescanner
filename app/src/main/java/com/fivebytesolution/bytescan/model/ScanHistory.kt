package com.fivebytesolution.bytescan.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)
