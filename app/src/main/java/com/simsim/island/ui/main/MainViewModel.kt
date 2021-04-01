package com.simsim.island.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.simsim.island.model.IslandThread
import com.simsim.island.paging.IslandMainPaging
import com.simsim.island.repository.AislandRepo
import com.simsim.island.service.AislandNetworkService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(application: Application,private val repo: AislandRepo) : AndroidViewModel(application) {
    private val _threadsResult=repo.threadLiveData
    val threadsResult:LiveData<List<IslandThread>?>
    get() = _threadsResult
    internal val currentSection=MutableLiveData("综合版1")
    var flow: Flow<PagingData<IslandThread>> =setFlow("综合版1")
    fun setFlow(section: String): Flow<PagingData<IslandThread>> {
        flow= Pager(
        PagingConfig(
            pageSize = 10,
            enablePlaceholders = false,
            maxSize = 300,
        )
    ){
        IslandMainPaging(AislandNetworkService(),section = section)
    }.flow.cachedIn(viewModelScope)
        return flow
    }





    fun getThreads(section:String="综合版1",page:Int=1){
        viewModelScope.launch {
            repo.getThreadsByPage(Uri.encode(section),page)
        }
    }

}