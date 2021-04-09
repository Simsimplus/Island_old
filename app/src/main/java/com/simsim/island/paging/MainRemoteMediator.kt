package com.simsim.island.paging

import android.net.Uri
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.simsim.island.database.IslandDatabase
import com.simsim.island.model.MainRemoteKey
import com.simsim.island.model.PoThread
import com.simsim.island.repository.AislandRepo
import com.simsim.island.service.AislandNetworkService
import com.simsim.island.util.LOG_TAG

@OptIn(ExperimentalPagingApi::class)
class MainRemoteMediator(
    private val service: AislandNetworkService,
    private val section: String,
    private val database: IslandDatabase
) : RemoteMediator<Int, PoThread>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PoThread>
    ): MediatorResult {
        Log.e(LOG_TAG,"LoadType:$loadType")
        val page: Int = when (loadType) {
            LoadType.REFRESH -> {
                1
            }
            LoadType.PREPEND -> {
//                val remoteKey =
//                    state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
//                        ?.let { poThread ->
//                            database.keyDao().getMainKey(poThread.ThreadId)
//                        }
//                val previousKey = remoteKey?.previousKey
//                previousKey ?: return MediatorResult.Success(endOfPaginationReached = false)
//                previousKey
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            LoadType.APPEND -> {
                val remoteKey = state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
                    ?.let { poThread ->
                        database.keyDao().getMainKey(poThread.ThreadId)
                    }
                val nextKey = remoteKey?.nextKey
                nextKey ?: return MediatorResult.Success(endOfPaginationReached = false)
                nextKey
            }
        }
        try {
            val url = "https://adnmb3.com/m/f/%s?page=%d".format(section, page)
            val response = service.getHtmlStringByPage(url)
            val threadList: List<PoThread>? = if (response != null) {
                AislandRepo.responseToThreadList(Uri.decode(section), response)
            } else {
                null
            }
            val endOfPaginationReached = threadList.isNullOrEmpty()
            if (threadList != null && threadList.isNotEmpty()) {
                database.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        database.keyDao().clearMainKeys()
                        database.threadDao().clearAllPoThread()
                    }
                    val previousKey = if (page == 1) null else page - 1
                    val nextKey = if (endOfPaginationReached) null else page + 1
                    val keys = threadList.map {
                        MainRemoteKey(it.ThreadId, previousKey, nextKey)
                    }
                    database.keyDao().insertMainKeys(keys)
                    database.threadDao().insertAllPoThreads(threadList)
                    database.threadDao().insertAllReplyThreads(
                        threadList.flatMap {
                            it.replyThreads
                        }
                    )
                }
                return MediatorResult.Success(endOfPaginationReached)
            } else {
                return MediatorResult.Success(endOfPaginationReached = false)
            }

        } catch (e: Exception) {
            Log.e("Simsim", "main remote mediator error:${e.stackTraceToString()}")
            return MediatorResult.Error(e)
        }
    }
}