package com.simsim.island.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.simsim.island.model.IslandThread
import com.simsim.island.repository.AislandRepo
import com.simsim.island.service.AislandNetworkService
import java.lang.Exception

class IslandMainPaging(private val service: AislandNetworkService, private val section: String):PagingSource<Int,IslandThread>() {
    companion object {
        private const val baseUrl = "https://adnmb3.com/m/f/%s?page=%d"
    }
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, IslandThread> {
        try {
            // Start refresh at page 1 if undefined.
            val nextPageNumber = params.key ?: 1
            val response=service.getHtmlStringByPage(baseUrl.format(section,nextPageNumber))
            val threadList:List<IslandThread>? = if (response!=null){
                AislandRepo.responseToThreadList(section,response)
            }else{
                null
            }

            return LoadResult.Page(
                threadList?: mutableListOf(),
                prevKey = if (nextPageNumber==1) null else nextPageNumber-1,
                nextKey = nextPageNumber+1,
            )
        }catch (e:Exception){
            Log.e("Simsim:error ",e.stackTraceToString())
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, IslandThread>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}