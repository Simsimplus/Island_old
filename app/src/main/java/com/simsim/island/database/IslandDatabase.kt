package com.simsim.island.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.simsim.island.model.BasicThread
import com.simsim.island.model.Converter
import com.simsim.island.model.PoThread

@Database(entities = [BasicThread::class,PoThread::class],version = 1)
@TypeConverters(Converter::class)
abstract class IslandDatabase: RoomDatabase() {
    abstract fun dao():ThreadDao
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