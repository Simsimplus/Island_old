package com.simsim.island.paging

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.simsim.island.database.IslandDatabase
import com.simsim.island.model.DetailRemoteKey
import com.simsim.island.model.ReplyThread
import com.simsim.island.service.AislandNetworkService
import com.simsim.island.util.LOG_TAG

@OptIn(ExperimentalPagingApi::class)
class DetailRemoteMediator(
    private val service: AislandNetworkService,
    private val poThreadId: Long,
    private val database: IslandDatabase,
    private val initialPage: Int,
    private val onlyPo:Boolean=false
) : RemoteMediator<Int, ReplyThread>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ReplyThread>
    ): MediatorResult {
        Log.e(LOG_TAG, "LoadType:$loadType")
        val page: Int = when (loadType) {
            LoadType.REFRESH -> {
                database.threadDao().clearAllReplyThreads(poThreadId)
                database.keyDao().clearReplyThreadsKeys()
                initialPage
            }
            LoadType.PREPEND -> {
                var remoteKey = state.firstItemOrNull()
                    ?.let { replyThread ->
                        Log.e(LOG_TAG, "fetch detail remote key from db")
                        var result: DetailRemoteKey?
                        while (true) {
                            result = database.keyDao().getDetailKey(replyThread.replyThreadId)
                            if (result != null) {
                                break
                            }
                        }
                        result
                    }
                if (remoteKey == null) {
                    remoteKey =
                        database.threadDao().getFirstReplyThread(poThreadId)?.let {
                            database.keyDao().getDetailKey(it.replyThreadId)
                        }
                }
                Log.e(LOG_TAG, "detail remoteKey:$remoteKey")
                val previousKey = remoteKey?.previousKey
                Log.e(LOG_TAG, "detail previousKey:$previousKey")
                previousKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                previousKey
            }
            LoadType.APPEND -> {
//                Log.e(LOG_TAG, "detail state.lastItemOrNull():${state.lastItemOrNull() ?: "null"}")
                var remoteKey = state.lastItemOrNull()
                    ?.let { replyThread ->
                        Log.e(LOG_TAG, "fetch detail remote key from db")
                        var result: DetailRemoteKey?
                        while (true) {
                            result = database.keyDao().getDetailKey(replyThread.replyThreadId)
                            if (result != null) {
                                break
                            }
                        }
                        result
                    }
                if (remoteKey == null) {
                    remoteKey =
                        database.threadDao().getLastReplyThread(poThreadId)?.let {
                            database.keyDao().getDetailKey(it.replyThreadId)
                        }
                }
                Log.e(LOG_TAG, "detail remoteKey:$remoteKey")
                val nextKey = remoteKey?.nextKey
                Log.e(LOG_TAG, "detail nextKey:$nextKey")
                nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                nextKey
            }
        }
        //this block maybe unnecessary
//        database.keyDao().getCurrentMaxPage()?.let { maxNextPage->
//            if ((maxNextPage-1)>page){
//                return MediatorResult.Success(endOfPaginationReached = false)
//            }
//        }
        try {
//            val maxPage: Int?
//            val url = "https://adnmb3.com/t/$poThreadId?page=$page"
////            Log.e("Simsim", "request for thread detail:$url")
//            val response = service.getHtmlStringByPage(url)
//            val threadList: List<ReplyThread>? = if (response != null) {
//                val doc = Jsoup.parse(response)
//                val poThreadDiv = doc.selectFirst("div[class=h-threads-item-main]")
//                val pages = doc.select("[href~=.*page=[0-9]+]")
//                maxPage = try {
//                    pages.map { pageTag->
//                        pageTag.attr("href").findPageNumber().toInt()
//                    }.maxOrNull()
//                } catch (e: Exception) {
//                    999
//                }
//                poThreadDiv?.let {
//                    val poThread= AislandRepo.basicThreadToPoThread(
//                            AislandRepo.divToBasicThread(
//                                poThreadDiv,
//                                section = "",
//                                poThreadId = poThreadId,
//                                isPo = true
//                            ),
//                            mutableListOf(), 999
//                        ).also {
//                            database.threadDao().insertPoThread(it)
//                        database.threadDao().getPoThread(poThreadId)?.let { poThread ->
//                            database.threadDao().updatePoThread(poThread.apply {
//                                this.maxPage=maxPage?:0
//                                try {
//                                    this.commentsNumber=max(database.threadDao().countReplyThreads(poThreadId)-1,this.commentsNumber.toInt()).toString()
//                                    Log.e(LOG_TAG,"update poThread comment number :${this.commentsNumber}")
//                                }catch (e:Exception){
//                                    Log.e(LOG_TAG,"update poThread comment number failed:${e.stackTraceToString()}")
//                                }
//                            })
//                        }
//                        }
//
////                    Log.e(LOG_TAG, "detailRM poThread:$poThread")
//
////                    Log.e("Simsim", "max page:$maxPage")
//                    val replyThreads = mutableListOf<ReplyThread>()
//                    if (page == 1) {
//                        Log.e(LOG_TAG, "detailRM poThredToBasicThread:${poThread.toBasicThread()}")
//                        replyThreads.add(poThread.toBasicThread())
//                    }
//                    val replyDivs = doc.select("div[class=h-threads-item-reply-main]")
//                    replyDivs.forEach { replyDiv ->
//                        val replyThread = AislandRepo.divToBasicThread(
//                            div = replyDiv,
//                            isPo = false,
//                            section = poThread.section,
//                            poThreadId = poThread.threadId,
//                            poUid = poThread.uid
//                        )
//                        replyThreads.add(
//                            replyThread
//                        )
////                        Log.e(LOG_TAG, "poUid=${poThread.uid},${replyThread}")
//                    }
//                    replyThreads.removeIf {
//                        it.uid.isBlank()
//                    }
//                    replyThreads
//                }
//
//            } else {
//                maxPage = 999
//                null
//            }
            val (threadList, maxPage) = service.getReplyThreadsAndMaxPageByPage(page, poThreadId,onlyPo)
            Log.e(LOG_TAG, "maxPage:$maxPage")
            threadList
                ?: return MediatorResult.Error(Exception("can't reach to page url:${"https://adnmb3.com/t/$poThreadId?page=$page"}"))
            maxPage ?: return MediatorResult.Error(IllegalArgumentException("can't find maxPage"))
            val endOfPaginationReached = if (page >= maxPage) {
                when {
                    threadList.isEmpty() -> true
                    threadList.count() == 1 && threadList[0].replyThreadId == 9999999L -> true
                    else -> false
                }
            } else {
                false
            }
            if (threadList.isNotEmpty()) {
                database.withTransaction {
                    val previousKey = if (page == 1) null else page - 1
                    val nextKey = if (threadList.size < 19) {
                        page
                    } else {
                        if (endOfPaginationReached) maxPage else page + 1
                    }
                    val keys = threadList.map {
                        val key = DetailRemoteKey(it.replyThreadId, previousKey, nextKey,page)
//                        database.keyDao().insertDetailKey(key)
                        key
                    }
                    database.keyDao().insertDetailKeys(keys)
                    database.threadDao().insertAllReplyThreads(threadList)
                }
                return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            } else {
                return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            }

        } catch (e: Exception) {
            Log.e("Simsim", "detail remote mediator error:${e.stackTraceToString()}")
            return MediatorResult.Error(e)
        }
    }
}