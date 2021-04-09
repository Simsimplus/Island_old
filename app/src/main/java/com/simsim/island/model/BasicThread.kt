package com.simsim.island.model

import androidx.room.*
import java.time.LocalDateTime
import java.time.ZoneOffset


class Converter {
    @TypeConverter
    fun fromLocalDateTime(time: LocalDateTime): Long = time.toEpochSecond(ZoneOffset.UTC)

    @TypeConverter
    fun toLocalDateTime(time: Long): LocalDateTime {
        return LocalDateTime.ofEpochSecond(time, 0, ZoneOffset.UTC)
    }

}

@Entity(
        foreignKeys = [ForeignKey(
                entity = PoThread::class,
                parentColumns = ["ThreadId"],
                childColumns = ["poThreadId"],
                onDelete = ForeignKey.CASCADE
        )]
)
data class BasicThread(
        @PrimaryKey var replyThreadId: String="",
        var poThreadId: String = "",
        var title: String = "",
        var name: String = "",
        var link: String = "",
        var time: LocalDateTime = LocalDateTime.now(),
        var uid: String = "",
        var imageUrl: String = "",
        var content: String = "",
        var isManager:Boolean=false,
        var isPo: Boolean = false,
        var commentsNumber: String = "0",
        var section: String,
        var references: String = ""
)

@Entity
data class PoThread constructor(
        @PrimaryKey var ThreadId: String="",
        var isManager:Boolean,
        var title: String = "",
        var name: String = "",
        var link: String = "",
        var time: LocalDateTime = LocalDateTime.now(),
        var uid: String = "",
        var imageUrl: String = "",
        var content: String = "",
        var isPo: Boolean = true,
        var isStar:Boolean=false,
        var commentsNumber: String = "0",
        var section: String,
        var references: String = "",
        var collectTime: LocalDateTime = LocalDateTime.now(),


){
        @Ignore
        var replyThreads: List<BasicThread> = listOf()
}

@Entity
data class MainRemoteKey(
        @PrimaryKey var poThreadId: String,
        var previousKey: Int?,
        var nextKey: Int?,
)

@Entity
data class DetailRemoteKey(
        @PrimaryKey var threadId: String,
        var previousKey: Int?,
        var nextKey: Int?,
)
