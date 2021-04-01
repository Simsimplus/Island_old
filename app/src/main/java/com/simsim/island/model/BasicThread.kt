package com.simsim.island.model

import java.util.*

data class BasicThread(
        var title:String="",
        var name:String="",
        var link:String="",
        var time:Date=Date(),
        var uid:String="",
        var imageUrl:String="",
        var content:String="",
)

data class IslandThread(
        var section:String,
        var poThread:BasicThread,
        var replyThreads:List<BasicThread>,
)
