package com.simsim.island.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simsim.island.R
import com.simsim.island.adapter.MainRecyclerViewAdapter.*
import com.simsim.island.databinding.MainRecyclerviewViewholderBinding
import com.simsim.island.model.IslandThread
import com.simsim.island.util.handleThreadId
import com.simsim.island.util.handleThreadTime

class MainRecyclerViewAdapter():PagingDataAdapter<IslandThread, IslandThreadViewHolder>(diffComparator) {
    inner class IslandThreadViewHolder(private val view: View):RecyclerView.ViewHolder(view){
        val binding=MainRecyclerviewViewholderBinding.bind(view)
    }

    override fun onBindViewHolder(holder: IslandThreadViewHolder, position: Int) {
        val thread=getItem(position)
        thread?.let {
            holder.binding.uidTextview.text= handleThreadId(it.poThread.uid)
            holder.binding.timeTextview.text= handleThreadTime(it.poThread.time)
            holder.binding.threadIdTextview.text= it.poThread.ThreadId
            holder.binding.contentTextview.text=it.poThread.content
            holder.binding.commentNumber.text=it.commentsNumber
            if (it.poThread.imageUrl.isNotBlank()){
                Glide.with(holder.itemView).load(it.poThread.imageUrl).into(holder.binding.imagePosted)
                holder.binding.imagePosted.visibility=View.VISIBLE
                holder.binding.imagePosted.setBackgroundResource(R.drawable.image_shape)
                holder.binding.imagePosted.clipToOutline=true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IslandThreadViewHolder {
        val view=LayoutInflater.from(parent.context).inflate(R.layout.main_recyclerview_viewholder,parent,false)
        return IslandThreadViewHolder(view)
    }
    companion object{
        val diffComparator=object : DiffUtil.ItemCallback<IslandThread>() {
            override fun areItemsTheSame(oldItem: IslandThread, newItem: IslandThread): Boolean {
                return oldItem.poThread.ThreadId==newItem.poThread.ThreadId
            }

            override fun areContentsTheSame(oldItem: IslandThread, newItem: IslandThread): Boolean {
                return oldItem.poThread.content==newItem.poThread.content
            }

        }
    }
}