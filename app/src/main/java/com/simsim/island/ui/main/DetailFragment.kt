package com.simsim.island.ui.main

import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simsim.island.MainActivity
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
    private lateinit var actionMode: ActionMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setHasOptionsMenu(true)
    }


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
        actionMode = (requireActivity() as MainActivity).startSupportActionMode(actionModeCallback)!!
        actionMode.title=mainThread.poThread.ThreadId
        return binding.root
    }

//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        menu.clear()
//        inflater.inflate(R.menu.detail_fragment_toolbar_menu,menu)
//        super.onCreateOptionsMenu(menu, inflater)
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when(item.itemId){
//            R.id.detail_fragment_menu_add->{
//                newThreadReply()
//            }
//            R.id.detail_fragment_menu_report->{
//
//            }
//            R.id.detail_fragment_menu_share->{
//
//            }
//        }
//        return super.onOptionsItemSelected(item)
//    }

    private fun newThreadReply() {
        parentFragmentManager.commit {
            add( R.id.activity_fragment_container,NewDraftFragment.newInstance(target = "thread",targetKeyWord =mainThread.poThread.ThreadId,actionMode),"reply")
            addToBackStack("reply")
            viewModel.isMainFragment.value = false
        }
    }

    override fun onDetach() {
        super.onDetach()
        viewModel.isMainFragment.value=true
    }
    private val actionModeCallback=object :ActionMode.Callback{
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.detail_fragment_toolbar_menu,menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return if (item!=null){
                return when(item.itemId){
                    R.id.detail_fragment_menu_share->{

                        true
                    }
                    R.id.detail_fragment_menu_report->{
                        true
                    }
                    R.id.detail_fragment_menu_add->{
                        newThreadReply()
                        true
                    }
                    else->{false}
                }
            }else{false}
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            parentFragmentManager.popBackStack()
        }

    }

    companion object {
        @JvmStatic
        fun newInstance(mainThread:IslandThread) =
            DetailFragment(mainThread)

    }
}