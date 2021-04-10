package com.simsim.island.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.simsim.island.model.BasicThread
import com.simsim.island.model.PoThread
import com.simsim.island.repository.AislandRepo
import com.simsim.island.service.AislandNetworkService
import com.simsim.island.util.findPageNumber
import com.simsim.island.util.toBasicThread
import org.jsoup.Jsoup

class DetailPaging(
    private val service: AislandNetworkService,
    private val poThread: PoThread,
) :
    PagingSource<Int, BasicThread>() {
    override fun getRefreshKey(state: PagingState<Int, BasicThread>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, BasicThread> {
//        if (!isNetworkConnected) return LoadResult.Error(Exception("not network"))
        try {
//            val poThread=mainThread.find {
//                it.isPo
//            }?:throw IllegalArgumentException("no poThread is available")
            // Start refresh at page 1 if undefined.
            val maxPage:Int?
            val nextPageNumber = params.key ?: 1
            val url="https://adnmb3.com/" + poThread.link + "?page=$nextPageNumber"
            Log.e("Simsim","request for thread detail:$url")
            val response =
                service.getHtmlStringByPage(url)
            val threadList: List<BasicThread>? = if (response != null) {
                val doc = Jsoup.parse(response)
                val pages=doc.select("[href~=.*page=[0-9]+]")
                maxPage =pages.last().attr("href").findPageNumber().toInt()
                Log.e("Simsim","max page:$maxPage")
                val replyThreads = mutableListOf<BasicThread>()
                if (nextPageNumber == 1) {
                    replyThreads.add(poThread.toBasicThread())
                }
                val replyDivs = doc.select("div[class=uk-container h-threads-reply-container]")
                replyDivs.forEach { replyDiv ->
                    replyThreads.add(AislandRepo.divToBasicThread(div=replyDiv,isPo = false,section = poThread.section,poThreadId = poThread.threadId,fId = poThread.fId))
                }
                replyThreads.removeIf {
                    it.uid.isBlank()
                }
                replyThreads
            } else {
                maxPage=null
                null
            }

            return LoadResult.Page(
                threadList ?: mutableListOf(),
                prevKey = if (nextPageNumber == 1) null else nextPageNumber - 1,
                nextKey = if (nextPageNumber==maxPage) null else nextPageNumber + 1,
            )
        } catch (e: Exception) {
            Log.e("Simsim:error ", e.stackTraceToString())
            return LoadResult.Error(e)
        }
    }

}