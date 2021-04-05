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
            minutes in 0..59 ->{
            return "${duration.toMinutes()}分前"
        }
        minutes in 60 until 60*24->{
            return "${duration.toHours()}时前"
        }
        minutes in 60*24 until 60*24*2->{
            return "昨天"
        }
        minutes in 60*24*2 until 60*24*3->{
            return "前天"
        }
        minutes in 60*24*3 until 60*24*4->{
            return "大前天"
        }
        minutes <0->{
            return "未来"
        }
        else ->{
            return "%d-%d-%d".format(time.year,time.month.value,time.dayOfMonth)
        }
    }
}
fun String.firstNumberPlus5():String=
    ((".*?(\\d+).*?".toRegex().matchEntire(this)?.groupValues?.get(1)?.toInt()?:0)+5).toString()

fun String.findPageNumber():String=".*page=(\\d+).*".toRegex().matchEntire(this)?.groupValues?.get(1)?:"99"

fun String.removeQueryTail():String=this.replace("\\?r=\\d+".toRegex(),"")
