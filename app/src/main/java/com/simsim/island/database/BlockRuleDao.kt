package com.simsim.island.database

import androidx.room.*
import com.simsim.island.model.BlockRule
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockRule(blockRule: BlockRule)

    @Query("select * from BlockRule")
    fun getAllBlockRulesFlow():Flow<List<BlockRule>>

    @Delete
    suspend fun deleteBlockRule(blockRule: BlockRule)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateBlockRule(blockRule: BlockRule)
}