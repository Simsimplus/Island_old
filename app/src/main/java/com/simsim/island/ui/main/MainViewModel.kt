package com.simsim.island.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.bumptech.glide.Glide
import com.simsim.island.database.IslandDatabase
import com.simsim.island.model.BasicThread
import com.simsim.island.model.PoThread
import com.simsim.island.paging.DetailPaging
import com.simsim.island.paging.MainRemoteMediator
import com.simsim.island.repository.AislandRepo
import com.simsim.island.service.AislandNetworkService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(application: Application,
                                        private val repo: AislandRepo,
                                        val database:IslandDatabase,
                                        private val networkService: AislandNetworkService
                                        ) : AndroidViewModel(application) {
//    private val database:IslandDatabase= IslandDatabase.newInstance(application.applicationContext)
    internal val currentSection=MutableLiveData<String>("综合版1")
//    var currentSection="综合版1"
    var mainFlow: Flow<PagingData<PoThread>> =setMainFlow("综合版1")
    var detailFlow:Flow<PagingData<BasicThread>> = emptyFlow()
    val isMainFragment=MutableLiveData(true)
    var windowHeight=1
    var actionBarHeight=1
    var refreshMainRecyclerView:MutableLiveData<Boolean> = MutableLiveData()
    private val context=application.applicationContext
//    private val networkService=AislandNetworkService()






    @OptIn(ExperimentalPagingApi::class)
    fun setMainFlow(section: String): Flow<PagingData<PoThread>> {
        mainFlow= Pager(
            PagingConfig(
                pageSize = 50,
                enablePlaceholders = false,
                maxSize = 9999,
                initialLoadSize = 100,
            ),
            remoteMediator = MainRemoteMediator(service = networkService,section = section,
                database = database
            )
        ){
            database.threadDao().getAllPoThreadsBySection(section)
        }.flow.map{ pagingData ->
            pagingData.map {
                it.replyThreads=database.threadDao().getAllReplyThreads(it.ThreadId)
                it
            }
        }.cachedIn(viewModelScope)
        return mainFlow
    }

    fun setDetailFlow(poThread: PoThread): Flow<PagingData<BasicThread>> {
        detailFlow=Pager(
            PagingConfig(
                pageSize = 10,
                enablePlaceholders = false,
                maxSize = 300,
                initialLoadSize = 10
            )
        ){
            DetailPaging(networkService,poThread)
        }.flow.cachedIn(viewModelScope)
        return detailFlow
    }


    fun starPoThread(poThreadId:String){
        viewModelScope.launch {
           val poThread= database.threadDao().getPoThread(poThreadId)
            val isStar=!poThread.isStar
            poThread.isStar=isStar
            database.threadDao().updatePoThread(poThread = poThread)
        }
    }

    fun doWhenDestroy(){
        viewModelScope.launch {
            Glide.get(this@MainViewModel.context).clearDiskCache()
            Glide.get(this@MainViewModel.context).clearMemory()
            database.threadDao().clearAllPoThread()
            database.keyDao().clearMainKeys()
            database.keyDao().clearDetailKeys()
        }
    }
//
//
//    fun setMainFlow(section: String): Flow<PagingData<PoThread>> {
//        mainFlow= Pager(
//        PagingConfig(
//            pageSize = 10,
//            enablePlaceholders = false,
//            maxSize = 300,
//            initialLoadSize = 10
//        )
//    ){
//        MainPaging(networkService,section = section)
//    }.flow.cachedIn(viewModelScope)
//        return mainFlow
//    }






}