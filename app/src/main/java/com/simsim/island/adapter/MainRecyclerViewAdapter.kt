package com.simsim.island.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simsim.island.MainActivity
import com.simsim.island.R
import com.simsim.island.adapter.MainRecyclerViewAdapter.*
import com.simsim.island.databinding.MainRecyclerviewViewholderBinding
import com.simsim.island.model.BasicThread
import com.simsim.island.model.PoThread
import com.simsim.island.ui.main.MainFragment
import com.simsim.island.ui.main.MainFragmentDirections
import com.simsim.island.util.handleThreadId
import com.simsim.island.util.handleThreadTime

class MainRecyclerViewAdapter(private val fragment:MainFragment,private val imageClickListener: (imageUrl:String)->Unit,private val clickListener: (poThread:BasicThread)->Unit):PagingDataAdapter<PoThread, IslandThreadViewHolder>(diffComparator) {
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
        thread?.let {
            val poThread=thread.toBasicThread()
            holder.binding.uidTextview.text= handleThreadId(poThread.uid)
            holder.binding.timeTextview.text= handleThreadTime(poThread.time)
            holder.binding.threadIdTextview.text= poThread.ThreadId
            holder.binding.contentTextview.text=poThread.content
            holder.binding.commentNumber.text=poThread.commentsNumber
            if (poThread.imageUrl.isNotBlank()){
                val imageUrl=poThread.imageUrl
    //                val imagePosted=holder.binding.imagePosted
                if (imageUrl.endsWith("gif")){
                    Glide.with(holder.itemView).asGif().load(imageUrl).placeholder(R.drawable.image_loading)
                        .error(R.drawable.image_load_failed)
                        .into(holder.binding.imagePosted)
                }else{
                    Glide.with(holder.itemView).asBitmap().load(imageUrl).placeholder(R.drawable.image_loading)
                        .error(R.drawable.image_load_failed).dontAnimate()
                        .into(holder.binding.imagePosted)
                }
                holder.binding.imagePosted.visibility=View.VISIBLE
                holder.binding.imagePosted.setBackgroundResource(R.drawable.image_shape)
                holder.binding.imagePosted.clipToOutline=true
                holder.binding.imagePosted.setOnClickListener {
                    imageClickListener(imageUrl.replace("thumb","image"))
    //                    (fragment.requireActivity() as MainActivity).showImageDetailFragment(imageUrl.replace("thumb","image"))
                }
            }else{
                Glide.with(fragment).clear(holder.binding.imagePosted)
                holder.binding.imagePosted.visibility=View.GONE
            }
            holder.binding.mdCard.setOnClickListener {
                clickListener.invoke(poThread)
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
        val diffComparator=object : DiffUtil.ItemCallback<PoThread>() {
            override fun areItemsTheSame(oldItem: PoThread, newItem: PoThread): Boolean {
                return oldItem.ThreadId==newItem.ThreadId
            }

            override fun areContentsTheSame(oldItem: PoThread, newItem: PoThread): Boolean {
                return oldItem.ThreadId==newItem.ThreadId
            }

        }
    }
}