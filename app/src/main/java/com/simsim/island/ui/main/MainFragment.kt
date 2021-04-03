package com.simsim.island.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.simsim.island.R
import com.simsim.island.adapter.MainLoadStateAdapter
import com.simsim.island.adapter.MainRecyclerViewAdapter
import com.simsim.island.databinding.MainFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : Fragment() {
    private lateinit var binding:MainFragmentBinding
    internal lateinit var adapter: MainRecyclerViewAdapter

    companion object {
        fun newInstance() = MainFragment()
    }

    private val viewModel: MainViewModel by activityViewModels()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding= MainFragmentBinding.inflate(inflater, container, false)
        binding.viewModel=viewModel
        binding.lifecycleOwner=this
        adapter= MainRecyclerViewAdapter(this){
            parentFragmentManager.commit {
                add(R.id.activity_fragment_container,DetailFragment.newInstance(it),"threadDetail")
                addToBackStack("threadDetail")
                viewModel.isMainFragment.value=false
            }
        }
        binding.mainRecyclerView.adapter=adapter
            .withLoadStateFooter(MainLoadStateAdapter(adapter::retry))
        val layoutManager=LinearLayoutManager(context)
        binding.mainRecyclerView.layoutManager=layoutManager
        binding.mainRecyclerView.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))

//        viewModel.getThreads()
        viewModel.threadsResult.observe(viewLifecycleOwner){
//            binding.message.text=it.toString()
        }
        viewModel.currentSection.observe(viewLifecycleOwner){
            viewModel.setMainFlow(it)
        }
        lifecycleScope.launch {
            viewModel.mainFlow.collectLatest {
                adapter.submitData(it)
            }
        }
        viewModel.isMainFragment.observe(viewLifecycleOwner){
            binding.mainRecyclerView.suppressLayout(!it)
        }
        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates->
                if(loadStates.refresh is LoadState.Loading){
                    binding.mainProgressIndicator.visibility=View.VISIBLE
                    binding.indicatorTextview.visibility=View.VISIBLE
                }else{
                    binding.mainProgressIndicator.visibility=View.GONE
                    binding.indicatorTextview.visibility=View.GONE
                }
//                binding.mainProgressIndicator.isVisible = loadStates.refresh is LoadState.Loading
//                binding.indicatorTextview.isVisible = loadStates.refresh is LoadState.Loading
                if (loadStates.refresh is LoadState.Error){
                    binding.indicatorTextview.isVisible=true
                    binding.indicatorTextview.text="错误，点击重试!"
                    binding.indicatorTextview.setOnClickListener {
                        adapter.retry()
                    }
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }


}