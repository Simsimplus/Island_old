package com.simsim.island.database

import androidx.paging.PagingSource
import androidx.room.*
import com.simsim.island.model.BasicThread
import com.simsim.island.model.PoThread
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao  {

    @Query("select * from basicThread where poThreadId =:poThreadId order by replyThreadId asc ")
    suspend fun getAllReplyThreads(poThreadId:String): List<BasicThread>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplyThreads(replyThreads:List<BasicThread>)

    @Query("select * from PoThread where ThreadId=:poThreadId order by collectTime desc")
    suspend fun getPoThread(poThreadId: String):PoThread

    @Query("select * from PoThread where ThreadId=:poThreadId")
    fun getFlowPoThread(poThreadId: String):Flow<PoThread>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPoThreads(poThreads:List<PoThread>)

    @Query("select * from poThread where section=:section order by collectTime asc")
    fun getAllPoThreadsBySection(section:String):PagingSource<Int,PoThread>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoThread(poThread: PoThread)

    @Update
    suspend fun updatePoThread(poThread: PoThread)

    @Query("delete from poThread")
    suspend fun clearAllPoThread()

//    @Insert
//    suspend fun starPoThread(savedPoThread: SavedPoThread)
//
//    @Delete
//    suspend fun deletePoThread(savedPoThread: SavedPoThread)
//
//    @Insert
//    suspend fun starAllBasicThread(savedBasicThreads: List<SavedBasicThread>)

}