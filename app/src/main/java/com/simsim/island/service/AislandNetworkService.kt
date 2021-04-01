package com.simsim.island.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import javax.inject.Inject


class AislandNetworkService @Inject constructor() {
    companion object {
        private const val baseUrl = "https://adnmb3.com"
    }

    private val retrofit = Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(ScalarsConverterFactory.create()).build()

    interface IslandHtmlService {
        @GET
        suspend fun getHtmlStringByPage(@Url url:String): Response<String>
    }

    private val service: IslandHtmlService by lazy { retrofit.create(IslandHtmlService::class.java) }

    suspend fun getHtmlStringByPage(url:String):String?=withContext(Dispatchers.IO){
            val response:String?
            val call = service.getHtmlStringByPage(url)
            Log.e("Simsim:url:", url)
            if (call.isSuccessful) {
                response = call.body()
//                Log.e("Simsim:response body:", response.toString())
            } else {
                response=null
//                Log.e("Simsim:error:", call.code().toString())
            }
        response
        }



    //    internal val requestResult=MutableLiveData<List<IslandThread>>()
//    suspend fun getXmlStringByPage(section:String,page:Int): String? {
//        var ret:String?=null
//        val url=baseUrl.format(section,page)
//        Log.e("Simsim",url)
//        val (_,response,result)=url.httpGet().awaitByteArrayResponseResult()
//        result.fold(
//            {
//            ret= String(it)
//            },
//            {
//                Log.e("Simsim",it.toString())
//            }
//        )
//        return ret
//    }
//    suspend fun getXmlStringByPage(section: String, page: Int): String? {
//        var ret: String? = null
//        val url = baseUrl.format(section, page)
//
//    }
}