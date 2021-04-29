package com.simsim.island.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simsim.island.R
import com.simsim.island.databinding.CookieRecyclerviewViewholderBinding
import com.simsim.island.model.Cookie

class CookieRecyclerViewAdapter(
    var list: MutableList<Cookie>,
    val switchOnCheckedChangeListener: (cookies: List<Cookie>) -> Unit,
    val deleteButtonClickListener: (cookie: Cookie) -> Unit
) : RecyclerView.Adapter<CookieRecyclerViewAdapter.CookieViewHolder>() {
    inner class CookieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = CookieRecyclerviewViewholderBinding.bind(view)
        fun bind(cookie: Cookie) {
            binding.cookieNameTextView.text = cookie.name
            binding.cookieSwitch.isChecked = cookie.isInUse
            binding.cookieSwitch.setOnCheckedChangeListener { _, isChecked ->
                cookie.isInUse = isChecked
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
    }

    fun submitList(list: MutableList<Cookie>) {
        this.list = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CookieViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.cookie_recyclerview_viewholder, parent, false)
        return CookieViewHolder(view)
    }

    override fun onBindViewHolder(holder: CookieViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}