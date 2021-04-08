package com.simsim.island.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.simsim.island.database.IslandDatabase
import com.simsim.island.model.PoThread
import com.simsim.island.service.AislandNetworkService

@OptIn(ExperimentalPagingApi::class)
class MainRemoteMediator(private val service: AislandNetworkService, private val section: String, private val database: IslandDatabase) :RemoteMediator<Int, PoThread>(){
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PoThread>
    ): MediatorResult {
        TODO("Not yet implemented")
    }
}