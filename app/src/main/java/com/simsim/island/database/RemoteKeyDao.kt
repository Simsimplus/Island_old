package com.simsim.island.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simsim.island.model.DetailRemoteKey
import com.simsim.island.model.MainRemoteKey

@Dao
interface RemoteKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMainKeys(keysMain:List<MainRemoteKey>)

    @Query("select * from MainRemoteKey where poThreadId=:poThreadId")
    suspend fun getMainKey(poThreadId:Long):MainRemoteKey

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetailKeys(keysDetail:List<DetailRemoteKey>)

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertDetailKey(keysDetail:DetailRemoteKey)

    @Query("select * from DetailRemoteKey where threadId=:threadId")
    suspend fun getDetailKey(threadId:Long):DetailRemoteKey?

    @Query("delete from MainRemoteKey")
    suspend fun clearMainKeys()

    @Query("delete from DetailRemoteKey")
    suspend fun clearReplyThreadsKeys()
}