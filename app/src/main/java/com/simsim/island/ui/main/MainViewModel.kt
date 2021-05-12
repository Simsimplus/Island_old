package com.simsim.island.ui.main

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.bumptech.glide.Glide
import com.simsim.island.model.*
import com.simsim.island.repository.AislandRepo
import com.simsim.island.util.LOG_TAG
import com.simsim.island.util.toSavedPoThread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repo: AislandRepo,
) : AndroidViewModel(application) {
    var currentPoThread: PoThread? = null
    var currentSectionId: String = ""
    var currentSectionName: String? = null
    val savedInstanceState = MutableLiveData<Long>()
    var currentReplyThreads = mutableListOf<ReplyThread>()
    var sectionList = repo.getAllSectionFlow()
    var mainFlow: Flow<PagingData<PoThread>> = emptyFlow()
    var currentPoThreadList = MutableLiveData<List<PoThread>>()
    var detailFlow: Flow<PagingData<ReplyThread>> = emptyFlow()
    val isMainFragment = MutableLiveData(true)
    var windowHeight = 1
    var actionBarHeight = 1
    private val glide: Glide by lazy { Glide.get(application.applicationContext) }
    val loginCookies = MutableLiveData<Map<String, String>>()
    val loadCookieFromUserSystemSuccess = MutableLiveData<Boolean>()
    private var blockRules: List<BlockRule> = mutableListOf()
    var cameraTakePictureSuccess = MutableLiveData<Boolean>()
    var picturePath = MutableLiveData<String>()
    var pictureUri = MutableLiveData<Uri>()
    val drawPicture = MutableLiveData<Bitmap>()
    var shouldTakePicture = MutableLiveData<String>()
    var successPostOrReply = MutableLiveData<Boolean?>()
    var errorPostOrReply = MutableLiveData<String?>()
    val QRcodeResult = MutableLiveData<String>()
    val savedPoThreadDataSetChanged = MutableLiveData<Boolean>()

    init {
        doUpdate()
        updateBlockRules()


    }

    private fun updateBlockRules() {
        viewModelScope.launch {
            blockRules = repo.getAllBlockRules()
            repo.getAllBlockRulesFlow().collectLatest {
                blockRules = it
            }
        }
    }
    suspend fun insertBlockRule(blockRule: BlockRule):Long{
            return repo.insertBlockRule(blockRule)
    }
    suspend fun getAllPoThreadsByUid(uid:String)=
        repo.getAllPoThreadsByUid(uid)


    fun doUpdate() {
        viewModelScope.launch {
            val record = repo.getRecord()
            if (record == null) {
                getSectionList()
                updateUpdateRecord()
            } else {
                if (Duration.between(record.lastUpdateTime, LocalDateTime.now()).toDays() >= 30) {
                    getSectionList()
                }
            }
        }
    }

    private suspend fun updateUpdateRecord() {
        repo.updateUpdateRecord()
    }


    private suspend fun getSectionList() {
        repo.getSectionAndEmojiList()
    }


    @OptIn(ExperimentalPagingApi::class)
    fun setMainFlow(sectionName: String, sectionUrl: String): Flow<PagingData<PoThread>> {
        mainFlow = Pager(
            PagingConfig(
                pageSize = 20,
                prefetchDistance = 20,
                enablePlaceholders = true,
                maxSize = 9999,
                initialLoadSize = 20,
            ),
            remoteMediator = repo.getMainRemoteMediator(sectionName = sectionName,sectionUrl=sectionUrl)
        ) {
            repo.getAllPoThreadsBySection(sectionName)
        }.flow
            .map { pagingData ->
                val list = mutableListOf<PoThread>()
                pagingData.map {
                    list.add(it)
                    currentSectionName = it.section
                    it
                }
                currentPoThreadList.value = list
                pagingData.filter { poThread ->
                    //return false if thread match any rule
                    !blockRules
                        .map { rule ->
                            ifThreadMatchRules(poThread, rule)
                        }
                        .contains(true)
                }
            }
            .cachedIn(viewModelScope)
        return mainFlow
    }

    private fun ifThreadMatchRules(poThread: PoThread, blockRule: BlockRule): Boolean {
        if (!blockRule.isEnable) {
            return false
        }
        val options = mutableSetOf<RegexOption>()
        blockRule.run {
            if (!isRegex) {
                options.add(RegexOption.LITERAL)
            }
            if (isNotCaseSensitive) {
                options.add(RegexOption.IGNORE_CASE)
            }
        }
        val ruleRegex = try {
            Regex(blockRule.rule,options)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "transform rule to regex failed:${e.stackTraceToString()}")
            Regex.fromLiteral(blockRule.rule)
        }

        return when (blockRule.target) {
            BlockTarget.TargetUid -> {
                ifMatchEntire(ruleRegex,poThread.uid,blockRule.matchEntire)
            }
            BlockTarget.TargetContent -> {
                ifMatchEntire(ruleRegex,poThread.content,blockRule.matchEntire)
            }
            BlockTarget.TargetSection -> {
                ifMatchEntire(ruleRegex,poThread.section,blockRule.matchEntire)
            }
            BlockTarget.TargetThreadId -> {
                ifMatchEntire(ruleRegex,poThread.threadId.toString(),blockRule.matchEntire)
            }
            else -> {
                listOf(
                    ifMatchEntire(ruleRegex,poThread.uid,blockRule.matchEntire),
                    ifMatchEntire(ruleRegex,poThread.content,blockRule.matchEntire),
                    ifMatchEntire(ruleRegex,poThread.section,blockRule.matchEntire),
                    ifMatchEntire(ruleRegex,poThread.threadId.toString(),blockRule.matchEntire)
                ).contains(true)

            }
        }
    }
    private fun ifMatchEntire(re:Regex,input:String,matchEntire:Boolean):Boolean=if (matchEntire) re.matches(input) else re.containsMatchIn(input)



    @OptIn(ExperimentalPagingApi::class)
    fun setDetailFlow(poThreadId: Long,initialPage:Int=1,localBlockRule:(ReplyThread)->Boolean={true},onlyPo:Boolean=false): Flow<PagingData<ReplyThread>> {
        currentReplyThreads = mutableListOf()
        detailFlow = Pager(
            PagingConfig(
                pageSize = 50,
                prefetchDistance = 100,
                enablePlaceholders = false,
                maxSize = 999999,
                initialLoadSize = 200
            ),
            remoteMediator = repo.getDetailRemoteMediator(
                poThreadId=poThreadId,
                initialPage=initialPage,
                onlyPo=onlyPo
            )
        ) {
            repo.getAllReplyThreadsPagingSource(poThreadId)
        }.flow.map { pagingData ->
            pagingData.filter {
                currentReplyThreads.add(it)
                localBlockRule(it)
            }
        }
            .cachedIn(viewModelScope)
        return detailFlow
    }

    fun setSavedPoThreadFlow():Flow<PagingData<PoThread>>{
        return Pager(
            PagingConfig(
                pageSize = 20,
                prefetchDistance = 20,
                enablePlaceholders = true,
                maxSize = 999999,
                initialLoadSize = 20
            ),
        ){
            repo.savedPoThreadPagingSource
        }.flow
            .cachedIn(viewModelScope)
    }
    fun setSavedReplyThreadFlow(poThread: PoThread):Flow<PagingData<ReplyThread>>{
        return Pager(
            PagingConfig(
                pageSize = 20,
                prefetchDistance = 20,
                enablePlaceholders = true,
                maxSize = 999999,
                initialLoadSize = 20
            ),
        ){
            repo.getSavedReplyThreadPagingSource(poThread =poThread )
        }.flow
            .cachedIn(viewModelScope)
    }


    fun updatePoThread(poThread: PoThread) {
        viewModelScope.launch {
            repo.updatePoThread(poThread)
        }
    }
    fun starPoThread(poThreadId: Long) {
        viewModelScope.launch {
            if (repo.isPoThreadStared(poThreadId)){
                repo.deleteSavedPoThread(poThreadId)
            }else{
                val poThread = getPoThread(poThreadId)
                poThread?.let {
                    val staredPoThreads = poThread.toSavedPoThread()
                    repo.insertSavedPoThread(staredPoThreads)
                    saveReplyThreads(poThreadId)
                    viewModelScope.launch {
                        val (pageStart,pageEnd)=repo.getCurrentPreviousNextPage()
                        val maxPage=poThread.maxPage
                        val minPage=1
                        launch {
                            launch {
                                if (pageStart>minPage){
                                    fetchAllReplyThreadByPageRange(poThreadId,minPage..pageStart)
                                }
                            }
                            launch {
                                if (pageEnd <= maxPage){
                                    fetchAllReplyThreadByPageRange(poThreadId,pageEnd..maxPage)
                                }
                            }

                        }


                    }
                }
            }
            savedPoThreadDataSetChanged.value=true
        }
    }
    fun isPoThreadStaredFlow(poThreadId: Long)=repo.isPoThreadStaredFlow(poThreadId)
    suspend fun getPoThread(poThreadId: Long) = repo.getPoThread(poThreadId)

    private suspend fun saveReplyThreads(poThreadId: Long) {
        viewModelScope.launch {
            repo.saveReplyThreads(poThreadId)
        }
    }

    private suspend fun fetchAllReplyThreadByPageRange(poThreadId: Long, pageRange:IntRange){
        pageRange.toMutableList().distinct().forEach { page->
            viewModelScope.launch {
                Log.e(LOG_TAG,"get threadList of threadId[$poThreadId] by page[$page]")
                repo.getReplyThreadsByPage(page, poThreadId)
            }
        }
    }


    fun doWhenDestroy() {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                Log.e(LOG_TAG, "vm do destroy work")
                glide.clearDiskCache()
//                threadDao.clearAllPoThread()
            }
            CoroutineScope(Dispatchers.Main).launch {
                Log.e(LOG_TAG, "vm do destroy work")
                glide.clearMemory()
            }

        }
    }
    suspend fun getReference(referenceId:Long):ReplyThread= withContext(viewModelScope.coroutineContext){
        repo.getReference(referenceId)
    }
    fun doPost(
//        cookie: String,
//        poThreadId: Long,
        content: String,
        image: InputStream?,
        imageType: String?,
        imageName: String?,
        fId: String,
        waterMark: Boolean = false,
        name: String = "",
        email: String = "",
        title: String = ""
    ) {
        viewModelScope.launch {

            val result = repo.doPost(
                content,
                image,
                imageType,
                imageName,
                fId,
                waterMark,
                name,
                email,
                title,
            )
            successPostOrReply.value=result.first
            errorPostOrReply.value= result.second
        }
    }

    fun doReply(
//        cookie: String,
        poThreadId: Long,
        content: String,
        image: InputStream?,
        imageType: String?,
        imageName: String?,
        waterMark: Boolean = false,
        name: String = "",
        email: String = "",
        title: String = ""
    ) {
        viewModelScope.launch {
            val result = repo.doReply(
//                cookie,
                poThreadId,
                content,
                image,
                imageType,
                imageName,
                waterMark,
                name,
                email,
                title,
            )
            successPostOrReply.value=result.first
            errorPostOrReply.value= result.second
        }
    }

    fun getCookiesFromUserSystem(cookieMap: Map<String, String>) {
        viewModelScope.launch {
            repo.getCookiesFromUserSystem(cookieMap)
            loadCookieFromUserSystemSuccess.value = true
        }
    }
    suspend fun getAllCookies()=repo.getAllCookies()
    suspend fun updateCookies(cookies:List<Cookie>)=repo.updateCookies(cookies)
    suspend fun deleteCookie(cookieValue:String)=repo.deleteCookie(cookieValue)
    fun isAnyCookieAvailable()=repo.isAnyCookieAvailable()
    suspend fun updateBlockRule(blockRule: BlockRule)=repo.updateBlockRule(blockRule)
    suspend fun deleteBlockRule(blockRule: BlockRule)=repo.deleteBlockRule(blockRule)
    fun getAllBlockRulesFlow()=repo.getAllBlockRulesFlow()
    suspend fun getBlockRule(blockRuleIndex: Long)=repo.getBlockRule(blockRuleIndex)
    suspend fun getSavedPoThread(threadId: Long)=repo.getSavedPoThread(threadId)
    suspend fun countSavedReplyThreads(threadId: Long)=repo.countSavedReplyThreads(threadId)
    fun getActiveCookieFlow()=repo.getActiveCookieFlow()
    suspend fun getCookieByValue(result: String)=repo.getCookieByValue(result)
    suspend fun insertCookie(newCookie: Cookie)=repo.insertCookie(newCookie)
    suspend fun getAllSavedPoThread()=repo.getAllSavedPoThread()
    suspend fun isAnyEmoji():Boolean=repo.isAnyEmoji()
    suspend fun insertAllEmojis(emojiList: List<Emoji>)=repo.insertAllEmojis(emojiList)
    suspend fun isAnySectionInDB(): Boolean =repo.isAnySectionInDB()
    suspend fun insertAllSection(sections: List<Section>)=repo.insertAllSection(sections)
    fun getAllSectionFlow()=repo.getAllSectionFlow()
    fun getAllEmojisFlow()=repo.getAllEmojisFlow()

    //
//
//    fun setMainFlow(section: String): Flow<PagingData<PoThread>> {
//        mainFlow= Pager(
//        PagingConfig(
//            pageSize = 50,
//            enablePlaceholders = false,
//            maxSize = 300,
//            initialLoadSize = 100
//        )
//    ){
//        MainPaging(networkService,section = section)
//    }.flow.cachedIn(viewModelScope)
//        return mainFlow
//    }
//    fun setDetailFlow(poThread: PoThread): Flow<PagingData<ReplyThread>> {
//        detailFlow = Pager(
//            PagingConfig(
//                pageSize = 100,
//                prefetchDistance = 100,
//                enablePlaceholders = false,
//                maxSize = 300,
//                initialLoadSize = 200
//            )
//        ) {
//            DetailPaging(networkService, poThread)
//        }.flow.cachedIn(viewModelScope)
//        return detailFlow
//    }


}