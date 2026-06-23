package com.fivebytesolution.bytescan.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fivebytesolution.bytescan.model.ScanHistory

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(history: ScanHistory)

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    suspend fun getAllHistory(): List<ScanHistory>

    @Query("DELETE FROM scan_history")
    suspend fun deleteAll()
}
