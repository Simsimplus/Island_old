package com.simsim.island.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.simsim.island.database.ThreadDao
import com.simsim.island.model.PoThread
import com.simsim.island.model.ReplyThread
import com.simsim.island.util.LOG_TAG
import com.simsim.island.util.toReplyThread

class SavedReplyThreadPagingSource(
    val threadDao: ThreadDao,
    val poThread: PoThread
): PagingSource<Int, ReplyThread>() {
    override fun getRefreshKey(state: PagingState<Int, ReplyThread>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ReplyThread> {
        return try {
            val  threadList= mutableListOf<ReplyThread>()
//            threadList.add(poThread.toBasicThread())
            threadList.addAll(threadDao.getAllSavedReplyThreads(poThread.threadId).map {
                it.toReplyThread()
            })
            LoadResult.Page(
                data=threadList,
                prevKey = null,
                nextKey = null
            )
        }catch (e:Exception){
            Log.e(LOG_TAG,"SavedPoThreadPagingSource:${e.stackTraceToString()}")
            LoadResult.Error(e)
        }
    }
}