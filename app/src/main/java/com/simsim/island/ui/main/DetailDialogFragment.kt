package com.simsim.island.ui.main

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.simsim.island.MainActivity
import com.simsim.island.R
import com.simsim.island.adapter.DetailRecyclerViewAdapter
import com.simsim.island.adapter.MainLoadStateAdapter
import com.simsim.island.dataStore
import com.simsim.island.databinding.DetailDialogfragmentBinding
import com.simsim.island.databinding.DetailRecyclerviewViewholderBinding
import com.simsim.island.dp2PxScale
import com.simsim.island.model.ReplyThread
import com.simsim.island.preferenceKey.PreferenceKey
import com.simsim.island.repository.AislandRepo
import com.simsim.island.util.*
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
    private lateinit var fab: FloatingActionButton
    private lateinit var layoutManager: LinearLayoutManager
    private var isFABEnable = true
    private lateinit var preferenceKey: PreferenceKey
//    private var fabSize:

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
        fab = binding.detailFabAdd
        preferenceKey = PreferenceKey(requireContext())
        return binding.root
    }

    private fun handleLoadingImage() {
        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates ->
                if (args.isSaved){
                    binding.detailLoadingImage.visibility = View.INVISIBLE
                    if (isFABEnable) {
                        binding.detailFabAdd.visibility = View.VISIBLE
                    } else {
                        binding.detailFabAdd.visibility = View.INVISIBLE
                    }
                }else{
                    when (loadStates.refresh) {
                        is LoadState.Loading -> {
//                        binding.detailLoadingImage.setImageResource(viewModel.randomLoadingImage)
                            binding.detailLoadingImage.visibility = View.VISIBLE
                            binding.detailFabAdd.visibility = View.INVISIBLE
                        }
                        is LoadState.Error -> {
//                        binding.detailLoadingImage.setImageResource(viewModel.randomLoadingImage)
                            binding.detailLoadingImage.visibility = View.VISIBLE
                            Snackbar
                                .make(
                                    binding.root,
                                    R.string.loading_page_fail_info,
                                    Snackbar.LENGTH_INDEFINITE
                                )
                                .setAction(getString(R.string.loading_fail_retry)) {
                                    adapter.retry()
                                }
                                .show()
                            binding.detailFabAdd.visibility = View.INVISIBLE
                        }
                        else -> {
                            binding.detailLoadingImage.visibility = View.INVISIBLE
                            if (isFABEnable) {
                                binding.detailFabAdd.visibility = View.VISIBLE
                            } else {
                                binding.detailFabAdd.visibility = View.INVISIBLE
                            }

                        }
                    }
                }

            }
        }
    }

    private fun observeThreadStarStatus() {
        lifecycleScope.launch {
            viewModel.database.threadDao().isPoThreadStared(args.ThreadId).collectLatest {
                val starItem =
                    binding.detailDialogToolbar.menu.findItem(R.id.detail_fragment_menu_star)
                if (it) {
                    starItem.title = "取消收藏"
                } else {
                    starItem.title = "收藏"
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFAB() {
        lifecycleScope.launch {
            launch {
                requireContext().dataStore.data.collectLatest { settings ->
                    settings[booleanPreferencesKey(preferenceKey.enableFabKey)]?.let { enable ->
                        isFABEnable = enable
                        binding.detailDialogToolbar.menu.findItem(R.id.detail_fragment_menu_add).isVisible =
                            !enable
                        fab.isVisible = enable
                    }
                    val swipeUp = settings[stringPreferencesKey(SWIPE_UP)]
                    val swipeDown = settings[stringPreferencesKey(SWIPE_DOWN)]
                    val swipeLeft = settings[stringPreferencesKey(SWIPE_LEFT)]
                    val swipeRight = settings[stringPreferencesKey(SWIPE_RIGHT)]
                    fab.setOnTouchListener(OnSwipeListener(
                        requireContext(),
                        onSwipeTop = {
                            getFABSwipeFunction(swipeString = swipeUp).invoke()
                        },
                        onSwipeBottom = {
                            getFABSwipeFunction(swipeString = swipeDown).invoke()
                        },
                        onSwipeLeft = {
                            getFABSwipeFunction(swipeString = swipeLeft).invoke()
                        },
                        onSwipeRight = {
                            getFABSwipeFunction(swipeString = swipeRight).invoke()
                        }
                    ))

                    settings[booleanPreferencesKey(preferenceKey.fabDefaultSizeKey)]?.let { setSizeDefault ->
                        if (setSizeDefault) {
                            fab.customSize = FloatingActionButton.NO_CUSTOM_SIZE
                            fab.size = FloatingActionButton.SIZE_AUTO
                        } else {
                            settings[intPreferencesKey(preferenceKey.fabSizeSeekBarKey)]?.let { fabCustomSize ->
                                fab.customSize =
                                    (fabCustomSize * requireContext().dp2PxScale()).toInt()
                            }
                        }

                    }
                    (settings[booleanPreferencesKey(preferenceKey.fabPlaceRightKey)]
                        ?: true).let { placeRight ->
                        val sideMargin =
                            settings[intPreferencesKey(preferenceKey.fabSideMarginKey)] ?: 0
                        val bottomMargin =
                            settings[intPreferencesKey(preferenceKey.fabBottomMarginKey)] ?: 0
                        val layoutParams = fab.layoutParams as CoordinatorLayout.LayoutParams
                        layoutParams.bottomMargin =
                            ((30 + bottomMargin) * requireContext().dp2PxScale()).toInt()
                        if (placeRight) {
                            layoutParams.gravity = Gravity.BOTTOM or Gravity.RIGHT
                            layoutParams.rightMargin =
                                ((30 + sideMargin) * requireContext().dp2PxScale()).toInt()
                            layoutParams.leftMargin = 0
                        } else {
                            layoutParams.gravity = Gravity.BOTTOM or Gravity.LEFT
                            layoutParams.leftMargin =
                                ((30 + sideMargin) * requireContext().dp2PxScale()).toInt()
                            layoutParams.rightMargin = 0
                        }
                        fab.layoutParams = layoutParams
                        fab.requestLayout()
                    }
                }
            }
            launch {
                fab.setOnClickListener {
                    newThreadReply(
                        target = TARGET_THREAD,
                        prefill = "",
                        threadId = args.ThreadId,
                        fId = ""
                    )
                }
            }
        }
    }

    private fun getFABSwipeFunction(swipeString: String?): () -> Unit {
        val array = requireActivity().resources.getStringArray(R.array.swipe_function)
        return when (swipeString) {
//                <item> 无</item>
//                <item>刷新页面</item>
//                <item>打开侧边栏</item>
//                <item>回到顶部</item>
            array[1] -> {
                {
                    adapter.refresh()
                    layoutManager.scrollToPosition(0)

                }
            }
            array[2] -> {
                {
                    //无侧边栏
                }
            }
            array[3] -> {
                {
                    layoutManager.scrollToPosition(0)
                }
            }
            else -> {
                {
                    //do nothing
                }
            }
        }
    }


    private fun showReferenceDialog(reference: String) {
        Log.e("Simsim", "create reference dialog with reference: $reference")
        lifecycleScope.launch {
            val referenceId = try {
                reference.replace("\\D".toRegex(), "").toLong()
            } catch (e: Exception) {
                Log.e(LOG_TAG, e.stackTraceToString())
                31163008
            }
            val list = viewModel.currentReplyThreads
            val index = list.indexOfFirst {
                it.replyThreadId == referenceId
            }
            val holderBinding =
                DetailRecyclerviewViewholderBinding.inflate(layoutInflater, binding.root, false)
            holderBinding.firstRowDetail.visibility = View.VISIBLE
            holderBinding.secondRowDetail.visibility = View.VISIBLE
            holderBinding.firstRowDetailPlaceholder.visibility = View.GONE
            holderBinding.secondRowDetailPlaceholder.visibility = View.GONE
            when (index) {
                in (0..list.size) -> {
                    val referenceThread = list[index]
                    adapter.bindHolder(
                        binding = holderBinding, thread = referenceThread
                    )
                    MaterialAlertDialogBuilder(requireContext()).setView(holderBinding.root).show()
                }
                else -> {
                    val url = "https://adnmb3.com/m/t/$referenceId"
                    Log.e("Simsim", "request for thread detail:$url")
                    val response = viewModel.networkService.getHtmlStringByPage(url)
                    val thread: ReplyThread = if (response != null) {
                        val doc = Jsoup.parse(response)
                        val poThreadDiv =
                            doc.selectFirst("div[class=uk-container h-threads-container]")
                        if (poThreadDiv == null) {
                            ReplyThread(replyThreadId = 888, poThreadId = 888, section = "")
                        } else {
                            val poThread = AislandRepo.divToBasicThread(
                                poThreadDiv,
                                isPo = true,
                                section = "",
                                poThreadId = 0L,
                            )
                            poThread
                        }

                    } else {
                        ReplyThread(replyThreadId = 888, poThreadId = 888, section = "")
                    }
                    adapter.bindHolder(
                        binding = holderBinding, thread = thread
                    )
                    MaterialAlertDialogBuilder(requireContext()).setView(holderBinding.root).show()
                }
            }


        }
    }


    private fun observeRecyclerViewFlow() {
        if (args.isSaved){
            lifecycleScope.launch {
                val poThread=viewModel.database.threadDao().getSavedPoThread(args.ThreadId).toPoThread()
                launch {
                    viewModel.setSavedReplyThreadFlow(poThread).collectLatest {
                        Log.e("Simsim", "got thread detail data:$it")
                        adapter.submitData(it)
//                    Log.e(LOG_TAG, "detail threads:${it}")
                    }
                }
            }

        }else{
            lifecycleScope.launch {
                viewModel.detailFlow.collectLatest {
                    Log.e("Simsim", "got thread detail data:$it")
                    adapter.submitData(it)
//                    Log.e(LOG_TAG, "detail threads:${it}")
                }
            }
        }

    }

    private fun setupRecyclerView() {
        adapter = DetailRecyclerViewAdapter(
            fragment = this,
            imageClickListener = { imageUrl ->
                val action = MainFragmentDirections.actionGlobalImageDetailFragment(imageUrl)
                findNavController().navigate(action)
            },
            referenceClickListener = { reference ->
                showReferenceDialog(reference)
            },
            popupMenuItemClickListener = { menuItem, replyThread ->
                when (menuItem.itemId) {
                    R.id.detail_popup_menu_reply -> {
                        newThreadReply(
                            target = TARGET_THREAD,
                            prefill = ">>${replyThread.replyThreadId}\n",
                            threadId = replyThread.poThreadId,
                            fId = ""
                        )
                        true
                    }
                    R.id.detail_popup_menu_report -> {
                        newThreadReply(
                            target = TARGET_SECTION,
                            prefill = ">>${replyThread.replyThreadId}\n举报理由：",
                            threadId = 0,
                            fId = "18"
                        )
                        true
                    }
                    R.id.detail_popup_menu_copy -> {
                        val clipboardManager =
                            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        ClipData.newPlainText("Copied Text", replyThread.content).also {
                            clipboardManager.setPrimaryClip(it)
                        }
                        Snackbar.make(binding.detailDialogLayout, "已复制至剪切板", Snackbar.LENGTH_LONG)
                            .setAction("分享") {
                                shareText(replyThread.content)
                            }
                            .show()
                        true
                    }
                    else -> {
                        false
                    }
                }
            },
            itemClickListener = {}
        )
        binding.detailDialogRecyclerView.adapter =
            adapter.withLoadStateHeader(MainLoadStateAdapter { adapter.retry() })
        layoutManager = LinearLayoutManager(context)
        binding.detailDialogRecyclerView.layoutManager = layoutManager
        binding.detailDialogRecyclerView.isMotionEventSplittingEnabled = false
    }

    private fun setupSwipeRefreshLayout() {
        val swipeRefreshLayout = binding.detailSwipeRefreshLayout
        swipeRefreshLayout.setColorSchemeResources(R.color.colorSecondary)
        swipeRefreshLayout.setOnRefreshListener {
            adapter.refresh()
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupFAB()
        doWhenReplySuccess()
        setupRecyclerView()
        setupSwipeRefreshLayout()
        observeRecyclerViewFlow()
        observeThreadStarStatus()
        handleLoadingImage()

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
                R.id.detail_fragment_menu_add -> {
                    newThreadReply(
                        target = TARGET_THREAD,
                        prefill = "",
                        threadId = args.ThreadId,
                        fId = ""
                    )
                    true
                }
                R.id.detail_fragment_menu_report -> {
                    newThreadReply(
                        target = TARGET_SECTION,
                        prefill = ">>${args.ThreadId}\n举报理由：",
                        threadId = 0,
                        fId = "18"
                    )
                    true
                }
                R.id.detail_fragment_menu_share -> {
                    val sharedText = viewModel.currentPoThread?.let { poThread ->
                        "${poThread.content}\nhttps://adnmb3.com/t/${poThread.threadId}"
                    } ?: "https://adnmb3.com/Forum"
                    shareText(sharedText)
                    true
                }
                R.id.detail_fragment_menu_star -> {
                    starThread(args.ThreadId)
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

    private fun shareText(sharedText: String) {
        (requireActivity() as MainActivity).shareText(sharedText)
    }

    private fun starThread(threadId: Long) {
        viewModel.starPoThread(threadId)
    }


    private fun newThreadReply(target: String, prefill: String, threadId: Long, fId: String) {
        val action = if (target == TARGET_THREAD) {
            DetailDialogFragmentDirections.actionGlobalNewDraftFragment(
                target = TARGET_THREAD,
                threadId = threadId,
                prefillText = prefill
            )
        } else {
            DetailDialogFragmentDirections.actionGlobalNewDraftFragment(
                target = TARGET_SECTION,
                fId = fId,
                prefillText = prefill
            )
        }
        findNavController().navigate(action)
        viewModel.isMainFragment.value = false
    }

    fun doWhenReplySuccess() {
        lifecycleScope.launch {
            viewModel.successReply.observe(viewLifecycleOwner) { success ->
                if (success) {
                    Snackbar.make(binding.detailDialogLayout, "发串成功", Snackbar.LENGTH_LONG)
                        .show()
                } else {
                    //todo
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        viewModel.isMainFragment.value = true
    }

}