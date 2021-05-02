package com.simsim.island.adapter

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simsim.island.R
import com.simsim.island.databinding.DetailRecyclerviewViewholderBinding
import com.simsim.island.model.ReplyThread
import com.simsim.island.util.handleThreadTime

class DetailRecyclerViewAdapter(
    private val fragment: Fragment,
    private val imageClickListener: (imageUrl: String) -> Unit,
    private val referenceClickListener:(reference:String)->Unit,
    private val itemClickListener: () -> Unit
) : PagingDataAdapter<ReplyThread, DetailRecyclerViewAdapter.BasicThreadViewHolder>(diffComparator) {
    class BasicThreadViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

//        val uidTextview:TextView=view.findViewById(R.id.uid_textview)
//        val timeTextview:TextView=view.findViewById(R.id.time_textview)
//        val threadIdTextview:TextView=view.findViewById(R.id.thread_id_textview)
//        val contentTextview:TextView=view.findViewById(R.id.content_textview)
//        val imagePosted:ImageView=view.findViewById(R.id.image_posted)
//        val commentNumber:TextView=view.findViewById(R.id.comment_number)
    }
    inner class HideTextSpan(
        var color:Int,
        val clickListener:()->Unit
    ):ClickableSpan(){
        override fun updateDrawState(textPaint: TextPaint) {
            textPaint.bgColor=color
//            super.updateDrawState(textPaint)
        }

        override fun onClick(widget: View) {
            color=ContextCompat.getColor(fragment.requireContext(),R.color.transparent)
            widget.invalidate()
        }
    }
    inner class ReferenceClickableSpan(
        val clickListener:()->Unit
    ):ClickableSpan(){
        override fun updateDrawState(textPaint: TextPaint) {
            textPaint.color=ContextCompat.getColor(fragment.requireContext(),R.color.thread_reference)
            textPaint.isUnderlineText=true
//            super.updateDrawState(textPaint)
        }

        override fun onClick(widget: View) {
            clickListener.invoke()
        }
    }

    override fun onBindViewHolder(holder: BasicThreadViewHolder, position: Int) {
        val binding = DetailRecyclerviewViewholderBinding.bind(holder.view)
        val thread = getItem(position)
        thread?.let {
            binding.firstRowDetail.visibility=View.VISIBLE
            binding.secondRowDetail.visibility=View.VISIBLE
            binding.firstRowDetailPlaceholder.visibility=View.GONE
            binding.secondRowDetailPlaceholder.visibility=View.GONE
            bindHolder(binding, it)
        }?: kotlin.run {
            binding.firstRowDetail.visibility=View.GONE
            binding.secondRowDetail.visibility=View.GONE
            binding.firstRowDetailPlaceholder.visibility=View.VISIBLE
            binding.secondRowDetailPlaceholder.visibility=View.VISIBLE
        }
    }

    fun bindHolder(
        binding: DetailRecyclerviewViewholderBinding,
        it: ReplyThread
    ) {
        binding.uidTextview.text = it.uid
        binding.timeTextview.text = handleThreadTime(it.time)
        binding.threadIdTextview.text = it.replyThreadId.toString()
        binding.contentTextview.apply{
            val referenceRegex=">>No.(\\d+)".toRegex()
            val hideTextRegex="\\[h\\](.*)?\\[\\/h\\]".toRegex()
            val hideTexts=hideTextRegex.findAll(it.content).toList().map{
                it.groupValues[1]
            }
            val content=SpannableString(it.content.replace("\\[h\\]|\\[\\/h\\]".toRegex(),""))

            hideTexts.distinct().forEach { hideText->
                Regex.fromLiteral(hideText).findAll(content).forEach {
                    content.setSpan(
                        HideTextSpan(ContextCompat.getColor(fragment.requireContext(),R.color.content_font_color)){},
                        it.range.first,
                        it.range.last+1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            referenceRegex.findAll(content).forEach {
                content.setSpan(
                    ReferenceClickableSpan{
                                            referenceClickListener(it.groupValues[0])
                    },
                    it.range.first,
                    it.range.last+1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            text=content
            movementMethod=LinkMovementMethod.getInstance()
            highlightColor=ContextCompat.getColor(fragment.requireContext(),R.color.transparent)
        }
        // set po id highlighted
        if (it.isPo) {
            binding.uidTextview.setTypeface(null, Typeface.BOLD)
            if (it.isManager) {
                binding.uidTextview.setTextColor(
                    ContextCompat.getColor(
                        fragment.requireContext(),
                        R.color.manager_red
                    )
                )
            } else {
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

        // if a posted image exists, load it from internet
        if (it.imageUrl.isNotBlank()) {
            val imageUrl = it.imageUrl
            if (imageUrl.endsWith("gif")) {
                Glide.with(binding.root).asGif().load(imageUrl)
                    .placeholder(R.drawable.image_loading)
                    .error(R.drawable.image_load_failed)
                    .into(binding.imagePosted)
            } else {
                Glide.with(binding.root).asBitmap().load(imageUrl)
                    .placeholder(R.drawable.image_loading)
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasicThreadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.detail_recyclerview_viewholder, parent, false)
        return BasicThreadViewHolder(view)
    }

    companion object {
        val diffComparator = object : DiffUtil.ItemCallback<ReplyThread>() {
            override fun areItemsTheSame(oldItem: ReplyThread, newItem: ReplyThread): Boolean {
                return oldItem.replyThreadId == newItem.replyThreadId
            }

            override fun areContentsTheSame(oldItem: ReplyThread, newItem: ReplyThread): Boolean {
                return oldItem == newItem
            }

        }
    }
}