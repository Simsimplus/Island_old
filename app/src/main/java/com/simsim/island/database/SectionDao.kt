package com.simsim.island.database

import androidx.room.*
import com.simsim.island.model.Section
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Update
    suspend fun updateSection(section: Section)

    @Query("select * from sectionList where isShow=1 order by sectionIndex asc")
    fun getAllSectionFlow(): Flow<List<Section>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSection(sections: List<Section>)

    @Query("select exists(select * from sectionList)")
    suspend fun isAnySectionInDB():Boolean
}