package com.simsim.island.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.simsim.island.R
import com.simsim.island.adapter.MainLoadStateAdapter.*

class MainLoadStateAdapter(private val retry:()->Unit) :LoadStateAdapter<MainLoadStateViewHolder>(){
    inner class MainLoadStateViewHolder(private val view: View):RecyclerView.ViewHolder(view){
        val indicator: CircularProgressIndicator =view.findViewById(R.id.load_state_progress_indicator)
        val indicatorTextView:TextView=view.findViewById(R.id.load_state_indicator_textview)
    }

    override fun onBindViewHolder(holder: MainLoadStateViewHolder, loadState: LoadState) {
        holder.let {
            when(loadState){
                is LoadState.Loading->{
                    it.indicator.isVisible=true
                    it.indicator.show()
                }
                is LoadState.Error->{
                    it.indicatorTextView.isVisible=true
                    it.indicatorTextView.text="未能到岸，请重试"
                    it.indicatorTextView.setOnClickListener {
                        retry.invoke()
                    }
                }
                else -> {}
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): MainLoadStateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.main_load_state_viewholder,parent,false)
        return MainLoadStateViewHolder(view)
    }
}