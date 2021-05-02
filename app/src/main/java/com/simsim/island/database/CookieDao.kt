package com.simsim.island.database

import androidx.room.*
import com.simsim.island.model.Cookie
import kotlinx.coroutines.flow.Flow

@Dao
interface CookieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCookies(cookies:List<Cookie>)

    @Query("select * from Cookie")
    suspend fun getAllCookies(): List<Cookie>

    @Query("select exists(select * from Cookie)")
    fun isAnyCookieAvailable(): Flow<Boolean>

    @Query("select * from Cookie where isInUse=1")
    fun getActiveCookieFlow():Flow<Cookie?>

    @Query("select * from Cookie where cookie=:cookieValue")
    suspend fun getCookieByValue(cookieValue:String):Cookie?

    @Query("delete from Cookie where cookie=:cookieValue")
    suspend fun deleteCookie(cookieValue: String)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCookies(cookies:List<Cookie>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCookie(cookie:Cookie)
}