package com.simsim.island.repository

import android.util.Log
import com.simsim.island.model.BasicThread
import com.simsim.island.model.PoThread
import com.simsim.island.service.AislandNetworkService
import com.simsim.island.util.LOG_TAG
import com.simsim.island.util.firstNumberPlus5
import com.simsim.island.util.referenceStringSpliterator
import com.simsim.island.util.removeQueryTail
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.LocalDateTime
import javax.inject.Inject

class AislandRepo @Inject constructor(private val service: AislandNetworkService) {
    companion object {
        //        private const val baseUrl = "https://adnmb3.com/m/f/%s?page=%d"
//        private const val islandUrl="https://adnmb3.com"
        fun responseToThreadList(section: String, response: String?): List<PoThread>? {
            val poThreads = mutableListOf<PoThread>()
            if (response != null) {
//                Log.e("Simsim:success", response)
                val doc = Jsoup.parse(response)
                val divs = doc.select("div[class=uk-container h-threads-container]")

                divs.forEach { poDiv ->
                    val poBasicThread = divToBasicThread(div = poDiv, isPo = true, section = section,poThreadId = "")
                    val replyDivs =
                        poDiv.select("div[class=uk-container h-threads-reply-container]")
                    val replyThreads = mutableListOf<BasicThread>()
                    replyDivs.forEach { replyDiv ->
                        replyThreads.add(
                            divToBasicThread(
                                div = replyDiv,
                                isPo = false,
                                section = section,
                                poThreadId = poBasicThread.replyThreadId
                            )
                        )
                    }
                    var commentsNumber = poDiv.select("font").find { font ->
                        "回应有\\s*(\\d+)\\s*篇被省略.*".toRegex().matches(font.ownText())
                    }?.ownText()?.firstNumberPlus5() ?: replyThreads.size.toString()
                    if (commentsNumber.length >= 4) commentsNumber = "999+"
                    poBasicThread.commentsNumber = commentsNumber
                    val poThread= basicThreadToPoThread(poBasicThread,replyThreads)
                    poThreads.add(poThread)

//                    Log.e("Simsim", poThread.toString())
                }
                return poThreads

            } else {
                return null
            }
        }

        fun divToBasicThread(div: Element, isPo: Boolean, section: String,poThreadId:String): BasicThread {
            val basicThread = BasicThread(section = section, isPo = isPo,poThreadId =poThreadId)
            val pTags = div.select("p")
            val divClassName = div.className()
            val img = div.selectFirst("img")
            if (img != null) {
                basicThread.imageUrl = img.attr("src")
            }
            pTags.forEach { p ->
                if (p.parent().className() == divClassName) {
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
                                        basicThread.link = child.attr("href").removeQueryTail()
                                        basicThread.replyThreadId =
                                            child.ownText().replace("\\D".toRegex(), "")
                                    }
                                }
                            }
                        }
                        "h-threads-second-col" -> {
                            p.children().forEach { child ->
                                when (child.className()) {
                                    //2021-03-17(三)16:06:23
                                    "h-threads-time" -> {
                                        basicThread.time = try {
                                            LocalDateTime.parse(
                                                child.ownText()
                                                    .replace("\\(.\\)|(?=\\d)\\s".toRegex(), "T")
                                            )
                                        } catch (e: Exception) {
                                            LocalDateTime.of(2099, 1, 1, 0, 1)
                                        }
                                    }
                                    "h-threads-uid" -> {
                                        val isManager= child.selectFirst("font[color=red]")!=null
                                        basicThread.isManager=isManager
                                        basicThread.uid = child.text()

                                    }
                                }
                            }
                        }
                        "h-threads-content" -> {
                            val referenceTags = try {
                                p.select("font")
                            } catch (e: Exception) {
                                Log.e("Simsim", e.stackTraceToString())
                                null
                            }
                            val references = mutableListOf<String>()
                            if (!referenceTags.isNullOrEmpty()) {
                                referenceTags.forEach { reference ->
                                    references.add(reference.ownText())
                                }
                            }
                            basicThread.references=references.joinToString(
                                referenceStringSpliterator)
                            basicThread.content = p.ownText()
                        }
                    }
                }

            }
//        Log.e("Simsim",basicThread.toString())
            return basicThread
        }
        private fun basicThreadToPoThread(basicThread: BasicThread, replyThreads:List<BasicThread>):PoThread{
            return PoThread(
                isManager = basicThread.isManager,
                ThreadId=basicThread.replyThreadId,
                title=basicThread.title,
            name=basicThread.name,
            link=basicThread.link,
            time=basicThread.time,
            uid=basicThread.uid,
            imageUrl=basicThread.imageUrl,
           content=basicThread.content,
            isPo=basicThread.isPo,
            commentsNumber=basicThread.commentsNumber,
            section=basicThread.section,
            references=basicThread.references,
            ).apply {
                this.replyThreads=replyThreads
            }
        }
    }

//    internal val threadLiveData = MutableLiveData<List<IslandThread>?>()
//    suspend fun getThreadsByPage(section: String, page: Int) {
//        val response: String? = service.getHtmlStringByPage(baseUrl.format(section, page))
//        threadLiveData.value=responseToThreadList(section,response)
//    }


}