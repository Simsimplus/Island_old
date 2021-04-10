package com.simsim.island.adapter

import android.graphics.Paint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simsim.island.R
import com.simsim.island.databinding.DetailRecyclerviewViewholderBinding
import com.simsim.island.model.BasicThread
import com.simsim.island.util.handleThreadId
import com.simsim.island.util.handleThreadTime
import com.simsim.island.util.referenceStringSpliterator

class DetailRecyclerViewAdapter(
    private val fragment: Fragment,
    private val poId: String,
    private val imageClickListener: (imageUrl: String) -> Unit,
    private val referenceClickListener:(reference:String)->Unit,
    private val itemClickListener: () -> Unit
) : PagingDataAdapter<BasicThread, DetailRecyclerViewAdapter.BasicThreadViewHolder>(diffComparator) {
    inner class BasicThreadViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

//        val uidTextview:TextView=view.findViewById(R.id.uid_textview)
//        val timeTextview:TextView=view.findViewById(R.id.time_textview)
//        val threadIdTextview:TextView=view.findViewById(R.id.thread_id_textview)
//        val contentTextview:TextView=view.findViewById(R.id.content_textview)
//        val imagePosted:ImageView=view.findViewById(R.id.image_posted)
//        val commentNumber:TextView=view.findViewById(R.id.comment_number)
    }

    override fun onBindViewHolder(holder: BasicThreadViewHolder, position: Int) {
        val thread = getItem(position)
        thread?.let {
            val binding = DetailRecyclerviewViewholderBinding.bind(holder.view)
//            Log.e("Simsim", it.toString())
            binding.uidTextview.text = handleThreadId(it.uid)
            binding.timeTextview.text = handleThreadTime(it.time)
            binding.threadIdTextview.text = it.replyThreadId.toString()
            binding.contentTextview.text = it.content
            // set po id highlighted
            if (it.uid == poId) {
                binding.uidTextview.setTypeface(null, Typeface.BOLD)
                if (it.isManager){
                    binding.uidTextview.setTextColor(
                        ContextCompat.getColor(
                            fragment.requireContext(),
                            R.color.manager_red
                        )
                    )
                }else{
                    binding.uidTextview.setTextColor(
                        ContextCompat.getColor(
                            fragment.requireContext(),
                            R.color.po_id_highlight
                        )
                    )
                }

            } else {
                binding.uidTextview.setTypeface(null, Typeface.NORMAL)
                binding.uidTextview.setTextColor(
                    ContextCompat.getColor(
                        fragment.requireContext(),
                        R.color.first_col_font_color
                    )
                )
            }

            //add text view to hold reference
            if (it.references.isNotBlank()) {
                val references = it.references.split(referenceStringSpliterator).reversed()
                val layoutParams = binding.contentTextview.layoutParams
//                val textSize = fragment.resources.getDimension(R.dimen.content_font_size)
                references.forEach { reference ->
                    val textView = LayoutInflater.from(fragment.requireContext())
                        .inflate(R.layout.reference_view, binding.detailFragmentContentLayout,false) as TextView
                    textView.apply {
                        text = reference
                        paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
//                        setTextSize(textSize)
                        tag = reference
                        setTextColor(
                            ContextCompat.getColor(
                                fragment.requireContext(),
                                R.color.thread_reference
                            )
                        )
                        setOnClickListener {
                            referenceClickListener(reference)
                        }
                    }
                    binding.detailFragmentContentLayout.addView(textView, 0, layoutParams)
                }
            } else {
                binding.detailFragmentContentLayout.children.forEach { childView ->
                    childView.tag?.run {
                        binding.detailFragmentContentLayout.removeView(childView)
                    }
                }
            }
            // if a posted image exists, load it from internet
            if (it.imageUrl.isNotBlank()) {
                val imageUrl = it.imageUrl
                if (imageUrl.endsWith("gif")){
                    Glide.with(holder.itemView).asGif().load(imageUrl).placeholder(R.drawable.image_loading)
                        .error(R.drawable.image_load_failed)
                        .into(binding.imagePosted)
                }else{
                    Glide.with(holder.itemView).asBitmap().load(imageUrl).placeholder(R.drawable.image_loading)
                        .error(R.drawable.image_load_failed).dontAnimate()
                        .into(binding.imagePosted)
                }

                binding.imagePosted.visibility = View.VISIBLE
                binding.imagePosted.setBackgroundResource(R.drawable.image_shape)
                binding.imagePosted.clipToOutline = true
                binding.imagePosted.setOnClickListener {
                    imageClickListener(imageUrl.replace("thumb", "image"))
//                    (fragment.requireActivity() as MainActivity).showImageDetailFragment(imageUrl.replace("thumb","image"))
                }
                //https://adnmb3.com/m/t/36468316
                if (it.content == "分享图片" || it.content.isBlank()) {
//                    Log.e("Simsim","thread to remove content:$it")
                    binding.contentTextview.visibility = View.GONE
                    binding.imagePosted.setPadding(0, 8, 0, 8)
                }
            } else {
                Glide.with(fragment).clear(binding.imagePosted)
                binding.imagePosted.visibility = View.GONE
                binding.contentTextview.visibility = View.VISIBLE
                binding.imagePosted.setPadding(0, 0, 0, 0)
            }


            binding.detailMdCard.setOnClickListener {
                itemClickListener.invoke()
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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.detail_recyclerview_viewholder, parent, false)
        return BasicThreadViewHolder(view)
    }

    companion object {
        val diffComparator = object : DiffUtil.ItemCallback<BasicThread>() {
            override fun areItemsTheSame(oldItem: BasicThread, newItem: BasicThread): Boolean {
                return oldItem.replyThreadId == newItem.replyThreadId
            }

            override fun areContentsTheSame(oldItem: BasicThread, newItem: BasicThread): Boolean {
                return oldItem.replyThreadId == newItem.replyThreadId
            }

        }
    }
}