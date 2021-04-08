package com.simsim.island.ui.main

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simsim.island.R
import com.simsim.island.adapter.DetailRecyclerViewAdapter
import com.simsim.island.databinding.DetailDialogfragmentBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DetailDialogFragment : DialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private val args: DetailDialogFragmentArgs by navArgs()
    private lateinit var binding: DetailDialogfragmentBinding
    private lateinit var adapter: DetailRecyclerViewAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var actionMode: ActionMode

    //1.set dialog style in onCreate()
    //2.set toolbar in OnViewCreated()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.fullscreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DetailDialogfragmentBinding.inflate(inflater, container, false)
        adapter = DetailRecyclerViewAdapter(this,
            poId = args.poId,
            imageClickListener = { imageUrl ->
                val action = MainFragmentDirections.actionGlobalImageDetailFragment(imageUrl)
                findNavController().navigate(action)
            },
            referenceClickListener = { reference ->
                showReferenceDialog(reference)
            }) {

        }
        setupRecyclerView()
        setupSwipeRefreshLayout()

        observeRecyclerViewFlow()
        return binding.root
    }

    private fun showReferenceDialog(reference: String) {
        Log.e("Simsim", "create reference dialog with reference: $reference")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(reference)
            .show()
    }

    private fun observeRecyclerViewFlow() {
        lifecycleScope.launch {
            viewModel.detailFlow.collectLatest {
                Log.e("Simsim", "got thread detail data:$it")
                adapter.submitData(it)
            }
        }
    }

    private fun setupRecyclerView() {
        binding.detailDialogRecyclerView.adapter = adapter
        layoutManager = LinearLayoutManager(context)
        binding.detailDialogRecyclerView.layoutManager = layoutManager
        binding.detailDialogRecyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                layoutManager.orientation
            )
        )
    }

    private fun setupSwipeRefreshLayout() {
        val swipeRefreshLayout = binding.detailSwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            adapter.refresh()
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar = binding.detailDialogToolbar
        toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24)
        toolbar.title = "No." + args.ThreadId
        toolbar.setNavigationOnClickListener {
            dismiss()
        }
        toolbar.inflateMenu(R.menu.detail_fragment_toolbar_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.detail_fragment_menu_add -> {
                    //                newThreadReply()
                    true
                }
                R.id.detail_fragment_menu_report -> {
                    true
                }
                R.id.detail_fragment_menu_share -> {
                    true
                }
                else -> {
                    false
                }
            }
        }
    }


    private fun newThreadReply() {
        parentFragmentManager.commit {
            add(
                R.id.nv_host_fragment_container,
                NewDraftFragment.newInstance(target = "thread", targetKeyWord = args.ThreadId),
                "reply"
            )
            addToBackStack("reply")
            viewModel.isMainFragment.value = false
        }
    }

    override fun onDetach() {
        super.onDetach()
        viewModel.isMainFragment.value = true
    }

}