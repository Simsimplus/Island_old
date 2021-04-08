package com.simsim.island.model

import androidx.room.*
import java.time.LocalDateTime
import java.time.ZoneOffset


class Converter {
    @TypeConverter
    fun fromLocalDateTime(time: LocalDateTime): Long = time.toEpochSecond(ZoneOffset.UTC)

    @TypeConverter
    fun toLocalDateTime(time: Long): LocalDateTime {
        return LocalDateTime.ofEpochSecond(time,0,ZoneOffset.UTC)
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
        @PrimaryKey var ThreadId: String = "",
        var poThreadId: String = "",
        var title: String = "",
        var name: String = "",
        var link: String = "",
        var time: LocalDateTime = LocalDateTime.now(),
        var uid: String = "",
        var imageUrl: String = "",
        var content: String = "",
        var isPo: Boolean = false,
        var commentsNumber: String = "0",
        var section: String,
        var references: String = ""
)

@Entity
data class PoThread(
        @PrimaryKey var ThreadId: String = "",
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
        var references: String = "",
        var collectTime: LocalDateTime = LocalDateTime.now(),
        @Ignore
        var replyThreads: List<BasicThread>
) {
    fun toBasicThread(): BasicThread = BasicThread(
            ThreadId = this.ThreadId,
            poThreadId = this.ThreadId,
            title = this.title,
            name = this.name,
            link = this.link,
            time = this.time,
            uid = this.uid,
            imageUrl = this.imageUrl,
            content = this.content,
            isPo = this.isPo,
            commentsNumber = this.commentsNumber,
            section = this.section,
            references = this.references,
    )
}
@Entity
data class RemoteKeysForPoThread(
        @PrimaryKey var poThreadId: String,
        var previousKey:Int?,
        var nextKey:Int?,
)
