package com.simsim.island.repository

import android.util.Log
import com.simsim.island.model.PoThread
import com.simsim.island.model.ReplyThread
import com.simsim.island.model.Section
import com.simsim.island.service.AislandNetworkService
import com.simsim.island.util.LOG_TAG
import com.simsim.island.util.firstNumber
import com.simsim.island.util.islandUrl
import com.simsim.island.util.removeQueryTail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.LocalDateTime
import javax.inject.Inject

class AislandRepoMobile @Inject constructor(private val service: AislandNetworkService) {
    companion object {
        //        private const val baseUrl = "https://adnmb3.com/m/f/%s?page=%d"
        fun responseToThreadList(section: String, response: String?, page: Int): List<PoThread>? {
            val poThreads = mutableListOf<PoThread>()
            if (response != null) {
//                Log.e("Simsim:success", response)
                val doc = Jsoup.parse(response)
                val divs = doc.select("div[class=uk-container h-threads-container]")
                val fId = doc.selectFirst("input[name=fid]")?.attr("value")
                    ?: throw IllegalArgumentException("can not find fid")
                divs.forEachIndexed { index, poDiv ->
                    val poBasicThread = divToBasicThread(
                        div = poDiv,
                        isPo = true,
                        section = section,
                        poThreadId = 0,
                        fId = fId
                    )
                    val replyDivs =
                        poDiv.select("div[class=uk-container h-threads-reply-container]")
                    val replyThreads = mutableListOf<ReplyThread>()
                    replyDivs.forEach { replyDiv ->
                        replyThreads.add(
                            divToBasicThread(
                                div = replyDiv,
                                isPo = false,
                                section = section,
                                poThreadId = poBasicThread.replyThreadId,
                                fId = fId,
                                poUid = poBasicThread.uid
                            )
                        )
                    }
                    var commentsNumber = ((poDiv.select("font").find { font ->
                        "?????????\\s*(\\d+)\\s*????????????.*".toRegex().matches(font.ownText())
                    }?.ownText()?.firstNumber() ?: 0)+replyThreads.size).toString()
                    if (commentsNumber.length >= 4) commentsNumber = "1k+"
                    poBasicThread.commentsNumber = commentsNumber
                    val poThread =
                        basicThreadToPoThread(poBasicThread, replyThreads, 20 * page + index)
                    poThreads.add(poThread)

//                    Log.e("Simsim", poThread.toString())
                }
                return poThreads

            } else {
                return null
            }
        }

        fun divToBasicThread(
            div: Element,
            isPo: Boolean,
            section: String,
            poThreadId: Long,
            fId: String,
            poUid: String = "ahsgjkdghahweuihuihafuidsbfiaubf"
        ): ReplyThread {
            val basicThread = ReplyThread(
                section = section,
                isPo = isPo,
                poThreadId = poThreadId,
                replyThreadId = 0
            )
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
                                            child.ownText().replace("\\D".toRegex(), "").toLong()
                                    }
                                }
                            }
                        }
                        "h-threads-second-col" -> {
                            p.children().forEach { child ->
                                when (child.className()) {
                                    //2021-03-17(???)16:06:23
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
                                        val isManager = child.selectFirst("font[color=red]") != null
                                        basicThread.isManager = isManager
                                        basicThread.uid = child.text()
                                        if (basicThread.uid == poUid) {
                                            basicThread.isPo = true
                                        }

                                    }
                                    "h-threads-info-uid" -> {
                                        val isManager = child.selectFirst("font[color=red]") != null
                                        basicThread.isManager = isManager
                                        basicThread.uid = child.text()
                                        if (basicThread.uid == poUid) {
                                            basicThread.isPo = true
                                        }

                                    }
                                }
                            }
                            if (p.text().contains("PO???")) {
                                basicThread.isPo = true
                            }
                        }
                        "h-threads-content" -> {
//                            val referenceTags = try {
//                                p.select("font[color=#789922]")
//                            } catch (e: Exception) {
//                                Log.e("Simsim", e.stackTraceToString())
//                                null
//                            }
//                            val references = mutableListOf<String>()
//                            if (!referenceTags.isNullOrEmpty()) {
//                                referenceTags.forEach { reference ->
//                                    references.add(reference.ownText())
//                                }
//                            }
//                            basicThread.references = references.joinToString(
//                                referenceStringSpliterator
//                            )
                            basicThread.content = p.ownText()
                        }
                    }
                }

            }
//        Log.e("Simsim", "poUid:$poUid  $basicThread")
            return basicThread
        }

        fun basicThreadToPoThread(
            replyThread: ReplyThread,
            replyThreads: List<ReplyThread>,
            pageIndex: Int
        ): PoThread {
            return PoThread(
                isManager = replyThread.isManager,
                threadId = replyThread.replyThreadId,
                title = replyThread.title,
                name = replyThread.name,
                link = replyThread.link,
                time = replyThread.time,
                uid = replyThread.uid,
                imageUrl = replyThread.imageUrl,
                content = replyThread.content,
                isPo = replyThread.isPo,
                commentsNumber = replyThread.commentsNumber,
                section = replyThread.section,
                pageIndex = pageIndex,
            ).apply {
                this.replyThreads = replyThreads
            }
        }
    }

    suspend fun getSectionList(): Flow<Section> {
        val sectionList = mutableListOf<Section>()
        return try {
            val response = service.getHtmlStringByPage("https://adnmb3.com/Forum")
            response?.let { rp ->
                val doc = Jsoup.parse(rp)
                val sectionGroups = doc.select("li[class~=uk-parent.*]")
                var index = 0
                sectionGroups.forEach { sectionGroup ->
                    val groupName =
                        sectionGroup.selectFirst("a[class=h-nav-parent-header]")?.ownText()
                            ?: "(???????????????????)"
                    val listTags = sectionGroup.select("li")
                    listTags.forEach { li ->
                        val section = li.selectFirst("a[href~=[/f]{3,}.*|[/]Forum[/]timeline.*]")
                        if (section != null) {
                            val sectionUrl = islandUrl + section.attr("href")
                            val sectionResponse = service.getHtmlStringByPage(sectionUrl)
                            val fId = sectionResponse?.let { r ->
                                val sectionDoc = Jsoup.parse(r)
                                sectionDoc.selectFirst("input[name=fid]")?.attr("value") ?: ""
                            } ?: ""
                            sectionList.add(
                                Section(
                                    sectionIndex = index++,
                                    sectionName = section.ownText(),
                                    group = groupName,
                                    sectionUrl = sectionUrl,
                                    fId = fId,
                                )
                            )
                        }

                    }
                }


            }
            sectionList.asFlow()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "getSectionList ${e.stackTraceToString()}")
            emptyFlow()
        }

    }
}