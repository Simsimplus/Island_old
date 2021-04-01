package com.simsim.island.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.simsim.island.adapter.MainRecyclerViewAdapter
import com.simsim.island.databinding.MainFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : Fragment() {
    private lateinit var binding:MainFragmentBinding
    private lateinit var adapter: MainRecyclerViewAdapter

    companion object {
        fun newInstance() = MainFragment()
    }

    private val viewModel: MainViewModel by activityViewModels()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding= MainFragmentBinding.inflate(inflater, container, false)
        binding.viewModel=viewModel
        binding.lifecycleOwner=this
        adapter= MainRecyclerViewAdapter()
        viewModel.getThreads()
        viewModel.threadsResult.observe(viewLifecycleOwner){
//            binding.message.text=it.toString()
        }
        viewModel.currentSection.observe(viewLifecycleOwner){
            viewModel.setFlow(it)
        }
        lifecycleScope.launch {
            viewModel.flow.collectLatest {
                adapter.submitData(it)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mainRecyclerView.adapter=adapter
        binding.mainRecyclerView.layoutManager=LinearLayoutManager(context)
        binding.mainRecyclerView.doOnPreDraw {
            binding.indicatorTextview.isVisible=false
            binding.mainProgressIndicator.isVisible=false
            binding.mainRecyclerView.visibility=View.VISIBLE
        }
    }

}