package com.simsim.island.database

import androidx.paging.PagingSource
import androidx.room.*
import com.simsim.island.model.BasicThread
import com.simsim.island.model.PoThread
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao  {

    @Query("select * from basicThread where poThreadId =:poThreadId order by threadId asc ")
    suspend fun getAllReplyThreads(poThreadId:String): Flow<List<BasicThread>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplyThreads(replyThreads:List<BasicThread>)

    @Query("select * from poThread where section=:section order by collectTime desc")
    suspend fun getAllPoThreadsBySection(section:String):PagingSource<Int,PoThread>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoThread(poThread: PoThread)

    @Update
    suspend fun updatePoThread(poThread: PoThread)

    @Query("delete from poThread")
    suspend fun clearAll()
}