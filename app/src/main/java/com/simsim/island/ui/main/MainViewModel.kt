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
import com.simsim.island.R
import com.simsim.island.database.IslandDatabase
import com.simsim.island.model.*
import com.simsim.island.paging.DetailRemoteMediator
import com.simsim.island.paging.MainRemoteMediator
import com.simsim.island.repository.AislandRepo
import com.simsim.island.service.AislandNetworkService
import com.simsim.island.util.LOG_TAG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repo: AislandRepo,
    val database: IslandDatabase,
    val networkService: AislandNetworkService
) : AndroidViewModel(application) {
//    var randomLoadingImage: Int = R.drawable.ic_blue_ocean1
    val newSearchQuery = MutableLiveData<String>()
    var currentPoThread: PoThread? = null
    var currentSectionId: String = ""
    var currentSectionName: String? = null
    val savedInstanceState= MutableLiveData<Long>()
    var currentReplyThreads = mutableListOf<BasicThread>()
    var sectionList = database.sectionDao().getAllSection()
    var mainFlow: Flow<PagingData<PoThread>> = emptyFlow()
    var currentPoThreadList=MutableLiveData<List<PoThread>>()
    var detailFlow: Flow<PagingData<BasicThread>> = emptyFlow()
    val isMainFragment = MutableLiveData(true)
    var windowHeight = 1
    var actionBarHeight = 1
    private val glide = Glide.get(application.applicationContext)
    val loginCookies = MutableLiveData<Map<String,String>>()
    val loadCookieFromUserSystemSuccess=MutableLiveData<Boolean>()

    init {
        doUpdate()
//        randomLoadingImage()
    }

    var cameraTakePictureSuccess = MutableLiveData<Boolean>()
    var picturePath = MutableLiveData<String>()
    var pictureUri = MutableLiveData<Uri>()
    val drawPicture=MutableLiveData<Bitmap>()
    var shouldTakePicture = MutableLiveData<String>()
    var successReply=MutableLiveData<Boolean>()
    var successPost=MutableLiveData<Boolean>()
    val QRcodeResult=MutableLiveData<String>()

    fun doUpdate(){
        viewModelScope.launch {
            val record=database.recordDao().getRecord()
            if (record==null || !database.sectionDao().isAnySectionInDB()){
                getSectionList()
                database.recordDao().insertRecord(UpdateRecord(lastUpdateTime =  LocalDateTime.now()))
            }else{
                if (Duration.between(record.lastUpdateTime,LocalDateTime.now()).toDays()>=30){
                    getSectionList()
                }
            }
        }
    }


//    private fun randomLoadingImage() {
//
//        viewModelScope.launch {
//            while (true) {
//                delay(10000)
//                randomLoadingImage = when ((1..12).random()) {
//                    1 -> R.drawable.ic_blue_ocean1
//                    2 -> R.drawable.ic_blue_ocean2
//                    3 -> R.drawable.ic_blue_ocean3
//                    4 -> R.drawable.ic_blue_ocean4
//                    5 -> R.drawable.ic_blue_ocean5
//                    6 -> R.drawable.ic_blue_ocean6
//                    7 -> R.drawable.ic_blue_ocean7
//                    8 -> R.drawable.ic_blue_ocean8
//                    9 -> R.drawable.ic_blue_ocean9
//                    10 -> R.drawable.ic_blue_ocean10
//                    11 -> R.drawable.ic_blue_ocean11
//                    12 -> R.drawable.ic_blue_ocean12
//                    else -> R.drawable.image_load_failed
//                }
//                Log.e("Simsim", "get loading image id :$randomLoadingImage")
//            }
//        }
//    }

    private suspend fun getSectionList() {
        val sectionList = repo.getSectionList()
        val sections = sectionList.toList(mutableListOf())
        database.sectionDao().insertAllSection(sections)
    }


    @OptIn(ExperimentalPagingApi::class)
    fun setMainFlow(sectionName: String, sectionUrl:String): Flow<PagingData<PoThread>> {

        mainFlow = Pager(
            PagingConfig(
                pageSize = 20,
                prefetchDistance = 20,
                enablePlaceholders = true,
                maxSize = 9999,
                initialLoadSize = 20,
            ),
            remoteMediator = MainRemoteMediator(
                service = networkService,
                sectionName = sectionName,
                sectionUrl=sectionUrl,
                database = database
            )
        ) {
            database.threadDao().getAllPoThreadsBySection(sectionName)
        }.flow
            .map { pagingData ->
                val list= mutableListOf<PoThread>()
                pagingData.map {
                    list.add(it)
                    currentSectionName = it.section
                    it
                }
                currentPoThreadList.value=list
                pagingData
            }
            .cachedIn(viewModelScope)
        return mainFlow
    }

    @OptIn(ExperimentalPagingApi::class)
    fun setDetailFlow(poThreadId: Long): Flow<PagingData<BasicThread>> {
        currentReplyThreads = mutableListOf()
        detailFlow = Pager(
            PagingConfig(
                pageSize = 50,
                prefetchDistance = 100,
                enablePlaceholders = true,
                maxSize = 999999,
                initialLoadSize = 150
            ),
            remoteMediator = DetailRemoteMediator(
                service = networkService,
                poThreadId = poThreadId,
                database = database
            )
        ) {
            database.threadDao().getAllReplyThreads(poThreadId = poThreadId)
        }.flow.map { pagingData ->
            pagingData.map {
                currentReplyThreads.add(it)
                it
            }
        }
            .cachedIn(viewModelScope)
        return detailFlow
    }


    fun starPoThread(poThreadId: Long) {
        viewModelScope.launch {
            val poThread = database.threadDao().getPoThread(poThreadId)
            poThread?.let {
                val staredPoThreads = StaredPoThreads(poThreadId = it.threadId)
                database.threadDao().addStarRecord(staredPoThreads)
                val isStar = !poThread.isStar
                poThread.isStar = isStar
                database.threadDao().updatePoThread(poThread = poThread)
            }
        }
    }

    fun doWhenDestroy() {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                Log.e(LOG_TAG, "vm do destroy work")
                glide.clearDiskCache()
//                database.threadDao().clearAllPoThread()
            }
            CoroutineScope(Dispatchers.Main).launch {
                Log.e(LOG_TAG, "vm do destroy work")
                glide.clearMemory()
            }

//            database.keyDao().clearMainKeys()
//            database.keyDao().clearReplyThreadsKeys()
        }
    }

    fun doPost(
        cookie: String,
//        poThreadId: Long,
        content: String,
        image: InputStream?,
        imageType:String?,
        imageName:String?,
        fId: String,
        waterMark: Boolean = false,
        name: String = "",
        email: String = "",
        title: String = ""
    ) {
        viewModelScope.launch {
            successPost.value= networkService.doPost(
                cookie,
//                poThreadId,
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
        }
    }

    fun doReply(
        cookie: String,
        poThreadId: Long,
        content: String,
        image: InputStream?,
        imageType:String?,
        imageName:String?,
        waterMark: Boolean = false,
        name: String = "",
        email: String = "",
        title: String = ""
    ) {
        viewModelScope.launch {
            successReply.value=networkService.doReply(
                cookie,
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
        }
    }
    fun getCookies(cookieMap:Map<String,String>){
        viewModelScope.launch {
            val cookieList=networkService.getCookies(cookieMap)
            database.cookieDao().insertAllCookies(cookieList)
            loadCookieFromUserSystemSuccess.value=true
        }
    }

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
//    fun setDetailFlow(poThread: PoThread): Flow<PagingData<BasicThread>> {
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