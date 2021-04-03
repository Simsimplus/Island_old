package com.simsim.island.repository

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.simsim.island.model.BasicThread
import com.simsim.island.model.IslandThread
import com.simsim.island.service.AislandNetworkService
import com.simsim.island.util.firstNumberPlus5
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.lang.Exception
import java.time.LocalDateTime
import javax.inject.Inject

class AislandRepo @Inject constructor(private val service: AislandNetworkService) {
    companion object {
        private const val baseUrl = "https://adnmb3.com/m/f/%s?page=%d"
        fun responseToThreadList(section: String,response: String?): List<IslandThread>? {
            val threads = mutableListOf<IslandThread>()
            if (response != null) {
//                Log.e("Simsim:success", response)
                val doc = Jsoup.parse(response)
                val divs = doc.select("div[class=uk-container h-threads-container]")

                divs.forEach { poDiv ->

                    val poThread = divToBasicThread(poDiv)
                    val replyDivs = poDiv.select("div[class=uk-container h-threads-reply-container]")
                    val replyThreads = mutableListOf<BasicThread>()
                    replyDivs.forEach { replyDiv ->
                        replyThreads.add(divToBasicThread(replyDiv))
                    }
                    var commentsNumber=poDiv.select("font").find { font->
                        "回应有\\s*(\\d+)\\s*篇被省略.*".toRegex().matches(font.ownText())
                    }?.ownText()?.firstNumberPlus5()?:replyThreads.size.toString()
                    if (commentsNumber.length>=4) commentsNumber="999+"
                    val islandThread=IslandThread(commentsNumber,Uri.decode(section), poThread, replyThreads,)
                    threads.add(islandThread)
                    Log.e("Simsim", islandThread.toString())
                }
                return threads

            } else {
                return null
            }
        }

        fun divToBasicThread(div: Element): BasicThread {
            val basicThread = BasicThread()
            val pTags = div.select("p")
            val divClassName=div.className()
            pTags.forEach { p ->
                if(p.parent().className()==divClassName){
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
                                        basicThread.ThreadId=child.ownText()
                                    }
                                }
                            }
                        }
                        "h-threads-second-col" -> {
                            p.children().forEach { child ->
                                when (child.className()) {
                                    //2021-03-17(三)16:06:23
                                    "h-threads-time" -> {
//                                        basicThread.time = SimpleDateFormat(
//                                            "yyyy-MM-dd-HH:mm:ss",
//                                            Locale.getDefault()
//                                        ).parse(
//                                            child.ownText().replace("\\(.\\)".toRegex(), "-")
//                                        ) ?: Date()
//                                Log.e("Simsim","time replaced as:${child.ownText().replace("\\(.\\)".toRegex(), "-")}")
//                                Log.e("Simsim","time parsed as:${basicThread.time}")
                                        basicThread.time= try {
                                            LocalDateTime.parse(child.ownText().replace("\\(.\\)|(?=\\d)\\s".toRegex(),"T"))
                                        }catch (e:Exception){
                                            LocalDateTime.of(2099,1,1,0,1)
                                        }
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

            }
//        Log.e("Simsim",basicThread.toString())
            return basicThread
        }
    }

    internal val threadLiveData = MutableLiveData<List<IslandThread>?>()
//    suspend fun getThreadsByPage(section: String, page: Int) {
//        val response: String? = service.getHtmlStringByPage(baseUrl.format(section, page))
//        threadLiveData.value=responseToThreadList(section,response)
//    }


}