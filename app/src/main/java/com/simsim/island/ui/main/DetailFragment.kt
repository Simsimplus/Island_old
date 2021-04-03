package com.simsim.island.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simsim.island.R
import com.simsim.island.adapter.DetailRecyclerViewAdapter
import com.simsim.island.databinding.DetailFragmentBinding
import com.simsim.island.model.IslandThread
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DetailFragment(private val mainThread:IslandThread) : Fragment() {
    private val viewModel:MainViewModel by activityViewModels()
    private lateinit var binding:DetailFragmentBinding
    private lateinit var adapter: DetailRecyclerViewAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding= DetailFragmentBinding.inflate(inflater,container,false)
        adapter= DetailRecyclerViewAdapter(this,mainThread.poThread.uid){}
        binding.detailRecyclerView.adapter=adapter
        val layoutManager=LinearLayoutManager(context)
        binding.detailRecyclerView.layoutManager=layoutManager
        binding.detailRecyclerView.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))
        viewModel.setDetailFlow(mainThread)
        lifecycleScope.launch {
            viewModel.detailFlow.collectLatest {
                adapter.submitData(it)
            }
        }
        return binding.root
    }

    override fun onDetach() {
        super.onDetach()
        viewModel.isMainFragment.value=true
    }

    companion object {
        @JvmStatic
        fun newInstance(mainThread:IslandThread) =
            DetailFragment(mainThread)
    }
}