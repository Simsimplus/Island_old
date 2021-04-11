package com.simsim.island.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simsim.island.model.Emoji
import kotlinx.coroutines.flow.Flow

@Dao
interface EmojiDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEmojis(emojis:List<Emoji>)

    @Query("select * from Emoji order by emojiIndex asc")
    fun getAllEmojis(): Flow<List<Emoji>>
}