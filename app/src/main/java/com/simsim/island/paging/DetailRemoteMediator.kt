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
import com.simsim.island.repository.AislandRepo
import com.simsim.island.service.AislandNetworkService
import com.simsim.island.util.LOG_TAG
import com.simsim.island.util.findPageNumber
import com.simsim.island.util.toBasicThread
import org.jsoup.Jsoup

@OptIn(ExperimentalPagingApi::class)
class DetailRemoteMediator(
    private val service: AislandNetworkService,
    private val poThreadId: Long,
    private val database: IslandDatabase
) : RemoteMediator<Int, ReplyThread>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ReplyThread>
    ): MediatorResult {
        Log.e(LOG_TAG, "LoadType:$loadType")
        val page: Int = when (loadType) {
            LoadType.REFRESH -> {
                1
            }
            LoadType.PREPEND -> {
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            LoadType.APPEND -> {
                Log.e(LOG_TAG, "detail state.lastItemOrNull():${state.lastItemOrNull() ?: "null"}")
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
        try {
            val maxPage: Int?
            val url = "https://adnmb3.com/t/$poThreadId?page=$page"
            Log.e("Simsim", "request for thread detail:$url")
            val response = service.getHtmlStringByPage(url)
            val threadList: List<ReplyThread>? = if (response != null) {
                val doc = Jsoup.parse(response)
                val poThreadDiv = doc.selectFirst("div[class=h-threads-item-main]")
                val pages = doc.select("[href~=.*page=[0-9]+]")
                maxPage = try {
                    pages.last().attr("href").findPageNumber().toInt()
                } catch (e: Exception) {
                    999
                }
                poThreadDiv?.let {
                    val poThread= database.threadDao().getPoThread(poThreadId)
                        ?: AislandRepo.basicThreadToPoThread(
                            AislandRepo.divToBasicThread(
                                poThreadDiv,
                                section = "",
                                poThreadId = poThreadId,
                                isPo = true
                            ),
                            mutableListOf(), 999
                        ).also {
                            database.threadDao().insertAllPoThreads(mutableListOf(it))
                        }

//                    Log.e(LOG_TAG, "detailRM poThread:$poThread")

//                    Log.e("Simsim", "max page:$maxPage")
                    val replyThreads = mutableListOf<ReplyThread>()
                    if (page == 1) {


                        Log.e(LOG_TAG, "detailRM poThredToBasicThread:${poThread.toBasicThread()}")
                        replyThreads.add(poThread.toBasicThread())
                    }
                    val replyDivs = doc.select("div[class=h-threads-item-reply-main]")
                    replyDivs.forEach { replyDiv ->
                        val replyThread = AislandRepo.divToBasicThread(
                            div = replyDiv,
                            isPo = false,
                            section = poThread.section,
                            poThreadId = poThread.threadId,
                            poUid = poThread.uid
                        )
                        replyThreads.add(
                            replyThread
                        )
                        Log.e(LOG_TAG, "poUid=${poThread.uid},${replyThread}")
                    }
                    replyThreads.removeIf {
                        it.uid.isBlank()
                    }
                    replyThreads
                }

            } else {
                maxPage = 999
                null
            }
            Log.e(LOG_TAG, "maxPage:$maxPage")
            threadList ?: return MediatorResult.Error(Exception("can't reach to page url:$url"))
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
                    if (loadType == LoadType.REFRESH) {
                        database.keyDao().clearReplyThreadsKeys()
//                        database.threadDao().clearAllReplyThreads(poThread.threadId)
                    }
                    val previousKey = if (page == 1) null else page - 1
                    val nextKey = if (endOfPaginationReached) maxPage else page + 1
                    val keys = threadList.map {
                        val key = DetailRemoteKey(it.replyThreadId, previousKey, nextKey)
//                        database.keyDao().insertDetailKey(key)
                        key
                    }

                    database.threadDao().insertAllReplyThreads(threadList)
                    database.keyDao().insertDetailKeys(keys)
                }
                return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            } else {
                return MediatorResult.Success(endOfPaginationReached = true)
            }

        } catch (e: Exception) {
            Log.e("Simsim", "detail remote mediator error:${e.stackTraceToString()}")
            return MediatorResult.Error(e)
        }
    }
}