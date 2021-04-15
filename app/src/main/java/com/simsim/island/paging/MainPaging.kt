package com.simsim.island.paging

import android.net.Uri
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.simsim.island.model.BasicThread
import com.simsim.island.model.PoThread
import com.simsim.island.repository.AislandRepo
import com.simsim.island.service.AislandNetworkService

class MainPaging(private val service: AislandNetworkService, private val section: String) :
    PagingSource<Int, PoThread>() {
    companion object {
        private const val baseUrl = "https://adnmb3.com/m/f/%s?page=%d"
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PoThread> {
//        if (!isNetworkConnected) return LoadResult.Error(Exception("not network"))
        try {
            // Start refresh at page 1 if undefined.
            val nextPageNumber = params.key ?: 1
            val response = service.getHtmlStringByPage(baseUrl.format(section, nextPageNumber))
            val threadList: List<PoThread>? = if (response != null) {
                AislandRepo.responseToThreadList(Uri.decode(section), response,nextPageNumber)
            } else {
                null
            }

            return LoadResult.Page(
                threadList ?: mutableListOf(),
                prevKey = if (nextPageNumber == 1) null else nextPageNumber - 1,
                nextKey = nextPageNumber + 1,
            )
        } catch (e: Exception) {
            Log.e("Simsim:error ", e.stackTraceToString())
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PoThread>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}