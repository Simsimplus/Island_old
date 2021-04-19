package com.simsim.island.repository

import android.util.Log
import com.simsim.island.model.BasicThread
import com.simsim.island.model.PoThread
import com.simsim.island.model.Section
import com.simsim.island.service.AislandNetworkService
import com.simsim.island.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import javax.inject.Inject

class AislandRepo @Inject constructor(private val service: AislandNetworkService) {
    companion object {
        //        private const val baseUrl = "https://adnmb3.com/m/f/%s?page=%d"
//        const val islandUrl="https://adnmb3.com"
        fun responseToThreadList(section: String, response: String?,page:Int): List<PoThread>? {
            val poThreads = mutableListOf<PoThread>()
            if (response != null) {
//                Log.e("Simsim:success", response)
                val doc = Jsoup.parse(response)
                val divs = doc.select("div[class=h-threads-item uk-clearfix]")
//                val fId =doc.selectFirst("input[name=fid]")?.attr("value")?:null
                divs.forEachIndexed  { index,mainDiv ->
                    val poDiv=mainDiv.selectFirst("div[class=h-threads-item-main]")?:throw IllegalArgumentException("can parse po thread")
                    val poBasicThread = divToBasicThread(div = poDiv, isPo = true, section = section,poThreadId = 0)
                    val replyDivs =
                        poDiv.select("div[class=h-threads-item-reply-main]")
                    val replyThreads = mutableListOf<BasicThread>()
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
                    val commentNumberDiv=mainDiv.selectFirst("div[class=h-threads-tips]")
                    var commentsNumber= ((commentNumberDiv?.text()?.firstNumber() ?: 0)+replyThreads.size).toString()
//                    var commentsNumber = doc.select("font").find { font ->
//                        "回应有\\s*(\\d+)\\s*篇被省略.*".toRegex().matches(font.ownText())
//                    }?.ownText()?.firstNumberPlus5() ?:
                    if (commentsNumber.length >= 4) commentsNumber = "1k+"
                    poBasicThread.commentsNumber = commentsNumber
                    val poThread= basicThreadToPoThread(poBasicThread,replyThreads,20*page+index)
                    poThreads.add(poThread)

//                    Log.e("Simsim", poThread.toString())
                }
                return poThreads

            } else {
                return null
            }
        }

        fun divToBasicThread(div: Element, isPo: Boolean, section: String,poThreadId:Long,poUid:String="ahsgjkdghahweuihuihafuidsbfiaubf"): BasicThread {
            val basicThread = BasicThread(section = section, isPo = isPo,poThreadId =poThreadId,replyThreadId = 0)
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
                            val actualSection=divTag.selectFirst("spam[style=color:gray]")
                            actualSection?.let {
                                basicThread.timelineActualSection=actualSection.ownText()
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
                                        basicThread.time = try {
                                            LocalDateTime.parse(
                                                child.ownText()
                                                    .replace("\\(.\\)|(?=\\d)\\s".toRegex(), "T")
                                            )
                                        } catch (e: Exception) {
                                            LocalDateTime.of(2099, 1, 1, 0, 1)
                                        }
                                    }
                                    "h-threads-info-uid" -> {
                                        val isManager= child.selectFirst("font[color=red]")!=null
                                        basicThread.isManager=isManager
                                        basicThread.uid=if (isManager){
                                            child.selectFirst("font[color=red]").ownText()
                                        }else{
                                            child.ownText()
                                        }

                                        if (basicThread.uid == poUid){
                                            basicThread.isPo=true
                                        }
                                        if (divTag.text().contains("PO主")){
                                            basicThread.isPo=true
                                        }

                                    }
                                    "h-threads-info-report-btn"->{
                                        child.selectFirst("a[class=h-threads-info-id]")?.also {grandChild->
                                            basicThread.replyThreadId =
                                                grandChild.ownText().replace("\\D".toRegex(), "").toLong()
                                        }
                                    }
                                }
                            }
                        }
                        "h-threads-content" -> {
                            val referenceTags = try {
                                divTag.select("font[color=#789922]")
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
                            basicThread.content = divTag.ownText()
                        }
                    }
                }

            }
//        Log.e("Simsim", "poUid:$poUid  $basicThread")
            return basicThread
        }
        fun basicThreadToPoThread(basicThread: BasicThread, replyThreads:List<BasicThread>,pageIndex:Int):PoThread{
            return PoThread(
                isManager = basicThread.isManager,
                threadId=basicThread.replyThreadId,
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
                pageIndex = pageIndex,
                timelineActualSection = basicThread.timelineActualSection
            ).apply {
                this.replyThreads=replyThreads
            }
        }
    }
    suspend fun getSectionList(): Flow<Section> {
        val sectionList= mutableListOf<Section>()
        return try {
            val response=service.getHtmlStringByPage("https://adnmb3.com/Forum")
            response?.let { rp->
                val doc=Jsoup.parse(rp)
                val sectionGroups=doc.select("li[class~=uk-parent.*]")
                var index=0
                sectionGroups.forEach {sectionGroup->
                    val groupName=sectionGroup.selectFirst("a[class=h-nav-parent-header]")?.ownText()?:"(ﾟДﾟ≡ﾟДﾟ)"
                    val listTags=sectionGroup.select("li")
                    listTags.forEach { li->
                        val section=li.selectFirst("a[href~=[/f]{3,}.*|[/]Forum[/]timeline.*]")
                        if (section!=null){
                            val sectionUrl=islandUrl+section.attr("href")
                            val sectionResponse=service.getHtmlStringByPage(sectionUrl)
                            val fId=sectionResponse?.let { r->
                                val sectionDoc=Jsoup.parse(r)
                                sectionDoc.selectFirst("input[name=fid]")?.attr("value")?:""
                            }?:""
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
        }catch (e:Exception){
            Log.e(LOG_TAG,"getSectionList ${e.stackTraceToString()}")
            emptyFlow()
        }

    }
}