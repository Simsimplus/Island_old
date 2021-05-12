package com.simsim.island.repository

import android.util.Log
import com.simsim.island.database.IslandDatabase
import com.simsim.island.model.*
import com.simsim.island.paging.DetailRemoteMediator
import com.simsim.island.paging.MainRemoteMediator
import com.simsim.island.paging.SavedPoThreadPagingSource
import com.simsim.island.paging.SavedReplyThreadPagingSource
import com.simsim.island.service.AislandNetworkService
import com.simsim.island.util.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.InputStream
import java.time.LocalDateTime
import javax.inject.Inject

class AislandRepo @Inject constructor(
    private val networkService: AislandNetworkService,
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

    internal val threadDao = database.threadDao()
    internal val blockRuleDao = database.blockRuleDao()
    internal val recordDao = database.recordDao()
    internal val sectionDao = database.sectionDao()
    internal val emojiDao = database.emojiDao()
    internal val cookieDao = database.cookieDao()
    internal val savedPoThreadPagingSource = SavedPoThreadPagingSource(threadDao)
    internal val keyDao = database.keyDao()
    fun getSavedReplyThreadPagingSource(poThread: PoThread) =
        SavedReplyThreadPagingSource(threadDao, poThread = poThread)

    suspend fun getSectionAndEmojiList(): Pair<List<Section>, List<Emoji>> {
        val sectionList = mutableListOf<Section>()
        val emojiList = mutableListOf<Emoji>()
        return try {
            networkService.getHtmlStringByPage("https://adnmb3.com/Public/Js/h.desktop.js")
                ?.let { rp ->
                    "emotList = \\[(.*)]".toRegex().find(rp)?.groupValues?.get(1)
                        ?.let { emojiSting ->
                            emojiSting.replace("\"", "").split(", ")
                                .forEachIndexed { index, emoji ->
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
            networkService.getHtmlStringByPage("https://adnmb3.com/Forum")?.let { rp ->
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
                            val sectionResponse = networkService.getHtmlStringByPage(sectionUrl)
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
        } finally {
            Log.d(LOG_TAG, "getSectionAndEmojiList")
            sectionDao.insertAllSection(sectionList)
            emojiDao.insertAllEmojis(emojiList)
        }

    }

    suspend fun updateUpdateRecord() {
        recordDao.insertRecord(UpdateRecord(lastUpdateTime = LocalDateTime.now()))
    }

    suspend fun getAllBlockRules(): List<BlockRule> = blockRuleDao.getAllBlockRules()
    suspend fun insertBlockRule(blockRule: BlockRule)=blockRuleDao.insertBlockRule(blockRule)
    fun getAllSectionFlow() = database.sectionDao().getAllSectionFlow()
    fun getAllBlockRulesFlow() = blockRuleDao.getAllBlockRulesFlow()
    fun getAllPoThreadsBySection(sectionName: String) =
        threadDao.getAllPoThreadsBySection(sectionName)

    fun getAllReplyThreadsPagingSource(poThreadId: Long) =
        threadDao.getAllReplyThreadsPagingSource(poThreadId = poThreadId)

    suspend fun saveReplyThreads(poThreadId: Long) {
        threadDao.getAllReplyThreads(poThreadId).let { replyThreads ->
            threadDao.insertAllSavedReplyThread(
                replyThreads.map {
                    it.toSavedReplyThread()
                }
            )
        }
    }

    suspend fun getCookiesFromUserSystem(cookieMap: Map<String, String>) {
        val cookieList = networkService.getCookies(cookieMap)
        database.cookieDao().insertAllCookies(cookieList)
    }

    suspend fun getRecord() = database.recordDao().getRecord()
    fun getMainRemoteMediator(sectionName: String, sectionUrl: String) = MainRemoteMediator(
        service = networkService,
        sectionName = sectionName,
        sectionUrl = sectionUrl,
        database = database
    )

    fun getDetailRemoteMediator(poThreadId: Long, initialPage: Int, onlyPo: Boolean = false) =
        DetailRemoteMediator(
            service = networkService,
            poThreadId = poThreadId,
            database = database,
            initialPage = initialPage,
            onlyPo = onlyPo
        )

    suspend fun getReplyThreadsByPage(page: Int, poThreadId: Long) {
        networkService.getReplyThreadsAndMaxPageByPage(
            page = page,
            poThreadId = poThreadId,
            saveDataToDBHere = true,
            forStar = true
        )
    }

    suspend fun insertSavedPoThread(staredPoThreads: SavedPoThread) =
        threadDao.insertSavedPoThread(staredPoThreads)

    suspend fun isPoThreadStared(poThreadId: Long) = threadDao.isPoThreadStared(poThreadId)
    fun isPoThreadStaredFlow(poThreadId: Long) = threadDao.isPoThreadStaredFlow(poThreadId)
    suspend fun deleteSavedPoThread(poThreadId: Long) =
        threadDao.deleteSavedPoThread(threadDao.getSavedPoThread(poThreadId))

    suspend fun getPoThread(poThreadId: Long) = threadDao.getPoThread(poThreadId)
    suspend fun updatePoThread(poThread: PoThread)=threadDao.updatePoThread(poThread)
    suspend fun getAllPoThreadsByUid(uid:String)=threadDao.getAllPoThreadsByUid(uid)

    suspend fun getCurrentPreviousNextPage() =
        Pair(keyDao.getCurrentPreviousPage(), keyDao.getCurrentNextPage())

    suspend fun getReference(referenceId:Long):ReplyThread{
        val url = "https://adnmb3.com/Home/Forum/ref?id=$referenceId"
        Log.e(LOG_TAG, "fetch reference from $url")
        Log.e("Simsim", "request for thread detail:$url")
        val response = networkService.getHtmlStringByPage(url)
        if (response != null) {
            val doc = Jsoup.parse(response)
            val poThreadId = doc.selectFirst("a[class=h-threads-info-id]")?.let { e ->
                e.attr("href").removeQueryTail().firstNumber().toLong()
            } ?: 0L
            val time =
                doc.selectFirst("span[class=h-threads-info-createdat]")?.let { e ->
                    parseIslandTime(e.ownText())
                } ?: LocalDateTime.now()
            val uid = doc.selectFirst("span[class=h-threads-info-uid]")?.let { e ->
                e.ownText().replace("ID:", "").trim()
            } ?: ""
            val content = doc.selectFirst("div[class=h-threads-content]")?.let { e ->
                e.wholeText().trim()
            } ?: ""
            return ReplyThread(
                replyThreadId = referenceId,
                poThreadId = poThreadId,
                time = time,
                uid = uid,
                content = content,
                section = ""
            )
    }else {
            return ReplyThread(replyThreadId = 888, poThreadId = 888, section = "")
        }
    }
    suspend fun getAllCookies()=database.cookieDao().getAllCookies()
    suspend fun doPost(
        content: String,
        image: InputStream?,
        imageType: String?,
        imageName: String?,
        fId: String,
        waterMark: Boolean = false,
        name: String = "",
        email: String = "",
        title: String = ""
    ) = networkService.doPost(
        content,
        image,
        imageType,
        imageName,
        fId,
        waterMark,
        name,
        email,
        title,
    )
    suspend fun doReply(
//        cookie: String,
        poThreadId: Long,
        content: String,
        image: InputStream?,
        imageType: String?,
        imageName: String?,
        waterMark: Boolean = false,
        name: String = "",
        email: String = "",
        title: String = ""
    )=networkService.doReply(
//                cookie,
        poThreadId,
        content,
        image,
        imageType,
        imageName,
        waterMark,
        name,
        email,
        title,
    )

    suspend fun updateCookies(cookies: List<Cookie>)=cookieDao.updateCookies(cookies)
    suspend fun deleteCookie(cookieValue: String)=cookieDao.deleteCookie(cookieValue)
    fun isAnyCookieAvailable()=cookieDao.isAnyCookieAvailable()
    suspend fun updateBlockRule(blockRule: BlockRule)=blockRuleDao.updateBlockRule(blockRule)
    suspend fun deleteBlockRule(blockRule: BlockRule)=blockRuleDao.deleteBlockRule(blockRule)
    suspend fun getBlockRule(blockRuleIndex: Long)=blockRuleDao.getBlockRule(blockRuleIndex)
    suspend fun getSavedPoThread(threadId: Long)=threadDao.getSavedPoThread(threadId)
    suspend fun countSavedReplyThreads(threadId: Long)=threadDao.countSavedReplyThreads(threadId)
    fun getActiveCookieFlow()=cookieDao.getActiveCookieFlow()
    suspend fun getCookieByValue(result: String)=cookieDao.getCookieByValue(result)
    suspend fun insertCookie(newCookie: Cookie)=cookieDao.insertCookie(newCookie)
    suspend fun getAllSavedPoThread()=threadDao.getAllSavedPoThread()
    suspend fun isAnyEmoji(): Boolean =emojiDao.isAny()
    suspend fun insertAllEmojis(emojiList: List<Emoji>)=emojiDao.insertAllEmojis(emojiList)
    suspend fun isAnySectionInDB(): Boolean =sectionDao.isAnySectionInDB()
    suspend fun insertAllSection(sections: List<Section>)=sectionDao.insertAllSection(sections)
    fun getAllEmojisFlow()=emojiDao.getAllEmojisFlow()
}