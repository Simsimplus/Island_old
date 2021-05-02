package com.simsim.island.database

import androidx.room.*
import com.simsim.island.model.BlockRule
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockRule(blockRule: BlockRule):Long

    @Query("select * from BlockRule")
    suspend fun getAllBlockRules():List<BlockRule>

    @Query("select * from BlockRule")
    fun getAllBlockRulesFlow():Flow<List<BlockRule>>

    @Query("select * from BlockRule where `index`=:index")
    suspend fun getBlockRule(index:Long):BlockRule

    @Delete
    suspend fun deleteBlockRule(blockRule: BlockRule)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateBlockRule(blockRule: BlockRule)
}