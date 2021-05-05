package com.simsim.island.database

import androidx.paging.PagingSource
import androidx.room.*
import com.simsim.island.model.PoThread
import com.simsim.island.model.ReplyThread
import com.simsim.island.model.SavedPoThread
import com.simsim.island.model.SavedReplyThread
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao  {

    /*
    for basicThread fetching
     */
    @Query("select * from ReplyThread where poThreadId =:poThreadId and replyThreadId !=9999999 order by replyThreadId asc ")
    fun getAllReplyThreadsPagingSource(poThreadId:Long): PagingSource<Int,ReplyThread>

    @Query("select * from ReplyThread where poThreadId =:poThreadId and replyThreadId !=9999999 order by replyThreadId asc ")
    suspend fun getAllReplyThreads(poThreadId:Long):List<ReplyThread>

    @Query("select * from ReplyThread where replyThreadId=:threadId limit 1")
    fun getReplyThread(threadId:Long):ReplyThread

    @Query("select * from ReplyThread where poThreadId =:poThreadId order by replyThreadId desc limit 1")
    suspend fun getLastReplyThread(poThreadId:Long):ReplyThread?

    @Query("select * from ReplyThread where poThreadId =:poThreadId order by replyThreadId asc limit 1")
    suspend fun getFirstReplyThread(poThreadId:Long):ReplyThread?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllReplyThreads(replyThreads:List<ReplyThread>)

    /*
    for poThread fetching
     */

    @Query("select * from PoThread where threadId=:poThreadId limit 1")
    suspend fun getPoThread(poThreadId: Long):PoThread?

    @Query("select * from PoThread where threadId=:poThreadId")
    fun getFlowPoThread(poThreadId: Long):Flow<PoThread?>

    //for fetch remote key
    @Query("select * from poThread where section=:section order by collectTime desc limit 1")
    suspend fun getLastPoThread(section:String):PoThread?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPoThreads(poThreads:List<PoThread>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPoThread(poThread:PoThread)

//    @Query("select exists(select * from SavedPoThread where threadId=:poThreadId)")
//    fun isPoThreadCollected(poThreadId: Long):Boolean

    //check whether there is any poThread collected
    @Query("select exists(select * from PoThread)")
    fun isThereAnyPoThreadInDB():Flow<Boolean>

    @Query("select * from poThread where section=:section and isShow=1 order by pageIndex asc")
    fun getAllPoThreadsBySection(section:String):PagingSource<Int,PoThread>
    @Query("select * from poThread where uid=:uid")
    suspend fun getAllPoThreadsByUid(uid:String):List<PoThread>

    @Update
    suspend fun updatePoThread(poThread: PoThread)



//    @Update
//    suspend fun updatePoThread(poThread: PoThread)

    //clear table when exit app
    @Query("delete from poThread")
    suspend fun clearAllPoThread()

    //ReplyThread will automatically be cleared when responsible poThread is clear
    @Query("delete from ReplyThread where poThreadId=:poThreadId")
    suspend fun clearAllReplyThreads(poThreadId: Long)


    /*
    for thread saving
     */
    // check whether a poThread is Stared and Saved
    @Query("select exists(select * from SavedPoThread where threadId=:poThreadId)")
    fun isPoThreadStaredFlow(poThreadId: Long):Flow<Boolean>

    @Query("select exists(select * from SavedPoThread where threadId=:poThreadId)")
    suspend fun isPoThreadStared(poThreadId: Long):Boolean

    @Insert
    suspend fun insertSavedPoThread(staredPoThreads: SavedPoThread)

    @Delete
    suspend fun deleteSavedPoThread(staredPoThreads: SavedPoThread)

    @Insert
    suspend fun insertAllSavedReplyThread(replyThreads: List<SavedReplyThread>)

    @Query("select * from SavedPoThread order by savedTime desc")
    suspend fun getAllSavedPoThread():List<SavedPoThread>

    @Query("select * from SavedPoThread where threadId=:poThreadId")
    suspend fun getSavedPoThread(poThreadId: Long):SavedPoThread

    @Query("select * from SavedReplyThread where poThreadId =:poThreadId and replyThreadId !=9999999 order by replyThreadId asc ")
    suspend fun getAllSavedReplyThreads(poThreadId:Long):List<SavedReplyThread>

    //count threads
    @Query("select count(*) from SavedReplyThread where poThreadId=:poThreadId ")
    suspend fun countSavedReplyThreads(poThreadId:Long):Int

//    @Insert
//    suspend fun starPoThread(savedPoThread: SavedPoThread)
//
//    @Delete
//    suspend fun deletePoThread(savedPoThread: SavedPoThread)
//
//    @Insert
//    suspend fun starAllBasicThread(savedBasicThreads: List<SavedReplyThread>)

}