package com.simsim.island.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.simsim.island.R
import com.simsim.island.databinding.CommonlyUsedRecyclerviewViewholderBinding
import com.simsim.island.model.Cookie

class CookieRecyclerViewAdapter(
    val context: Context,
    var list: MutableList<Cookie>,
    val switchOnCheckedChangeListener: (cookies: List<Cookie>) -> Unit,
    val deleteButtonClickListener: (cookie: Cookie) -> Unit
) : RecyclerView.Adapter<CookieRecyclerViewAdapter.CookieViewHolder>() {
    inner class CookieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = CommonlyUsedRecyclerviewViewholderBinding.bind(view)
        private val indicator=binding.indicatorBar
        fun bind(cookie: Cookie) {
            binding.editButton.visibility=View.GONE
            binding.nameTextView.text = cookie.name
            binding.enableSwitch.isChecked = cookie.isInUse.also {
                setIndicatorColor(it)
            }
            binding.enableSwitch.setOnCheckedChangeListener { _, isChecked ->
                cookie.isInUse = isChecked.also {
                    setIndicatorColor(it)
                }
                if (isChecked) {
                    list = list.map {
                        it.isInUse = it.cookie == cookie.cookie
                        it
                    }.toMutableList()
                    notifyDataSetChanged()
                }
                switchOnCheckedChangeListener.invoke(list)
            }
            binding.deleteButton.setOnClickListener {
                deleteButtonClickListener.invoke(cookie)
                list.remove(cookie)
                notifyDataSetChanged()
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

    fun submitList(list: MutableList<Cookie>) {
        this.list = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CookieViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.commonly_used_recyclerview_viewholder, parent, false)
        return CookieViewHolder(view)
    }

    override fun onBindViewHolder(holder: CookieViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}