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
        Log.e(LOG_TAG, "LoadType:$loadType")
        val page: Int = when (loadType) {
            LoadType.REFRESH -> {
                1
            }
            LoadType.PREPEND -> {
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            LoadType.APPEND -> {
                var remoteKey = state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
                    ?.let { poThread ->
                            database.keyDao().getMainKey(poThread.threadId)
                    }
                if (remoteKey == null) {
                    remoteKey =database.threadDao().getLastPoThread(section)?.let {
                            database.keyDao().getMainKey(it.threadId)
                        }
                }
                val nextKey = remoteKey?.nextKey
                Log.e(LOG_TAG,"nextKey:$nextKey")
                nextKey?:return MediatorResult.Success(endOfPaginationReached = true)
                nextKey
            }
        }
        try {
            val url = "https://adnmb3.com/m/f/%s?page=%d".format(section, page)
            val response = service.getHtmlStringByPage(url)

            val threadList: List<PoThread>? = if (response != null) {
                AislandRepo.responseToThreadList(Uri.decode(section), response,page)
            } else {
                null
            }
//            val endOfPaginationReached = threadList.isNullOrEmpty()
            Log.e(LOG_TAG,"threadList:$threadList")
            if (threadList != null && threadList.isNotEmpty()) {
                database.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        database.keyDao().clearMainKeys()
                        database.threadDao().clearAllPoThread()
                    }
                    val previousKey = if (page == 1) null else page - 1
                    val nextKey = page + 1
                    val keys = threadList.map {
                        MainRemoteKey(it.threadId, previousKey, nextKey)
                    }
                    database.keyDao().insertMainKeys(keys)
                    threadList.forEach {
                        if (database.threadDao().isPoThreadStared(it.threadId)){
                            it.isStar=true
                            }
                        if (database.threadDao().isPoThreadCollected(it.threadId)){
                            val collectedPoThread=database.threadDao().getPoThread(it.threadId)
                            collectedPoThread?.let {
                                it.collectTime=collectedPoThread.collectTime
                                it.pageIndex=collectedPoThread.pageIndex
                            }
                        }
                    }
                    database.threadDao().insertAllPoThreads(threadList)
                    //insert po threads first to give main remote key right foreign key

                }
                return MediatorResult.Success(endOfPaginationReached = false)
            } else {
                return MediatorResult.Success(endOfPaginationReached = false)
            }

        } catch (e: Exception) {
            Log.e("Simsim", "main remote mediator error:${e.stackTraceToString()}")
            return MediatorResult.Error(e)
        }
    }
}