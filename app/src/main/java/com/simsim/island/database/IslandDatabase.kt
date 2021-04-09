package com.simsim.island.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.simsim.island.model.*

@Database(entities = [BasicThread::class,PoThread::class,MainRemoteKey::class,DetailRemoteKey::class],version = 3,exportSchema = false)
@TypeConverters(Converter::class)
abstract class IslandDatabase: RoomDatabase() {
    abstract fun keyDao():RemoteKeyDao
    abstract fun threadDao():ThreadDao
    companion object{
        @Volatile
        private var INSTANCE:IslandDatabase?=null
        fun newInstance(context: Context):IslandDatabase= INSTANCE?: synchronized(this){
            Log.e("Simsim",context.toString())
            val instance= Room.databaseBuilder(context,IslandDatabase::class.java,"IslandDatabase").fallbackToDestructiveMigration().build()
            INSTANCE=instance
            instance
        }
    }
}