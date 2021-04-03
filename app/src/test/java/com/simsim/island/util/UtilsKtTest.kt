package com.simsim.island.util

import org.junit.Test

import org.junit.Assert.*
import java.time.Duration
import java.time.LocalDateTime

class UtilsKtTest {

    @Test
    fun test() {
        assertEquals(com.simsim.island.util.handleThreadId("ID: xU1E0dX"),"xU1E0dX")
        println(handleThreadTime(LocalDateTime.of(2021,3,31,0,0)))
        println("回应有 92 篇被省略。要阅读所有回应请按下回应链接。".firstNumberPlus5())
        println("https://adnmb3.com/m/t/36304658?page=37".findPageNumber())
    }
}