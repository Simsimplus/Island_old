package com.simsim.island.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simsim.island.MainActivity
import com.simsim.island.R
import com.simsim.island.adapter.MainRecyclerViewAdapter.*
import com.simsim.island.databinding.MainRecyclerviewViewholderBinding
import com.simsim.island.model.IslandThread
import com.simsim.island.ui.main.MainFragment
import com.simsim.island.util.handleThreadId
import com.simsim.island.util.handleThreadTime

class MainRecyclerViewAdapter(private val fragment:MainFragment,private val clickListener: (mainThread:IslandThread)->Unit):PagingDataAdapter<IslandThread, IslandThreadViewHolder>(diffComparator) {
    inner class IslandThreadViewHolder(view: View):RecyclerView.ViewHolder(view){
        val binding=MainRecyclerviewViewholderBinding.bind(view)
//        val uidTextview:TextView=view.findViewById(R.id.uid_textview)
//        val timeTextview:TextView=view.findViewById(R.id.time_textview)
//        val threadIdTextview:TextView=view.findViewById(R.id.thread_id_textview)
//        val contentTextview:TextView=view.findViewById(R.id.content_textview)
//        val imagePosted:ImageView=view.findViewById(R.id.image_posted)
//        val commentNumber:TextView=view.findViewById(R.id.comment_number)
    }

    override fun onBindViewHolder(holder: IslandThreadViewHolder, position: Int) {
        val thread=getItem(position)
        thread?.let { islandThread ->
            holder.binding.uidTextview.text= handleThreadId(islandThread.poThread.uid)
            holder.binding.timeTextview.text= handleThreadTime(islandThread.poThread.time)
            holder.binding.threadIdTextview.text= islandThread.poThread.ThreadId
            holder.binding.contentTextview.text=islandThread.poThread.content
            holder.binding.commentNumber.text=islandThread.commentsNumber
            if (islandThread.poThread.imageUrl.isNotBlank()){
                val imageUrl=islandThread.poThread.imageUrl
//                val imagePosted=holder.binding.imagePosted
                Glide.with(holder.itemView).load(imageUrl).into(holder.binding.imagePosted)
                holder.binding.imagePosted.visibility=View.VISIBLE
                holder.binding.imagePosted.setBackgroundResource(R.drawable.image_shape)
                holder.binding.imagePosted.clipToOutline=true
                holder.binding.imagePosted.setOnClickListener {
                    (fragment.requireActivity() as MainActivity).showImageDetailFragment(imageUrl.replace("thumb","image"))
                }
            }else{
                Glide.with(fragment).clear(holder.binding.imagePosted)
                holder.binding.imagePosted.visibility=View.GONE
            }
            holder.binding.mdCard.setOnClickListener {
                clickListener.invoke(islandThread)
            }
//            holder.uidTextview.text= handleThreadId(islandThread.poThread.uid)
//            holder.timeTextview.text= handleThreadTime(islandThread.poThread.time)
//            holder.threadIdTextview.text= islandThread.poThread.ThreadId
//            holder.contentTextview.text=islandThread.poThread.content
//            holder.commentNumber.text=islandThread.commentsNumber
//            if (islandThread.poThread.imageUrl.isNotBlank()){
//                Glide.with(fragment).load(islandThread.poThread.imageUrl).into(holder.imagePosted)
//                holder.imagePosted.visibility=View.VISIBLE
//                holder.imagePosted.setBackgroundResource(R.drawable.image_shape)
//                holder.imagePosted.clipToOutline=true
//            }
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
                return oldItem.poThread.ThreadId==newItem.poThread.ThreadId
            }

        }
    }
}