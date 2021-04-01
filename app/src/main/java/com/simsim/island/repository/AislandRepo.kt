package com.simsim.island.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.simsim.island.model.BasicThread
import com.simsim.island.model.IslandThread
import com.simsim.island.service.AislandRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class AislandRepo @Inject constructor(private val service: AislandRequest) {
    companion object {
        private const val baseUrl = "https://adnmb3.com/m/f/%s?page=%d"
    }

    internal val threadLiveData = MutableLiveData<List<IslandThread>?>()
    suspend fun getThreadsByPage(section: String, page: Int) {
        val threads = mutableListOf<IslandThread>()
        //            val call = service.service.getHtmlStringByPage(baseUrl.format(section, page))
//            Log.e("Simsim:url:", baseUrl.format(section, page))
//            if (call.isSuccessful) {
//                response = call.body()
//                Log.e("Simsim:response body:", response.toString())
//            } else {
//                Log.e("Simsim:error:", call.code().toString())
//            }
        val response: String? = service.getHtmlStringByPage(baseUrl.format(section, page))
        if (response != null) {
            Log.e("Simsim:success", response)
            val doc = Jsoup.parse(response)
            val divs = doc.select("div[class=uk-container h-threads-container]")
            divs.forEach { element ->
                val poThread = divToBasicThread(element)
                val replyDivs = element.select("div[class=uk-container h-threads-reply-container]")
                val replyThreads = mutableListOf<BasicThread>()
                replyDivs.forEach { replyDiv ->
                    replyThreads.add(divToBasicThread(replyDiv))
                }
                threads.add(IslandThread(section, poThread, replyThreads))
                Log.e("Simsim", IslandThread(section, poThread, replyThreads).toString())
            }
            threadLiveData.value = threads

        } else {
            threadLiveData.value = null
        }


    }

    private fun divToBasicThread(div: Element): BasicThread {
        val basicThread = BasicThread()
        val pTags = div.select("p")
        pTags.forEach { p ->
            when (p.className()) {
                "h-threads-first-col" -> {
                    p.children().forEach { child ->
                        when (child.className()) {
                            "h-threads-title" -> {
                                basicThread.title = child.ownText()
                            }
                            "h-threads-name" -> {
                                basicThread.name = child.ownText()
                            }
                            "h-threads-id" -> {
                                basicThread.link = child.attr("href")
                            }
                        }
                    }
                }
                "h-threads-second-col" -> {
                    p.children().forEach { child ->
                        when (child.className()) {
                            //2021-03-17(三)16:06:23
                            "h-threads-time" -> {
                                basicThread.time = SimpleDateFormat(
                                    "yyyy-MM-dd-HH:mm:ss",
                                    Locale.getDefault()
                                ).parse(
                                    child.ownText().replace("\\(.\\)".toRegex(), "-")
                                ) ?: Date()
//                                Log.e("Simsim","time replaced as:${child.ownText().replace("\\(.\\)".toRegex(), "-")}")
//                                Log.e("Simsim","time parsed as:${basicThread.time}")
                            }
                            "h-threads-uid" -> {
                                basicThread.uid = child.ownText()
                            }
                        }
                    }
                    //check whether there is image
                    val img = p.selectFirst("img")
                    if (img !== null) {
                        basicThread.imageUrl = img.attr("src")
                    }
                }
                "h-threads-content" -> {
                    basicThread.content = p.ownText()
                }
            }
        }
//        Log.e("Simsim",basicThread.toString())
        return basicThread
    }
}