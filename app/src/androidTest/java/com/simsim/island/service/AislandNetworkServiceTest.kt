package com.simsim.island.service

import com.simsim.island.model.Cookie
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

class AislandNetworkServiceTest {
    private lateinit var service: AislandNetworkService
    @Before
    fun setUp() {
        service=AislandNetworkService()
    }

    @Test
    fun test(){
        GlobalScope.launch {
            val results=service.getCookies(mapOf(
                "PHPSESSID" to "os9deilv73e82761tmeuigg142",
                "memberUserspapapa" to "%17%3D%F4%BA%A0%16e9%FD%F2%B0%D7%9A%92%F1%24%A7%10O%19S%CD%25%E0%27R%99%B0%FA%7D%14%8A%E3%18%FD%8C%15%88%DF%F7%AE%8Ek%A4G%09%F7%A9%1A%F4%26%D4ZV%1E%DA0%60%C0%A6%1F%ED%CE%E1"
            ))
            println(results)
        }
    }
}