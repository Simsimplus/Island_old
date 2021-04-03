package com.simsim.island.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simsim.island.MainActivity
import com.simsim.island.R
import com.simsim.island.databinding.DetailRecyclerviewViewholderBinding
import com.simsim.island.model.BasicThread
import com.simsim.island.ui.main.DetailFragment
import com.simsim.island.util.handleThreadId
import com.simsim.island.util.handleThreadTime

class DetailRecyclerViewAdapter(private val fragment:DetailFragment,val poId:String, private val clickListener: ()->Unit):PagingDataAdapter<BasicThread, DetailRecyclerViewAdapter.BasicThreadViewHolder>(diffComparator) {
    inner class BasicThreadViewHolder(private val view: View):RecyclerView.ViewHolder(view){
        val binding=DetailRecyclerviewViewholderBinding.bind(view)
//        val uidTextview:TextView=view.findViewById(R.id.uid_textview)
//        val timeTextview:TextView=view.findViewById(R.id.time_textview)
//        val threadIdTextview:TextView=view.findViewById(R.id.thread_id_textview)
//        val contentTextview:TextView=view.findViewById(R.id.content_textview)
//        val imagePosted:ImageView=view.findViewById(R.id.image_posted)
//        val commentNumber:TextView=view.findViewById(R.id.comment_number)
    }

    override fun onBindViewHolder(holder: BasicThreadViewHolder, position: Int) {
        val thread=getItem(position)
        thread?.let {
            holder.binding.uidTextview.text= handleThreadId(it.uid)
            val uidTextviewText=holder.binding.uidTextview.text
            if (it.uid==poId){
                holder.binding.uidTextview.text=uidTextviewText.toString().plus("(po)")
            }
            holder.binding.timeTextview.text= handleThreadTime(it.time)
            holder.binding.threadIdTextview.text=it.ThreadId
            holder.binding.contentTextview.text=it.content
            if (it.imageUrl.isNotBlank()){
                val imageUrl=it.imageUrl
//                val imagePosted=holder.binding.imagePosted
                Glide.with(holder.itemView).load(imageUrl.replace("thumb","image")).placeholder(R.drawable.image_loading).error(R.drawable.image_load_failed).dontAnimate().into(holder.binding.imagePosted)
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
            holder.binding.detailMdCard.setOnClickListener {
                clickListener.invoke()
            }
//            holder.uidTextview.text= handleThreadId(it.poThread.uid)
//            holder.timeTextview.text= handleThreadTime(it.poThread.time)
//            holder.threadIdTextview.text= it.poThread.ThreadId
//            holder.contentTextview.text=it.poThread.content
//            holder.commentNumber.text=it.commentsNumber
//            if (it.poThread.imageUrl.isNotBlank()){
//                Glide.with(fragment).load(it.poThread.imageUrl).into(holder.imagePosted)
//                holder.imagePosted.visibility=View.VISIBLE
//                holder.imagePosted.setBackgroundResource(R.drawable.image_shape)
//                holder.imagePosted.clipToOutline=true
//            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasicThreadViewHolder {
        val view=LayoutInflater.from(parent.context).inflate(R.layout.detail_recyclerview_viewholder,parent,false)
        return BasicThreadViewHolder(view)
    }
    companion object{
        val diffComparator=object : DiffUtil.ItemCallback<BasicThread>() {
            override fun areItemsTheSame(oldItem: BasicThread, newItem: BasicThread): Boolean {
                return oldItem.ThreadId==newItem.ThreadId
            }

            override fun areContentsTheSame(oldItem: BasicThread, newItem: BasicThread): Boolean {
                return oldItem.ThreadId==newItem.ThreadId
            }

        }
    }
}