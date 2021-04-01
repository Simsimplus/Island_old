package com.simsim.island.util

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun handleThreadId(id:String): String {
    val regex="ID:([\\s0-9a-zA-Z]+)".toRegex()
    return regex.matchEntire(id)?.groupValues?.get(1)?.trim() ?:""
}

fun handleThreadTime(time:LocalDateTime):String{
    val now= LocalDateTime.now()
    val duration=Duration.between(time,now)
    val minutes=duration.toMinutes()
    when{
        minutes<60 ->{
            return "${duration.toMinutes()}分"
        }
        minutes>=60 && minutes<60*24->{
            return "${duration.toHours()}时"
        }
        minutes>=60*24 && minutes<60*24*30->{
            return "${duration.toDays()}天"
        }
        minutes>=60*24*30 && minutes<60*24*30*12->{
            return "${duration.toDays()/30}月"
        }
        minutes>60*24*30*12->{
            return "${duration.toDays()/(30*12)}年"
        }
        else ->{
            return "未知"
        }
    }
}
fun String.firstNumberPlus5():String=
    ((".*?(\\d+).*?".toRegex().matchEntire(this)?.groupValues?.get(1)?.toInt()?:0)+5).toString()
