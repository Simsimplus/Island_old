package com.simsim.island.ui.main

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.simsim.island.R
import com.simsim.island.adapter.DetailRecyclerViewAdapter
import com.simsim.island.adapter.MainLoadStateAdapter
import com.simsim.island.databinding.DetailDialogfragmentBinding
import com.simsim.island.databinding.DetailRecyclerviewViewholderBinding
import com.simsim.island.model.BasicThread
import com.simsim.island.repository.AislandRepo
import com.simsim.island.util.LOG_TAG
import com.simsim.island.util.OnSwipeListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

@AndroidEntryPoint
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
        setupRecyclerView()
        setupSwipeRefreshLayout()
        observeRecyclerViewFlow()
        observeThreadStarStatus()
        setupFAB()
        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates->
                when(loadStates.refresh){
                     is LoadState.Loading->{
                         binding.detailLoadingImage.setImageResource(viewModel.randomLoadingImage)
//                         binding.detailLoadingImage.visibility=View.VISIBLE
//                         binding.detailFabAdd.visibility=View.INVISIBLE
                     }
                    is LoadState.Error->{
                        binding.detailLoadingImage.setImageResource(R.drawable.ic_loading_page_failed)
                        binding.detailLoadingImage.visibility=View.VISIBLE
                        Snackbar
                            .make(binding.root,R.string.loading_page_fail_info,Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.loading_fail_retry)){
                                adapter.retry()
                            }
                            .show()
                        binding.detailFabAdd.visibility=View.INVISIBLE
                    }
                    else->{
                        binding.detailLoadingImage.visibility=View.INVISIBLE
                        binding.detailFabAdd.visibility=View.VISIBLE
                    }
                }
            }
        }

        return binding.root
    }

    private fun observeThreadStarStatus() {
        lifecycleScope.launch {
            viewModel.database.threadDao().getFlowPoThread(args.ThreadId).collectLatest {
                it?.let {
                    val starItem =
                        binding.detailDialogToolbar.menu.findItem(R.id.detail_fragment_menu_star)
                    if (it.isStar) {
                        starItem.title = "取消收藏"
                    } else {
                        starItem.title = "收藏"
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFAB() {
        binding.detailFabAdd.setOnClickListener {
            newThreadReply()
        }
        binding.detailFabAdd.setOnTouchListener(
            OnSwipeListener(
                requireContext(),
                onSwipeTop = {},
                onSwipeRight = {},
                onSwipeLeft = {
                              adapter.refresh()
                },
                onSwipeBottom = {},
            )
        )
    }


    private fun showReferenceDialog(reference: String) {
        Log.e("Simsim", "create reference dialog with reference: $reference")
        lifecycleScope.launch {
            val referenceId = try{ reference.replace("\\D".toRegex(), "").toLong() }catch (e:Exception){
                Log.e(LOG_TAG,e.stackTraceToString())
                31163008
            }
            val list = viewModel.currentReplyThreads
            val index = list.indexOfFirst {
                it.replyThreadId == referenceId
            }
            val holder =
                DetailRecyclerviewViewholderBinding.inflate(layoutInflater, binding.root, false)
            when (index) {
                in (0..list.size) -> {
                    val referenceThread = list[index]
                    adapter.bindHolder(
                        holder = DetailRecyclerViewAdapter.BasicThreadViewHolder(
                            holder.root
                        ), it = referenceThread
                    )
                    MaterialAlertDialogBuilder(requireContext()).setView(holder.root).show()
                }
                else -> {
                    val url = "https://adnmb3.com/m/t/$referenceId"
                    Log.e("Simsim", "request for thread detail:$url")
                    val response = viewModel.networkService.getHtmlStringByPage(url)
                    val thread: BasicThread = if (response != null) {
                        val doc = Jsoup.parse(response)
                        val poThreadDiv =
                            doc.selectFirst("div[class=uk-container h-threads-container]")
                        if (poThreadDiv==null){
                            BasicThread(replyThreadId = 888, poThreadId = 888, section = "", fId = "")
                        }else{
                            val poThread = AislandRepo.divToBasicThread(
                                poThreadDiv,
                                isPo = true,
                                section = "",
                                poThreadId = 0L,
                                fId = ""
                            )
                            poThread
                        }

                    } else {
                        BasicThread(replyThreadId = 888, poThreadId = 888, section = "", fId = "")
                    }
                    adapter.bindHolder(
                        holder = DetailRecyclerViewAdapter.BasicThreadViewHolder(
                            holder.root
                        ), it = thread
                    )
                    MaterialAlertDialogBuilder(requireContext()).setView(holder.root).show()
                }
            }


        }
    }



private fun observeRecyclerViewFlow() {
    lifecycleScope.launch {
        viewModel.detailFlow.collectLatest {
            Log.e("Simsim", "got thread detail data:$it")
            adapter.submitData(it)
            Log.e(LOG_TAG, "detail threads:${it}")
        }
    }
}

private fun setupRecyclerView() {
    adapter = DetailRecyclerViewAdapter(this,
        imageClickListener = { imageUrl ->
            val action = MainFragmentDirections.actionGlobalImageDetailFragment(imageUrl)
            findNavController().navigate(action)
        },
        referenceClickListener = { reference ->
            showReferenceDialog(reference)
        }, {

        })
    binding.detailDialogRecyclerView.adapter =
        adapter.withLoadStateHeader(MainLoadStateAdapter { adapter.retry() })
    layoutManager = LinearLayoutManager(context)
    binding.detailDialogRecyclerView.layoutManager = layoutManager
//        binding.detailDialogRecyclerView.addItemDecoration(
//            DividerItemDecoration(
//                context,
//                layoutManager.orientation
//            )
//        )
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
    toolbar.subtitle = "No." + args.ThreadId
    toolbar.title = "adnmb.com"
    toolbar.setNavigationOnClickListener {
        dismiss()
    }
    toolbar.inflateMenu(R.menu.detail_fragment_toolbar_menu)
    toolbar.setOnMenuItemClickListener {
        when (it.itemId) {
//                R.id.detail_fragment_menu_add -> {
//                                    newThreadReply()
//                    true
//                }
            R.id.detail_fragment_menu_report -> {
                true
            }
            R.id.detail_fragment_menu_share -> {
                true
            }
            R.id.detail_fragment_menu_star -> {
                viewModel.starPoThread(args.ThreadId)
//                    val starItem = toolbar.menu.findItem(R.id.detail_fragment_menu_star)
//                    when(starItem.title){
//                        "收藏"->{
//                            starItem.title="取消收藏"
//                        }
//                        "取消收藏"->{
//                            starItem.title="收藏"
//                        }
//                    }
                true
            }
            else -> {
                false
            }
        }
    }
}


private fun newThreadReply(prefill:String="") {
    val action = DetailDialogFragmentDirections.actionGlobalNewDraftFragment(
        target = "thread",
        threadId= args.ThreadId,
        prefillText = prefill
    )
    findNavController().navigate(action)
    viewModel.isMainFragment.value = false
}

override fun onDetach() {
    super.onDetach()
    viewModel.isMainFragment.value = true
}

}