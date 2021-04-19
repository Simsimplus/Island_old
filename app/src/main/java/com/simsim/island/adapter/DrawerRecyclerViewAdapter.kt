package com.simsim.island.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simsim.island.R
import com.simsim.island.adapter.DrawerRecyclerViewAdapter.*

class DrawerRecyclerViewAdapter(private var sectionList:List<String>?,private val clickListener: (sectionName:String)->Unit): RecyclerView.Adapter<DrawerRecyclerViewViewHolder>() {
    inner class DrawerRecyclerViewViewHolder(view: View):RecyclerView.ViewHolder(view){
        val sectionName: TextView =view.findViewById(R.id.section_name)
    }

    internal fun submitList(sectionList:List<String>){
        this.sectionList=sectionList
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DrawerRecyclerViewViewHolder {
        val view=LayoutInflater.from(parent.context).inflate(R.layout.spinner_viewholder,parent,false)
        return DrawerRecyclerViewViewHolder(view)
    }

    override fun onBindViewHolder(holder: DrawerRecyclerViewViewHolder, position: Int) {
        sectionList?.let { list->
            val sectionName=list[position]
            holder.sectionName.text=sectionName
            holder.itemView.setOnClickListener {
                clickListener(sectionName)
            }
        }
    }

    override fun getItemCount(): Int {
        return sectionList?.size?:0
    }
}