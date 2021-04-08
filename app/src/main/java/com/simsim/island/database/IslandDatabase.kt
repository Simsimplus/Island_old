package com.simsim.island.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.simsim.island.model.*

@Database(entities = [BasicThread::class,PoThread::class,MainRemoteKey::class,DetailRemoteKey::class],version = 1)
@TypeConverters(Converter::class)
abstract class IslandDatabase: RoomDatabase() {
    abstract fun keyDao():RemoteKeyDao
    abstract fun threadDao():ThreadDao
    companion object{
        @Volatile
        private var INSTANCE:IslandDatabase?=null
        fun newInstance(context: Context):IslandDatabase= INSTANCE?: synchronized(this){
            val instance= Room.databaseBuilder(context.applicationContext,IslandDatabase::class.java,"IslandDatabase").build()
            INSTANCE=instance
            instance
        }
    }
}