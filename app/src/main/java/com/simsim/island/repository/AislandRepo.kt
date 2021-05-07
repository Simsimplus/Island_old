package com.simsim.island.repository

import android.util.Log
import com.simsim.island.database.IslandDatabase
import com.simsim.island.model.Emoji
import com.simsim.island.model.PoThread
import com.simsim.island.model.ReplyThread
import com.simsim.island.model.Section
import com.simsim.island.service.AislandNetworkService
import com.simsim.island.util.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import javax.inject.Inject

class AislandRepo @Inject constructor(
    private val service: AislandNetworkService,
    private val database: IslandDatabase
) {
    companion object {
        //        private const val baseUrl = "https://adnmb3.com/m/f/%s?page=%d"
//        const val islandUrl="https://adnmb3.com"
        fun responseToThreadList(section: String, response: String?, page: Int): List<PoThread>? {
            val poThreads = mutableListOf<PoThread>()
            if (response != null) {
//                Log.e("Simsim:success", response)
                val doc = Jsoup.parse(response)
                val divs = doc.select("div[class=h-threads-item uk-clearfix]")
//                val fId =doc.selectFirst("input[name=fid]")?.attr("value")?:null
                divs.forEachIndexed { index, mainDiv ->
                    val poDiv = mainDiv.selectFirst("div[class=h-threads-item-main]")
                        ?: throw IllegalArgumentException("can parse po thread")
                    val poBasicThread = divToBasicThread(
                        div = poDiv,
                        isPo = true,
                        section = section,
                        poThreadId = 0
                    )
                    val replyDivs =
                        poDiv.select("div[class=h-threads-item-reply-main]")
                    val replyThreads = mutableListOf<ReplyThread>()
                    replyDivs.forEach { replyDiv ->
                        replyThreads.add(
                            divToBasicThread(
                                div = replyDiv,
                                isPo = false,
                                section = section,
                                poThreadId = poBasicThread.replyThreadId,
                                poUid = poBasicThread.uid
                            )
                        )
                    }
                    val commentNumberDiv = mainDiv.selectFirst("div[class=h-threads-tips]")
                    var commentsNumber = ((commentNumberDiv?.text()?.firstNumber()
                        ?: 0) + replyThreads.size).toString()
//                    var commentsNumber = doc.select("font").find { font ->
//                        "回应有\\s*(\\d+)\\s*篇被省略.*".toRegex().matches(font.ownText())
//                    }?.ownText()?.firstNumberPlus5() ?:
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
            poUid: String = "ahsgjkdghahweuihuihafuidsbfiaubf"
        ): ReplyThread {
            val basicThread = ReplyThread(
                section = section,
                isPo = isPo,
                poThreadId = poThreadId,
                replyThreadId = 0
            )
            val divTags = div.select("div")
            val divClassName = div.className()
            val img = div.selectFirst("img")
            if (img != null) {
                basicThread.imageUrl = img.attr("src")
            }
            divTags.forEach { divTag ->
                if (divTag.parent().className() == divClassName) {
                    when (divTag.className()) {
                        "h-threads-info" -> {
                            val actualSection = divTag.selectFirst("spam[style=color:gray]")
                            actualSection?.let {
                                basicThread.timelineActualSection = actualSection.ownText()
                            }
                            divTag.children().forEach { child ->
                                when (child.className()) {
                                    "h-threads-info-title" -> {
                                        basicThread.title = child.ownText()
                                    }
                                    "h-threads-info-email" -> {
                                        basicThread.name = child.ownText()
                                    }
                                    "h-threads-info-id" -> {
                                        basicThread.link = child.attr("href").removeQueryTail()
                                        basicThread.replyThreadId =
                                            child.ownText().replace("\\D".toRegex(), "").toLong()
                                    }
                                    "h-threads-info-createdat" -> {
                                        basicThread.time = parseIslandTime(child.ownText())
                                    }
                                    "h-threads-info-uid" -> {
                                        val isManager = child.selectFirst("font[color=red]") != null
                                        basicThread.isManager = isManager
                                        basicThread.uid = if (isManager) {
                                            child.selectFirst("font[color=red]").ownText()
                                        } else {
                                            child.ownText().replace("ID:", "")
                                        }

                                        if (basicThread.uid == poUid) {
                                            basicThread.isPo = true
                                        }
                                        if (divTag.text().contains("PO主")) {
                                            basicThread.isPo = true
                                        }

                                    }
                                    "h-threads-info-report-btn" -> {
                                        child.selectFirst("a[class=h-threads-info-id]")
                                            ?.also { grandChild ->
                                                basicThread.replyThreadId =
                                                    grandChild.ownText()
                                                        .replace("\\D".toRegex(), "").toLong()
                                            }
                                    }
                                }
                            }
                        }
                        "h-threads-content" -> {
//                            val referenceTags = try {
//                                divTag.select("font[color=#789922]")
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
//                            basicThread.references=references.joinToString(
//                                referenceStringSpliterator)
                            val contentOrigin = divTag.wholeText()
                            basicThread.content = divTag.wholeText().trim()
//                                .replace("<br>","\n")
//                                .replace(">>No\\.\\d+\\s?".toRegex(),"")
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
                timelineActualSection = replyThread.timelineActualSection
            ).apply {
                this.replyThreads = replyThreads
            }
        }
    }

    suspend fun getSectionAndEmojiList(): Pair<List<Section>, List<Emoji>> {
        val sectionList = mutableListOf<Section>()
        val emojiList = mutableListOf<Emoji>()
        return try {
            service.getHtmlStringByPage("https://adnmb3.com/Public/Js/h.desktop.js")?.let { rp ->
                "emotList = \\[(.*)]".toRegex().find(rp)?.groupValues?.get(1)?.let { emojiSting ->
                    emojiSting.replace("\"", "").split(", ").forEachIndexed { index, emoji ->
                        emojiList.add(
                            Emoji(
                                index,
                                emoji
                            ).also {
                                Log.e(LOG_TAG, it.toString())
                            }
                        )
                    }
                }
            }
            service.getHtmlStringByPage("https://adnmb3.com/Forum")?.let { rp ->
                val doc = Jsoup.parse(rp)
                val sectionGroups = doc.select("li[class~=uk-parent.*]")
                var index = 0
                sectionGroups.forEach { sectionGroup ->
                    val groupName =
                        sectionGroup.selectFirst("a[class=h-nav-parent-header]")?.ownText()
                            ?: "(ﾟДﾟ≡ﾟДﾟ)"
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



            Pair(sectionList, emojiList)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "getSectionList ${e.stackTraceToString()}")
            Pair(sectionList, emojiList)
        }

    }


}