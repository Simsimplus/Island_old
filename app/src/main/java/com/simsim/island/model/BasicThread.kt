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
                parentColumns = ["threadId"],
                childColumns = ["poThreadId"],
                onDelete = ForeignKey.CASCADE
        )]
)
data class BasicThread(
        @ColumnInfo(index = true)
        @PrimaryKey var replyThreadId: Long,
        @ColumnInfo(index = true)
        var poThreadId: Long,
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
        var references: String = "",
        var fId:String,
)

@Entity
data class PoThread constructor(
        @PrimaryKey var threadId: Long,
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
        var fId:String

        ){
        @Ignore
        var replyThreads: List<BasicThread> = listOf()
}

@Entity(
//        foreignKeys = [ForeignKey(
//                entity = PoThread::class,
//                parentColumns = ["threadId"],
//                childColumns = ["FKpoThreadId"],
//                onDelete = ForeignKey.CASCADE
//        )]
)
data class MainRemoteKey(
        @ColumnInfo(index = true)
        @PrimaryKey
        var poThreadId: Long,
//        @ColumnInfo(index = true)
//        var FKpoThreadId: String,
        var previousKey: Int?,
        var nextKey: Int?,
)

@Entity(
//        foreignKeys = [ForeignKey(
//                entity = BasicThread::class,
//                parentColumns = ["replyThreadId"],
//                childColumns = ["FkthreadId"],
//                onDelete = ForeignKey.CASCADE
//        )]
)
data class DetailRemoteKey(
        @ColumnInfo(index = true)
        @PrimaryKey
        var threadId: Long,
//        @ColumnInfo(index = true)
//        var FkthreadId: String,
        var previousKey: Int?,
        var nextKey: Int?,
)

@Entity
data class StaredPoThreads(
        @ColumnInfo(index = true)
        @PrimaryKey
        var poThreadId: Long
)
