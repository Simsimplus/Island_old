package com.simsim.island.util

import org.junit.Test

import org.junit.Assert.*
import java.time.LocalDateTime

class UtilsKtTest {

    @Test
    fun test() {
        assertEquals(com.simsim.island.util.handleThreadId("ID: xU1E0dX"),"xU1E0dX")
        println(handleThreadTime(LocalDateTime.of(2021,3,31,0,0)))
        println("回应有 92 篇被省略。要阅读所有回应请按下回应链接。".firstNumber())
        println("https://adnmb3.com/m/t/36304658?page=37".findPageNumber())
        println("https://adnmb3.com/m/t/36458060?r=36458142".removeQueryTail())
        println("""{"cookie":"%D8%A9%AE%99%1BKc%BC%16iDt%94%7B%DDm%86%15%81%AA%8Ct%3E%BB"}""".extractCookie())
    }
}