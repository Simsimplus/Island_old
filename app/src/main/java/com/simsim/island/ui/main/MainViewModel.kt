package com.simsim.island.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.simsim.island.model.IslandThread
import com.simsim.island.repository.AislandRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(application: Application,private val repo: AislandRepo) : AndroidViewModel(application) {
    private val _threadsResult=repo.threadLiveData
    val threadsResult:LiveData<List<IslandThread>?>
    get() = _threadsResult


    fun getThreads(section:String="综合版1",page:Int=1){
        viewModelScope.launch {
            repo.getThreadsByPage(Uri.encode(section),page)
        }
    }

}