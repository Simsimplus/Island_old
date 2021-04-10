package com.simsim.island.ui.main

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.bumptech.glide.Glide
import com.simsim.island.database.IslandDatabase
import com.simsim.island.model.BasicThread
import com.simsim.island.model.PoThread
import com.simsim.island.model.StaredPoThreads
import com.simsim.island.paging.DetailPaging
import com.simsim.island.paging.DetailRemoteMediator
import com.simsim.island.paging.MainRemoteMediator
import com.simsim.island.repository.AislandRepo
import com.simsim.island.sectionDataStore
import com.simsim.island.service.AislandNetworkService
import com.simsim.island.util.LOG_TAG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repo: AislandRepo,
    val database: IslandDatabase,
    private val networkService: AislandNetworkService
) : AndroidViewModel(application) {
    //    private val database:IslandDatabase= IslandDatabase.newInstance(application.applicationContext)
    internal val currentSection = MutableLiveData<String>("综合版1")
    var currentPoThread: PoThread? = null
    var mainFlow: Flow<PagingData<PoThread>> = setMainFlow("综合版1")
    var detailFlow: Flow<PagingData<BasicThread>> = emptyFlow()
    val isMainFragment = MutableLiveData(true)
    var windowHeight = 1
    var actionBarHeight = 1
    var refreshMainRecyclerView: MutableLiveData<Boolean> = MutableLiveData()
    private val glide = Glide.get(application.applicationContext)

    private val dataStore=application.applicationContext.sectionDataStore
    suspend fun getSectionLocal()=
        dataStore.data.catch {
            if (it is IOException) {
                it.printStackTrace()
                emit(emptyPreferences())
            } else {
                throw it
            }
        }.map {
            it[stringPreferencesKey("sectionList")]?:""
        }


    suspend fun getSectionList():Flow<String>{
        val sectionList=repo.getSectionList().map {
            Uri.decode(it.replace("/f/".toRegex(), ""))
        }
//        dataStore.edit {
//            it[stringPreferencesKey("sectionList")]=sectionList.toList(mutableListOf()).joinToString("|")
//        }
        Log.e(LOG_TAG,sectionList.toList(mutableListOf()).joinToString("|"))
        return sectionList
    }


    @OptIn(ExperimentalPagingApi::class)
    fun setMainFlow(section: String): Flow<PagingData<PoThread>> {
        mainFlow = Pager(
            PagingConfig(
                pageSize = 50,
                enablePlaceholders = false,
                maxSize = 9999,
                initialLoadSize = 150,
            ),
            remoteMediator = MainRemoteMediator(
                service = networkService,
                section = section,
                database = database
            )
        ) {
            database.threadDao().getAllPoThreadsBySection(section)
        }.flow
//            .map{ pagingData ->
//            pagingData.map {
//                it.replyThreads=database.threadDao().getAllReplyThreads(it.threadId)
//                it
//            }
//        }
            .cachedIn(viewModelScope)
        return mainFlow
    }

    @OptIn(ExperimentalPagingApi::class)
    fun setDetailFlow(poThread: PoThread): Flow<PagingData<BasicThread>> {
        detailFlow=Pager(
            PagingConfig(
                pageSize = 50,
                enablePlaceholders = false,
                maxSize = 999999,
                initialLoadSize = 150
            ),
            remoteMediator = DetailRemoteMediator(service = networkService,poThread = poThread,database=database)
        ){
            database.threadDao().getAllReplyThreads(poThreadId = poThread.threadId)
        }.flow
            .cachedIn(viewModelScope)
        return detailFlow
    }


    fun starPoThread(poThreadId: Long) {
        viewModelScope.launch {
//            if (currentPoThread!=null) {
//                val poThread= currentPoThread!!
//                if (poThread.threadId==poThreadId){
//                    val isStar=!poThread.isStar
//                    poThread.isStar=isStar
//                    database.threadDao().updatePoThread(poThread = poThread)
//                }
//            }
            val poThread = database.threadDao().getPoThread(poThreadId)
            poThread?.let {
                val staredPoThreads=StaredPoThreads(poThreadId=it.threadId)
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