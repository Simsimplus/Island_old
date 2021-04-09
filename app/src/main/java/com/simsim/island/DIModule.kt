package com.simsim.island

import android.content.Context
import androidx.room.Room
import com.simsim.island.database.IslandDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule{

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context):IslandDatabase{
        return Room.databaseBuilder(context,IslandDatabase::class.java,"IslandDatabase").fallbackToDestructiveMigration().build()
    }
}