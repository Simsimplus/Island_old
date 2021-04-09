package com.simsim.island.model

//import androidx.room.Entity
//import androidx.room.ForeignKey
//import androidx.room.Ignore
//import androidx.room.PrimaryKey
//import java.time.LocalDateTime
//
//
//@Entity(
//    foreignKeys = [ForeignKey(
//        entity = SavedPoThread::class,
//        parentColumns = ["ThreadId"],
//        childColumns = ["poThreadId"],
//        onDelete = ForeignKey.CASCADE
//    )]
//)
//data class SavedBasicThread(
//    @PrimaryKey var replyThreadId: String="",
//    var poThreadId: String = "",
//    var title: String = "",
//    var name: String = "",
//    var link: String = "",
//    var time: LocalDateTime = LocalDateTime.now(),
//    var uid: String = "",
//    var imageUrl: String = "",
//    var content: String = "",
//    var isManager:Boolean=false,
//    var isPo: Boolean = false,
//    var commentsNumber: String = "0",
//    var section: String,
//    var references: String = ""
//)
//
//@Entity
//data class SavedPoThread constructor(
//    @PrimaryKey var ThreadId: String="",
//    var isManager:Boolean,
//    var title: String = "",
//    var name: String = "",
//    var link: String = "",
//    var time: LocalDateTime = LocalDateTime.now(),
//    var uid: String = "",
//    var imageUrl: String = "",
//    var content: String = "",
//    var isPo: Boolean = true,
//    var isStar:Boolean=false,
//    var commentsNumber: String = "0",
//    var section: String,
//    var references: String = "",
//    var collectTime: LocalDateTime = LocalDateTime.now(),
//
//
//    ){
//    @Ignore
//    var replyThreads: List<BasicThread> = listOf()
//}