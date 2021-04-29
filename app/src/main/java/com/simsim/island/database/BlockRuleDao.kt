package com.simsim.island.database

import androidx.room.*
import com.simsim.island.model.BlockRule

@Dao
interface BlockRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockRule(blockRule: BlockRule)

    @Query("select * from BlockRule")
    suspend fun getAllBlockRules():List<BlockRule>

    @Query("delete from BlockRule where `index`=:blockRuleIndex")
    suspend fun deleteBlockRules(blockRuleIndex: Int)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateBlockRule(blockRule: BlockRule)
}