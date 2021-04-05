package com.simsim.island.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.simsim.island.model.BasicThread
import com.simsim.island.model.IslandThread
import com.simsim.island.paging.IslandDetailPaging
import com.simsim.island.paging.IslandMainPaging
import com.simsim.island.repository.AislandRepo
import com.simsim.island.service.AislandNetworkService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(application: Application,private val repo: AislandRepo) : AndroidViewModel(application) {
    private val _threadsResult=repo.threadLiveData
    val threadsResult:LiveData<List<IslandThread>?>
    get() = _threadsResult
    internal val currentSection=MutableLiveData<String>("综合版1")
//    var currentSection="综合版1"
    var mainFlow: Flow<PagingData<IslandThread>> =setMainFlow("综合版1")
    var detailFlow:Flow<PagingData<BasicThread>> = emptyFlow()
    val isMainFragment=MutableLiveData(true)
    var windowHeight=1
    var actionBarHeight=1
    var refreshMainRecyclerView:MutableLiveData<Boolean> = MutableLiveData()
    private val networkService=AislandNetworkService()


    fun setDetailFlow(mainThread: IslandThread): Flow<PagingData<BasicThread>> {
        detailFlow=Pager(
            PagingConfig(
                pageSize = 10,
                enablePlaceholders = false,
                maxSize = 300,
                initialLoadSize = 10
            )
        ){
            IslandDetailPaging(networkService,mainThread)
        }.flow.cachedIn(viewModelScope)
        return detailFlow
    }



    fun setMainFlow(section: String): Flow<PagingData<IslandThread>> {
        mainFlow= Pager(
        PagingConfig(
            pageSize = 10,
            enablePlaceholders = false,
            maxSize = 300,
            initialLoadSize = 10
        )
    ){
        IslandMainPaging(networkService,section = section)
    }.flow.cachedIn(viewModelScope)
        return mainFlow
    }





//    fun getThreads(section:String="综合版1",page:Int=1){
//        viewModelScope.launch {
//            repo.getThreadsByPage(Uri.encode(section),page)
//        }
//    }

}