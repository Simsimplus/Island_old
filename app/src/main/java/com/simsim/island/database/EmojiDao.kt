package com.simsim.island.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simsim.island.model.Emoji
import kotlinx.coroutines.flow.Flow

@Dao
interface EmojiDao {
    @Query("select exists(select * from Emoji)")
    suspend fun isAny():Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEmojis(emojis:List<Emoji>)

    @Query("select * from Emoji order by emojiIndex asc")
    fun getAllEmojisFlow(): Flow<List<Emoji>>
}