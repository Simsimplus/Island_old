package com.simsim.island.model

import java.time.LocalDateTime
import java.util.*

data class BasicThread(
        var ThreadId:String="",
        var title:String="",
        var name:String="",
        var link:String="",
        var time:LocalDateTime= LocalDateTime.now(),
        var uid:String="",
        var imageUrl:String="",
        var content:String="",
)

data class IslandThread(
        var commentsNumber:String="0",
        var section:String,
        var poThread:BasicThread,
        var replyThreads:List<BasicThread>,

)
