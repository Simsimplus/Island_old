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

    @TypeConverter
    fun toTarget(name: String): BlockTarget {
        return BlockTarget.valueOf(name)
    }

    @TypeConverter
    fun fromTarget(target: BlockTarget): String {
        return target.name
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
data class ReplyThread(
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
        var isManager: Boolean = false,
        var isPo: Boolean = false,
        var commentsNumber: String = "0",
        var section: String,
        var timelineActualSection: String = "",
)

@Entity
data class PoThread constructor(
        @PrimaryKey var threadId: Long,
        var pageIndex: Int,
        var isManager: Boolean,
        var title: String = "",
        var name: String = "",
        var link: String = "",
        var time: LocalDateTime = LocalDateTime.now(),
        var uid: String = "",
        var imageUrl: String = "",
        var content: String = "",
        var isPo: Boolean = true,
        var commentsNumber: String = "0",
        var section: String,
        var collectTime: LocalDateTime = LocalDateTime.now(),
        var timelineActualSection: String = "",
        var isShow: Boolean=true
        ) {
    @Ignore
    var replyThreads: List<ReplyThread> = listOf()
}

@Entity(
        foreignKeys = [ForeignKey(
                entity = SavedPoThread::class,
                parentColumns = ["threadId"],
                childColumns = ["poThreadId"],
                onDelete = ForeignKey.CASCADE
        )]
)
data class SavedReplyThread(
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
        var isManager: Boolean = false,
        var isPo: Boolean = false,
        var commentsNumber: String = "0",
        var section: String,
        var timelineActualSection: String = "",
)

@Entity
data class SavedPoThread constructor(
        @PrimaryKey var threadId: Long,
        var pageIndex: Int,
        var isManager: Boolean,
        var title: String = "",
        var name: String = "",
        var link: String = "",
        var time: LocalDateTime = LocalDateTime.now(),
        var uid: String = "",
        var imageUrl: String = "",
        var content: String = "",
        var isPo: Boolean = true,
        var commentsNumber: String = "0",
        var section: String,
        var collectTime: LocalDateTime = LocalDateTime.now(),
        var timelineActualSection: String = "",
        var savedTime:LocalDateTime=LocalDateTime.now()
) {
        @Ignore
        var replyThreads: List<ReplyThread> = listOf()
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

//@Entity
//data class StaredPoThreads(
//        @ColumnInfo(index = true)
//        @PrimaryKey
//        var poThreadId: Long
//)

@Entity(tableName = "sectionList")
data class Section(
        var sectionIndex: Int,
        @ColumnInfo(index = true)
        @PrimaryKey
        var sectionName: String,
        var isShow: Boolean = true,
//        var alias:String="",
        var group: String,
        var sectionUrl: String,
        var fId: String,
)

@Entity
data class Emoji(
        var emojiIndex: Int,
        @ColumnInfo(index = true)
        @PrimaryKey
        var emoji: String,
)

@Entity
data class UpdateRecord(
        @ColumnInfo(index = true)
        @PrimaryKey()
        var index: Int = 666,
        var lastUpdateTime: LocalDateTime
)

data class SectionGroup(
        var groupName: String,
        var sectionNameList: List<Section>,
)

@Entity
data class Cookie(
        @ColumnInfo(index = true)
        @PrimaryKey()
        var cookie: String,
        var name: String = "",
        var isInUse: Boolean = false
)

@Entity
data class BlockRule(
        @ColumnInfo(index = true)
        @PrimaryKey(autoGenerate = true)
        var index: Long=0,
        var rule: String,
        var name: String = rule,
        var isRegex: Boolean = false,
        var isEnable: Boolean = true,
        var isNotCaseSensitive: Boolean = false,
        var matchEntire:Boolean=true,
        var target: BlockTarget=BlockTarget.TargetContent
)
