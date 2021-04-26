package com.simsim.island.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simsim.island.model.Cookie
import kotlinx.coroutines.flow.Flow

@Dao
interface CookieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCookies(cookies:List<Cookie>)

    @Query("select * from Cookie")
    suspend fun getAllCookies(): List<Cookie>

    @Query("select * from Cookie where cookie=:cookieValue")
    suspend fun getCookieByValue(cookieValue:String):Cookie?
}