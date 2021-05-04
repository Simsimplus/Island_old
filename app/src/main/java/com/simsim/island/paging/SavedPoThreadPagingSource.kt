package com.simsim.island.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.simsim.island.database.ThreadDao
import com.simsim.island.model.PoThread
import com.simsim.island.util.LOG_TAG
import com.simsim.island.util.toPoThread

class SavedPoThreadPagingSource(
    val threadDao:ThreadDao
): PagingSource<Int, PoThread>() {
    override fun getRefreshKey(state: PagingState<Int, PoThread>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PoThread> {
        return try {
            val  threadList=threadDao.getAllSavedPoThread().map {
                it.toPoThread()
            }
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