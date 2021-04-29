package com.simsim.island.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simsim.island.R
import com.simsim.island.databinding.BlockRuleRecyclerviewViewholderBinding
import com.simsim.island.model.BlockRule

class BlockRuleRecyclerViewAdapter(
    val list: List<BlockRule>,
    val editButtonClickListener: (blockRule: BlockRule) -> Unit,
    val switchOnCheckedChangeListener: (blockRule: BlockRule) -> Unit,
    val deleteButtonClickListener: (blockRule: BlockRule) -> Unit
) : RecyclerView.Adapter<BlockRuleRecyclerViewAdapter.BlockRuleViewHolder>() {
    inner class BlockRuleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = BlockRuleRecyclerviewViewholderBinding.bind(view)
        fun bind(blockRule: BlockRule) {
            binding.blockRuleNameTextView.text = blockRule.name
            binding.blockRuleSwitch.isChecked = blockRule.isEnable
            binding.blockRuleSwitch.setOnCheckedChangeListener { _, isChecked ->
                blockRule.isEnable=isChecked
                switchOnCheckedChangeListener.invoke(blockRule)
            }
            binding.deleteButton.setOnClickListener {
                deleteButtonClickListener.invoke(blockRule)
            }
            binding.editButton.setOnClickListener {
                editButtonClickListener.invoke(blockRule)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockRuleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.block_rule_recyclerview_viewholder, parent, false)
        return BlockRuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: BlockRuleViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}