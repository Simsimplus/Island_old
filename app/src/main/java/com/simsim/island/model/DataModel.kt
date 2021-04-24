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
        var timelineActualSection:String="",
)

@Entity
data class PoThread constructor(
        @PrimaryKey var threadId: Long,
        var pageIndex:Int,
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
        var timelineActualSection:String="",

        ){
        @Ignore
        var replyThreads: List<BasicThread> = listOf()
}

@Entity
data class MainRemoteKey(
        @ColumnInfo(index = true)
        @PrimaryKey
        var poThreadId: Long,
//        @ColumnInfo(index = true)
//        var FKpoThreadId: String,
        var previousKey: Int?,
        var nextKey: Int?,
)

@Entity
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

@Entity(tableName = "sectionList")
data class Section(
        var sectionIndex:Int,
        @ColumnInfo(index = true)
        @PrimaryKey
        var sectionName:String,
        var isShow:Boolean=true,
//        var alias:String="",
        var group:String,
        var sectionUrl:String,
        var fId:String,
)

@Entity
data class Emoji(
        var emojiIndex:Int,
        @ColumnInfo(index = true)
        @PrimaryKey
        var emoji:String,
)

@Entity
data class UpdateRecord(
        @ColumnInfo(index = true)
        @PrimaryKey()
        var index:Int=666,
        var lastUpdateTime:LocalDateTime
)

data class SectionGroup(
        var groupName:String,
        var sectionNameList:List<Section>,
)

data class Cookie(
        val cookie:String,
)
