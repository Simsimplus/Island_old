package com.simsim.island.service

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.room.withTransaction
import com.simsim.island.database.IslandDatabase
import com.simsim.island.model.Cookie
import com.simsim.island.model.DetailRemoteKey
import com.simsim.island.model.ReplyThread
import com.simsim.island.repository.AislandRepo
import com.simsim.island.util.LOG_TAG
import com.simsim.island.util.findPageNumber
import com.simsim.island.util.toBasicThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.jsoup.Jsoup
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class AislandNetworkService @Inject constructor(
    private val database: IslandDatabase
) {
    companion object {
        private const val baseUrl = "https://adnmb3.com"
    }
    var cookieInUse:String?=null
    init {
        CoroutineScope(Dispatchers.IO).launch {
            database.cookieDao().getActiveCookieFlow().collectLatest {
                it?.let { cookie ->
                    cookieInUse=cookie.cookie
                    Log.e(LOG_TAG,"current cookie:$cookieInUse")
                }
            }
        }
    }
    val logging = HttpLoggingInterceptor().apply {
        level=HttpLoggingInterceptor.Level.BODY
    }
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val retrofit = Retrofit.Builder().client(okHttpClient).baseUrl(baseUrl)
        .addConverterFactory(ScalarsConverterFactory.create()).build()

    interface IslandHtmlService {
        @GET
        suspend fun getHtmlStringByPage(@Url url: String,@HeaderMap() header: HashMap<String, String> ): Response<String>

        @Multipart
        @POST
        suspend fun postNewThread(
            @Url url: String,
            @HeaderMap cookie: HashMap<String, String>,
            @PartMap formData: HashMap<String, RequestBody>,
            @Part image:MultipartBody.Part?
        ): Response<String>
    }

    private val service: IslandHtmlService by lazy { retrofit.create(IslandHtmlService::class.java) }

    suspend fun getHtmlStringByPage(url: String,cookie: String?=null): String? = withContext(Dispatchers.IO) {
        val header=hashMapOf(
            "Cookie" to (cookie?:"userhash=${cookieInUse?:""}"),
            "Host" to "adnmb3.com",
            "referer" to "https://adnmb3.com/Forum"
        )
        val response: String?=
        try {
            val call = service.getHtmlStringByPage(url,header)
            Log.e("Simsim:url:", url)
            if (call.isSuccessful) {
                call.body()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "retrofit exception:$e")
            throw e
        }
        response
    }

    suspend fun getReplyThreadsByPage(page: Int,poThreadId: Long,saveDataToDBHere:Boolean=false):Pair<List<ReplyThread>?,Int?> = withContext(Dispatchers.IO){
        val url = "https://adnmb3.com/t/$poThreadId?page=$page"
        var maxPage:Int=999
        val threadList = getHtmlStringByPage(url)?.let{response->
            val doc = Jsoup.parse(response)
            val poThreadDiv = doc.selectFirst("div[class=h-threads-item-main]")
            val pages = doc.select("[href~=.*page=[0-9]+]")
            maxPage = try {
                pages.map { pageTag->
                    pageTag.attr("href").findPageNumber().toInt()
                }.maxOrNull()?:999
            } catch (e: Exception) {
                999
            }
            poThreadDiv?.let {
                val poThread= AislandRepo.basicThreadToPoThread(
                    AislandRepo.divToBasicThread(
                        poThreadDiv,
                        section = "",
                        poThreadId = poThreadId,
                        isPo = true
                    ),
                    mutableListOf(), 999
                ).also {
                    database.threadDao().insertPoThread(it)
                    database.threadDao().getPoThread(poThreadId)?.let { poThread ->
                        database.threadDao().updatePoThread(poThread.apply {
                            this.maxPage=maxPage?:0
                            try {
                                this.commentsNumber= Integer.max(
                                    database.threadDao().countReplyThreads(poThreadId) - 1,
                                    this.commentsNumber.toInt()
                                ).toString()
                                Log.e(LOG_TAG,"update poThread comment number :${this.commentsNumber}")
                            }catch (e:Exception){
                                Log.e(LOG_TAG,"update poThread comment number failed:${e.stackTraceToString()}")
                            }
                        })
                    }
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
//                        Log.e(LOG_TAG, "poUid=${poThread.uid},${replyThread}")
                }
//                replyThreads.removeIf {
//                    it.uid.isBlank()
//                }
                replyThreads
            }
        }
        if (saveDataToDBHere && !threadList.isNullOrEmpty()){
            CoroutineScope(Dispatchers.IO).launch {
                database.withTransaction {
                    val endOfPaginationReached = if (page >= maxPage) {
                        when {
                            threadList.isEmpty() -> true
                            threadList.count() == 1 && threadList[0].replyThreadId == 9999999L -> true
                            else -> false
                        }
                    } else {
                        false
                    }
                    val previousKey = if (page == 1) null else page - 1
                    val nextKey = if (threadList.size < 19) {
                        page
                    } else {
                        if (endOfPaginationReached) maxPage else page + 1
                    }
                    val keys = threadList.map {
                        val key = DetailRemoteKey(it.replyThreadId, previousKey, nextKey)
//                        database.keyDao().insertDetailKey(key)
                        key
                    }
                    database.keyDao().insertDetailKeys(keys)
                    database.threadDao().insertAllReplyThreads(threadList)
                }
            }
        }
        Pair(threadList,maxPage)
    }

    suspend fun doReply(
//        cookie: String,
        poThreadId: Long,
        content: String,
        image: InputStream?,
        imageType:String?,
        imageName:String?,
        waterMark: Boolean = false,
        name: String = "",
        email: String = "",
        title: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val response: Boolean = try {
            val formData = hashMapOf<String, RequestBody>()
            formData["resto"] =
                poThreadId.toString().toRequestBody(null)
            formData["content"] = content.toRequestBody(null)
            val imageDataMultipart=image?.let { img ->
                val imageDataPart = img.use{
                    val bytes=it.readBytes()
                    bytes
                }.toRequestBody(imageType?.toMediaTypeOrNull())
                if (waterMark){
                    formData["water"] =
                        "true".toRequestBody("text/plain".toMediaTypeOrNull())
                }

                MultipartBody.Part.createFormData("image","nmb.${MimeTypeMap.getSingleton().getExtensionFromMimeType(imageType)?:"jpg"}",imageDataPart)
            }
                formData["name"] = name.toRequestBody("text/plain".toMediaTypeOrNull())


                formData["title"] = title.toRequestBody("text/plain".toMediaTypeOrNull())


                formData["email"] = email.toRequestBody("text/plain".toMediaTypeOrNull())

            val headers=hashMapOf("cookie" to "userhash=${cookieInUse?:""}")
            val call = service.postNewThread(
                "https://adnmb3.com/Home/Forum/doReplyThread.html",
                headers,
                formData,
                imageDataMultipart
            )
            Log.e(LOG_TAG,"image type:$imageType")
            Log.e(LOG_TAG,"cookie:${cookieInUse?:""}")
            if (call.isSuccessful) {
                Log.e(LOG_TAG, "do reply:${call.body()}")
                call.body()?.let {
                    it.contains("成功")
                }?:false
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "retrofit exception:$e")
            throw e
        }
        response
    }

    suspend fun doPost(
//        cookie: String,
        content: String,
        image: InputStream?,
        imageType:String?,
        imageName:String?,
        fId: String,
        waterMark: Boolean = false,
        name: String = "",
        email: String = "",
        title: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val response: Boolean = try {
            val formData = hashMapOf<String, RequestBody>()
            formData["fid"] = fId.toRequestBody("text/plain".toMediaTypeOrNull())
//            formData["resto"]= RequestBody.create(MediaType.parse("text/plain"),poThreadId.toString())
            formData["content"] = content.toRequestBody("text/plain".toMediaTypeOrNull())
            val imageDataMultipart=image?.let { img ->
                val imageDataPart = img.use{
                    val bytes=it.readBytes()
                    bytes
                }.toRequestBody(imageType?.toMediaTypeOrNull())
                if (waterMark){
                    formData["water"] =
                        "true".toRequestBody("text/plain".toMediaTypeOrNull())
                }
                MultipartBody.Part.createFormData("image","nmb.${MimeTypeMap.getSingleton().getExtensionFromMimeType(imageType)?:"jpg"}",imageDataPart)
            }

                formData["name"] = name.toRequestBody("text/plain".toMediaTypeOrNull())


                formData["title"] = title.toRequestBody("text/plain".toMediaTypeOrNull())


                formData["email"] = email.toRequestBody("text/plain".toMediaTypeOrNull())

            val headers=hashMapOf("cookie" to "userhash=$${cookieInUse?:""}")
            val call = service.postNewThread(
                "https://adnmb3.com/Home/Forum/doPostThread.html",
                headers,
                formData,
                imageDataMultipart,
            )
            Log.e(LOG_TAG,"cookie:${cookieInUse?:""}")
//            Log.e("Simsim:url:", url)
            if (call.isSuccessful) {
                Log.e(LOG_TAG, "do post:${call.body()}")
                call.body()?.let {
                    it.contains("成功")
                }?:false
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "retrofit exception:$e")
            throw e
        }
        response
    }
    suspend fun getCookies(cookieMap:Map<String,String>):List<Cookie> = withContext(Dispatchers.IO){
        val cookie=cookieMap.toString().replace(", ","; ").replace("[{}]".toRegex(),"")
        val response= getHtmlStringByPage(url = "https://adnmb3.com/Member/User/Cookie/index.html",cookie =cookie )
        response?.let { r->
            val doc=Jsoup.parse(r)
            val cookieList= mutableListOf<Cookie>()
            doc.select("a[href~=/Member/User/Cookie/export/id/.*]").forEach { cookiePageTag->
                val (cookieName,cookieValue) = getCookieFromPage(cookiePageTag.attr("href"),cookie)
                cookieValue?.let {
                    cookieList.add(Cookie(cookie=cookieValue,name=cookieName))
                }
            }
            cookieList
        }?: listOf()
    }
    suspend fun getCookieFromPage(url:String,cookie: String):Pair<String,String?> = withContext(Dispatchers.IO){
        val response= getHtmlStringByPage(url = url,cookie =cookie )
        response?.let { r->
            val doc=Jsoup.parse(r)
            val cookieName=doc.selectFirst("div[class=tpl-form-maintext]")?.ownText()?:""
            val cookieValueRaw=doc.selectFirst("img[src~=http://publicapi.ovear.info.*]")?.attr("src")
            val cookieValue=cookieValueRaw?.let { raw->
                val rawDecode= Uri.decode(raw).replace("""[{}"]""".toRegex(),"").replaceBeforeLast("text=","")
                rawDecode.split(":")[1]
            }
            cookieName to cookieValue
        }?:"" to ""
    }


//    suspend fun doReplyWithFuel(
//        cookie: String,
//        poThreadId: Long,
//        content: String,
//        image: InputStream?,
//        imageType:String?,
//        imageName:String?,
//        waterMark: Boolean = false,
//        name: String = "",
//        email: String = "",
//        title: String = ""
//    ): Boolean = withContext(Dispatchers.IO) {
//        val water = if (waterMark) "true" else "false"
//        val (request, response, result) = if (image != null) {
//            Fuel
//                .upload(
//                    path = "https://adnmb3.com/Home/Forum/doReplyThread.html",
//                    method = Method.POST,
////                    parameters = listOf(
////                        "resto" to poThreadId.toString(),
////                        "content" to content,
////                        "water" to water,
////                        "name" to name,
////                        "email" to email,
////                        "title" to title,
////                    )
//                )
//                .add(
//                    BlobDataPart(
//                        inputStream = image,
//                        name = "image",
//                        filename = "nmb.${
//                            MimeTypeMap.getSingleton().getExtensionFromMimeType(imageType) ?: "jpg"
//                        }",
//                        contentLength = null,
//                        contentType = "application/octet-stream",
//                    ),
//                    InlineDataPart(poThreadId.toString(), "resto"),
//                    InlineDataPart(content, "content"),
//                    InlineDataPart(water, "water"),
//                    InlineDataPart(name, "name"),
//                    InlineDataPart(email, "email"),
//                    InlineDataPart(title, "title"),
//                )
//                .header("cookie", "userhash=$cookie")
//                .awaitByteArrayResponseResult()
//        } else {
//            Fuel
//                .upload(
//                    path = "https://adnmb3.com/Home/Forum/doReplyThread.html",
//                    method = Method.POST,
////                    parameters = listOf(
////                        "content" to content,
////                        "water" to waterMark,
////                        "name" to name,
////                        "email" to email,
////                        "title" to title,
////                    )
//                )
//                .add(
//                    InlineDataPart(poThreadId.toString(), "resto"),
//                    InlineDataPart(content, "content"),
//                    InlineDataPart(water, "water"),
//                    InlineDataPart(name, "name"),
//                    InlineDataPart(email, "email"),
//                    InlineDataPart(title, "title"),
//                )
//                .header("cookie", "userhash=$cookie")
//                .awaitByteArrayResponseResult()
//        }
//        Log.e(LOG_TAG,"request with fuel:${request.body}")
//        result.fold(
//            {
//                val responseBody=String(it)
//                Log.e(LOG_TAG, "{response with fuel:$responseBody}")
//                responseBody.contains("回复成功")
//            },
//            {
//                Log.e(LOG_TAG,"fuel error ${it.stackTraceToString()}")
//                false
//            }
//        )
//
//    }
//
//    suspend fun doPostWithFuel(
//        cookie: String,
//        content: String,
//        image: InputStream?,
//        imageType:String?,
//        imageName:String?,
//        fId: String,
//        waterMark: Boolean = false,
//        name: String = "",
//        email: String = "",
//        title: String = ""
//    ): Boolean = withContext(Dispatchers.IO) {
//        val water = if (waterMark) "true" else "false"
//        val (request, response, result) = if (image!=null){
//            Fuel
//                .upload(
//                    path = "https://adnmb3.com/Home/Forum/doPostThread.html",
//                    method = Method.POST,
////                    parameters = listOf(
////                        "fid" to fId,
////                        "content" to content,
////                        "water" to water,
////                        "name" to name,
////                        "email" to email,
////                        "title" to title,
////                    )
//                )
//                .add(BlobDataPart(
//                    inputStream = image,
//                    name = "image",
//                    filename = "nmb.${MimeTypeMap.getSingleton().getExtensionFromMimeType(imageType)?:"jpg"}",
//                    contentLength = null,
//                    contentType = "application/octet-stream",
//                ),
//                    InlineDataPart(fId,"fid"),
//                    InlineDataPart(content,"content"),
//                    InlineDataPart(water,"water"),
//                    InlineDataPart(name,"name"),
//                    InlineDataPart(email,"email"),
//                    InlineDataPart(title,"title"),
//                )
//                .header("cookie", "userhash=$cookie")
//                .awaitByteArrayResponseResult()
//        }else{
//            Fuel
//                .upload(
//                    path = "https://adnmb3.com/Home/Forum/doReplyThread.html",
//                    method = Method.POST,
////                    parameters = listOf(
////                        "content" to content,
////                        "water" to waterMark,
////                        "name" to name,
////                        "email" to email,
////                        "title" to title,
////                    )
//                )
//                .add(
//                    InlineDataPart(fId,"fid"),
//                    InlineDataPart(content,"content"),
//                    InlineDataPart(water,"water"),
//                    InlineDataPart(name,"name"),
//                    InlineDataPart(email,"email"),
//                    InlineDataPart(title,"title"),
//                )
//                .header("cookie", "userhash=$cookie")
//                .awaitByteArrayResponseResult()
//        }
//        Log.e(LOG_TAG,"request with fuel:${request.headers}")
//        result.fold(
//            {
//                val responseBody=String(it)
//                Log.e(LOG_TAG, "{response with fuel:$responseBody}")
//                responseBody.contains("回复成功")
//            },
//            {
//                Log.e(LOG_TAG,"fuel error ${it.stackTraceToString()}")
//                false
//            }
//        )
//    }
}