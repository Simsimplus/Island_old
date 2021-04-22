package com.simsim.island.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simsim.island.R
import com.simsim.island.adapter.DrawerRecyclerViewAdapter.*
import com.simsim.island.model.Section
import com.simsim.island.model.SectionGroup
import kotlin.properties.Delegates

class DrawerRecyclerViewAdapter(private var sectionGroup: SectionGroup,private val clickListener: (section:Section)->Unit): RecyclerView.Adapter<DrawerRecyclerViewViewHolder>() {
    sealed class DrawerRecyclerViewViewHolder(view: View):RecyclerView.ViewHolder(view){
        class HeaderVH(view: View):DrawerRecyclerViewViewHolder(view){
            val tv=view.findViewById<TextView>(R.id.text_view_header)
            fun bind(content:String,onItemClickListener:()->Unit){
                tv.text=content
                tv.setOnClickListener {
                    onItemClickListener()
                }
            }
        }

        class ListVH(view: View):DrawerRecyclerViewViewHolder(view){
            val tv=view.findViewById<TextView>(R.id.text_view_list)
            fun bind(section: Section,onItemClickListener:(section:Section)->Unit){
                tv.text=section.sectionName
                tv.setOnClickListener {
                    onItemClickListener(section)
                }
            }
        }

    }
    companion object{
        const val HEADER_VIEW_TYPE=0
        const val LIST_VIEW_TYPE=1
    }
    override fun getItemViewType(position: Int): Int {
       return when(position){
            0->{
                HEADER_VIEW_TYPE
            }
            else->{
                LIST_VIEW_TYPE
            }
        }
    }


    var isExpanded:Boolean by Delegates.observable(false){_,_,newValue->
        if (newValue){
            notifyItemRangeInserted(1,sectionGroup.sectionNameList.size)
        }else{
            notifyItemRangeRemoved(1,sectionGroup.sectionNameList.size)
        }
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DrawerRecyclerViewViewHolder {
        val inflater=LayoutInflater.from(parent.context)
        return when(viewType){
            HEADER_VIEW_TYPE->{
                val view=inflater.inflate(R.layout.drawer_header_view_holder, parent, false)
                DrawerRecyclerViewViewHolder.HeaderVH(view)
            }
            LIST_VIEW_TYPE->{
                val view=inflater.inflate(R.layout.drawer_list_view_holder, parent, false)
                DrawerRecyclerViewViewHolder.ListVH(view)
            }
            else->{
                throw IllegalArgumentException("wrong view type for drawer recycler view")
            }
        }
    }

    override fun onBindViewHolder(holder: DrawerRecyclerViewViewHolder, position: Int) {
        when(holder){
            is DrawerRecyclerViewViewHolder.HeaderVH->{
                holder.bind(sectionGroup.groupName){
                    isExpanded=!isExpanded
                }
            }
            is DrawerRecyclerViewViewHolder.ListVH->{
                holder.bind(sectionGroup.sectionNameList[position-1],clickListener)
            }
        }
    }

    override fun getItemCount(): Int {
        return if(isExpanded) sectionGroup.sectionNameList.size+1 else 1
    }
}