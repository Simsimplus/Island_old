package com.simsim.island.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simsim.island.model.UpdateRecord
import kotlinx.coroutines.selects.select

@Dao
interface RecordDao {
    @Query("select * from UpdateRecord limit 1")
    suspend fun getRecord():UpdateRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: UpdateRecord)
}