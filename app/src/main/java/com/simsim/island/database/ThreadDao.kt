package com.simsim.island.database

import androidx.paging.PagingSource
import androidx.room.*
import com.simsim.island.model.BasicThread
import com.simsim.island.model.PoThread
import com.simsim.island.model.StaredPoThreads
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao  {

    @Query("select * from basicThread where poThreadId =:poThreadId and replyThreadId !=9999999 order by replyThreadId asc ")
    fun getAllReplyThreads(poThreadId:Long): PagingSource<Int,BasicThread>

    @Query("select * from BasicThread where replyThreadId=:threadId limit 1")
    fun getReplyThread(threadId:Long):BasicThread

    @Query("select * from BasicThread where poThreadId =:poThreadId order by replyThreadId desc limit 1")
    suspend fun getLastReplyThread(poThreadId:Long):BasicThread?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplyThreads(replyThreads:List<BasicThread>)

    @Query("select * from PoThread where threadId=:poThreadId limit 1")
    suspend fun getPoThread(poThreadId: Long):PoThread?

    @Query("select * from PoThread where threadId=:poThreadId")
    fun getFlowPoThread(poThreadId: Long):Flow<PoThread?>

    @Query("select * from poThread where section=:section order by collectTime desc limit 1")
    suspend fun getLastPoThread(section:String):PoThread?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPoThreads(poThreads:List<PoThread>)

    @Query("select exists(select * from PoThread where threadId=:poThreadId)")
    fun isPoThreadCollected(poThreadId: Long):Boolean

    @Query("select exists(select * from PoThread)")
    fun isThereAnyPoThreadInDB():Flow<Boolean>

    @Query("select * from poThread where section=:section order by pageIndex asc")
    fun getAllPoThreadsBySection(section:String):PagingSource<Int,PoThread>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
////    suspend fun insertPoThread(poThread: PoThread)

    @Update
    suspend fun updatePoThread(poThread: PoThread)

    @Query("delete from poThread where isStar=:isStar")
    suspend fun clearAllPoThread(isStar:Boolean=false)

    @Query("delete from BasicThread where poThreadId=:poThreadId")
    suspend fun clearAllReplyThreads(poThreadId: Long)

    @Query("select exists(select * from StaredPoThreads where poThreadId=:poThreadId)")
    fun isPoThreadStared(poThreadId: Long):Boolean

    @Insert
    suspend fun addStarRecord(staredPoThreads: StaredPoThreads)



//    @Insert
//    suspend fun starPoThread(savedPoThread: SavedPoThread)
//
//    @Delete
//    suspend fun deletePoThread(savedPoThread: SavedPoThread)
//
//    @Insert
//    suspend fun starAllBasicThread(savedBasicThreads: List<SavedBasicThread>)

}