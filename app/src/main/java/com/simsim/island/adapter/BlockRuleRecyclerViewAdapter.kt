package com.simsim.island.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.simsim.island.R
import com.simsim.island.databinding.CommonlyUsedRecyclerviewViewholderBinding
import com.simsim.island.model.BlockRule

class BlockRuleRecyclerViewAdapter(
    val context: Context,
    var list: List<BlockRule>,
    val editButtonClickListener: (blockRule: BlockRule) -> Unit,
    val switchOnCheckedChangeListener: (blockRule: BlockRule) -> Unit,
    val deleteButtonClickListener: (blockRule: BlockRule) -> Unit
) : RecyclerView.Adapter<BlockRuleRecyclerViewAdapter.BlockRuleViewHolder>() {
    inner class BlockRuleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = CommonlyUsedRecyclerviewViewholderBinding.bind(view)
        private val indicator=binding.indicatorBar
        @SuppressLint("SetTextI18n")
        fun bind(blockRule: BlockRule) {
            binding.nameTextView.text = "名称：${blockRule.name}"
            binding.detailTextView.text="规则：${blockRule.rule}"
            binding.enableSwitch.isChecked = blockRule.isEnable.also {
                setIndicatorColor(it)
            }

            binding.enableSwitch.setOnCheckedChangeListener { _, isChecked ->
                blockRule.isEnable=isChecked.also {
                    setIndicatorColor(it)
                }
                switchOnCheckedChangeListener.invoke(blockRule)
            }
            binding.deleteButton.setOnClickListener {
                deleteButtonClickListener.invoke(blockRule)
            }
            binding.editButton.setOnClickListener {
                editButtonClickListener.invoke(blockRule)
            }
        }
        fun setIndicatorColor(isEnable:Boolean){
            if (isEnable){
                indicator.setBackgroundColor(
                    ContextCompat.getColor(context,R.color.colorSecondary)
                )
            }else{
                indicator.setBackgroundColor(
                    ContextCompat.getColor(context,R.color.first_col_font_color)
                )
            }
        }
    }
    fun submitList(list: List<BlockRule>){
        this.list=list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockRuleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.commonly_used_recyclerview_viewholder, parent, false)
        return BlockRuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: BlockRuleViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}