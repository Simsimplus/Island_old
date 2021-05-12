package com.simsim.island.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.simsim.island.MainActivity
import com.simsim.island.R
import com.simsim.island.adapter.MainRecyclerViewAdapter
import com.simsim.island.dataStore
import com.simsim.island.databinding.MainFragmentBinding
import com.simsim.island.dp2PxScale
import com.simsim.island.model.BlockRule
import com.simsim.island.model.BlockTarget
import com.simsim.island.model.PoThread
import com.simsim.island.model.Section
import com.simsim.island.preferenceKey.PreferenceKey
import com.simsim.island.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : Fragment() {
    private lateinit var binding: MainFragmentBinding
    internal lateinit var adapter: MainRecyclerViewAdapter
    internal lateinit var layoutManager: LinearLayoutManager
    private lateinit var mainFlowJob: Job
    private lateinit var retryIfNotReady: Job
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawer: NavigationView
    private lateinit var fab: FloatingActionButton
    private var isFABEnable = true
    private lateinit var preferenceKey: PreferenceKey
//    private var loadingImageId =R.drawable.ic_blue_ocean1


    private val viewModel: MainViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MainFragmentBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        drawerLayout = binding.drawerLayout
        drawer = binding.sectionDrawer
        fab = binding.fabAdd
        preferenceKey = PreferenceKey(requireContext())
        requestPermissions()

        initialMainFlow()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()

        setupFAB()
        handleSavedInstanceState()

        setupSwipeRefresh()
        setupDrawerSections()
        observingDataChange()
        handleLaunchLoading()
        doWhenPostSuccess()

    }

    private fun requestPermissions() {
        lifecycleScope.launch {
            (requireActivity() as MainActivity).requestPermission.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                )
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFAB() {
        lifecycleScope.launch {
            launch {
                requireContext().dataStore.data.collectLatest { settings ->
                    settings[booleanPreferencesKey(preferenceKey.enableFabKey)]?.let { enable ->
                        isFABEnable = enable
                        binding.mainToolbar.menu.findItem(R.id.main_fragment_menu_add).isVisible =
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
                    Log.e(LOG_TAG, "setting collected")
                }
            }
            launch {
                fab.setOnClickListener {
                    Log.e(LOG_TAG, "main fab clicked")
                    newThread()
                }
            }
            launch {
                Log.e(LOG_TAG, "test")
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
                    if (drawerLayout.isDrawerOpen(drawer)) {
                        drawerLayout.closeDrawer(drawer)
                    } else {
                        layoutManager.scrollToPosition(0)
                        adapter.refresh()
                    }
                }
            }
            array[2] -> {
                {
                    drawerLayout.openDrawer(drawer)
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

    private fun newThread() {
        lifecycleScope.launch {
            val action = MainFragmentDirections.actionGlobalNewDraftFragment(
                target = TARGET_SECTION,
                sectionName = viewModel.currentSectionName ?: "",
                fId = viewModel.currentSectionId
            )
            findNavController().navigate(action)
            viewModel.isMainFragment.value = false
        }
    }

    private fun setupDrawerSections() {
        lifecycleScope.launch {
            val menu = drawer.menu
            drawer.setNavigationItemSelectedListener {

                true
            }
            viewModel.sectionList.distinctUntilChanged().collectLatest { sectionList ->
                var index = 10
                sectionList.groupBy {
                    it.group
                }.forEach { (group, list) ->
                    index += 1
                    menu.addSubMenu(0, index, 0, group).also { subMenu ->
                        subMenu.item
                            .setOnMenuItemClickListener { item ->
                                Log.e(LOG_TAG, "subMenu item clicked")
                                if (subMenu.hasVisibleItems()) {
                                    subMenu.children.forEach { child ->
                                        child.isVisible = false
                                    }
                                } else {
                                    subMenu.children.forEach { child ->
                                        child.isVisible = true
                                    }
                                }
                                true
                            }
                        list.forEach { section ->
                            subMenu.add(section.sectionName).also {
                                it.setOnMenuItemClickListener { item ->
                                    Log.e(LOG_TAG, "drawer item index:${item.itemId}")
                                    drawer.setCheckedItem(item)
                                    onSectionClicked(section)
                                    true
                                }
                            }
                        }
                    }
                }
            }
        }
//        lifecycleScope.launch {
//            viewModel.database.sectionDao().getAllSection().distinctUntilChanged()
//                .collect { sectionList ->
////                    val drawerLayout = binding.drawerLayout
////                    val drawer = binding.navigationView
//                    val adapters = mutableListOf<DrawerRecyclerViewAdapter>()
//                    val concatAdapterConfig = ConcatAdapter
//                        .Config
//                        .Builder()
//                        .setIsolateViewTypes(false)
//                        .build()
//                    val sectionGroupBy = sectionList.groupBy {
//                        it.group
//                    }
//                    sectionGroupBy.forEach { (group, list) ->
//                        val groupAdapter =
//                            DrawerRecyclerViewAdapter(SectionGroup(group, list)) { section ->
//                                onSectionClicked(section)
//                            }
//                        adapters.add(groupAdapter)
//                    }
//                    val concatAdapter = ConcatAdapter(concatAdapterConfig, adapters)
//                    with(binding.drawerRecyclerView) {
//                        layoutManager = LinearLayoutManager(requireContext())
//                        adapter = concatAdapter
//                    }
//
//                }
//        }
    }

    private fun onSectionClicked(section: Section) {
        drawerLayout.close()
        binding.mainToolbar.title = section.sectionName
        viewModel.currentSectionId = section.fId
        viewModel.setMainFlow(
            section.sectionName,
            section.sectionUrl
        )
        observeMainFlow()
    }


    private fun handleSavedInstanceState() {
        viewModel.savedInstanceState.observe(viewLifecycleOwner) {
            toDetailFragment(it)
        }
    }


    private fun observingDataChange() {
        viewModel.isMainFragment.observe(viewLifecycleOwner) {
            binding.mainRecyclerView.suppressLayout(!it)
        }
    }

    private fun initialMainFlow() {
        lifecycleScope.launch {
            viewModel.sectionList.take(1).collect {
                val sectionId = if (it.isNotEmpty()) it[0].fId else "4"
                val sectionName = if (it.isNotEmpty()) it[0].sectionName else "综合版1"
                val sectionUrl =
                    if (it.isNotEmpty()) it[0].sectionUrl else "https://adnmb3.com/f/%E7%BB%BC%E5%90%88%E7%89%881"
                Log.e(LOG_TAG, "first sectionName:$sectionName")
                viewModel.setMainFlow(sectionName = sectionName, sectionUrl = sectionUrl)
                viewModel.currentSectionId = sectionId
                observeMainFlow()
                binding.mainToolbar.title = sectionName
            }
        }
    }

    private fun observeMainFlow() {
        layoutManager.scrollToPosition(0)
        try {
            mainFlowJob.cancel()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "mainFlowJob:${e.stackTraceToString()}")
        }
        mainFlowJob = lifecycleScope.launch {
            viewModel.mainFlow.collectLatest {
                adapter.submitData(it)
            }
        }
        mainFlowJob.start()
    }

    private fun handleLaunchLoading() {
        lifecycleScope.launch {
            var runOnce = true
            adapter.loadStateFlow.collectLatest { loadStates ->
                when (loadStates.refresh) {
                    is LoadState.Loading -> {
                        retryIfNotReady = lifecycleScope.launch {
                            if (runOnce) {
                                try {
                                    Log.e(LOG_TAG, "main retry")
                                    adapter.refresh()
                                } catch (e: Exception) {
                                    Log.e(LOG_TAG, "retry failed:${e.stackTraceToString()}")
                                } finally {
                                    runOnce = false
                                }
                            }

                        }
                        retryIfNotReady.start()
                        binding.loadingImage.visibility = View.VISIBLE
                        binding.fabAdd.visibility = View.INVISIBLE
//                        Glide.with(this@MainFragment).load(viewModel.randomLoadingImage)
//                            .into(binding.loadingImage)
                    }
                    is LoadState.Error -> {
                        binding.loadingImage.visibility = View.VISIBLE
                        binding.fabAdd.visibility = View.INVISIBLE
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
                    }
                    else -> {
                        try {
                            retryIfNotReady.cancel()
                        } catch (e: Exception) {
                            Log.e(
                                LOG_TAG,
                                "cancel retryIfNotReady failed:${e.stackTraceToString()}"
                            )
                        }
                        binding.loadingImage.visibility = View.GONE
                        if (isFABEnable) {
                            binding.fabAdd.visibility = View.VISIBLE
                        } else {
                            binding.fabAdd.visibility = View.INVISIBLE
                        }

                    }
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        lifecycleScope.launch {
            val swipeRefreshLayout = binding.swipeFreshLayout
            swipeRefreshLayout.setColorSchemeResources(R.color.colorSecondary)
            swipeRefreshLayout.setOnRefreshListener {
                Log.e("Simsim", "main recycler view refresh by swipeRefreshLayout")
                layoutManager.scrollToPosition(0)
                adapter.refresh()
                if (swipeRefreshLayout.isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }


    private fun setupRecyclerView() {
        adapter = MainRecyclerViewAdapter(
            fragment = this,
            imageClickListener = { imageUrl ->
                val action = MainFragmentDirections.actionGlobalImageDetailFragment(imageUrl)
                findNavController().navigate(action)
            },
            popupMenuItemClickListener = { menuItem, poThread ->
                when (menuItem.itemId) {
                    R.id.main_popup_menu_block_thread -> {
                        blockThreadById(poThread)
                        true
                    }
                    R.id.main_popup_menu_block_uid -> {
                        blockThreadByUid(poThread)
                        true
                    }
                    R.id.main_popup_menu_star -> {
                        viewModel.starPoThread(poThread.threadId)
                        true
                    }
                    else -> {
                        false
                    }
                }

            },
            clickListener = { poThread ->
                viewModel.currentPoThread = poThread
                toDetailFragment(poThread.threadId)
            }
        )
        binding.mainRecyclerView.adapter = adapter
//            .withLoadStateFooter(MainLoadStateAdapter(adapter::retry))
        layoutManager = LinearLayoutManager(context)
        binding.mainRecyclerView.layoutManager = layoutManager
        binding.mainRecyclerView.isMotionEventSplittingEnabled = false
//        val callBack = object :
//            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
//            override fun onMove(
//                recyclerView: RecyclerView,
//                viewHolder: RecyclerView.ViewHolder,
//                target: RecyclerView.ViewHolder
//            ): Boolean {
//                return false
//            }
//
//            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//                Log.e(LOG_TAG, "rv item swiped")
//                val position = viewHolder.absoluteAdapterPosition
//                adapter.getItemByPosition(position)?.let {poThread->
//
//                    blockThreadById(poThread)
//                }
//            }
//
//        }
//        ItemTouchHelper(callBack).also {
//            it.attachToRecyclerView(binding.mainRecyclerView)
//        }
    }

    private fun blockThreadById(poThread: PoThread) {
        lifecycleScope.launch {
            viewModel.updatePoThread(poThread.apply {
                isShow = false
            })
            var shouldBlock = true
            Snackbar.make(binding.mainCoordinatorLayout, "已屏蔽此串", Snackbar.LENGTH_SHORT)
                .setAction("取消") {
                    shouldBlock = false
                    lifecycleScope.launch {
                        viewModel.updatePoThread(poThread.apply {
                            isShow = true
                        })
                    }
                }
                .addCallback(
                    object : Snackbar.Callback() {
                        override fun onDismissed(
                            transientBottomBar: Snackbar?,
                            event: Int
                        ) {
                            if (shouldBlock) {
                                lifecycleScope.launch {
                                    viewModel.insertBlockRule(
                                        BlockRule(
                                            index = 0,
                                            rule = poThread.threadId.toString(),
                                            target = BlockTarget.TargetThreadId
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
                .show()
        }
    }

    private fun blockThreadByUid(poThread: PoThread) {
        lifecycleScope.launch {
            val list = viewModel.getAllPoThreadsByUid(poThread.uid)
            list.forEach {
                viewModel.updatePoThread(it.apply {
                    isShow = false
                })
            }
            var shouldBlock = true
            Snackbar
                .make(binding.mainCoordinatorLayout, "已屏蔽此饼干", Snackbar.LENGTH_SHORT)
                .setAction("取消") {
                    shouldBlock = false
                    lifecycleScope.launch {
                        list.forEach {
                            viewModel.updatePoThread(it.apply {
                                isShow = true
                            })
                        }
                    }
                }
                .addCallback(
                    object : Snackbar.Callback() {
                        override fun onDismissed(
                            transientBottomBar: Snackbar?,
                            event: Int
                        ) {
                            if (shouldBlock) {
                                lifecycleScope.launch {
                                    viewModel.insertBlockRule(
                                        BlockRule(
                                            index = 0,
                                            rule = poThread.uid,
                                            target = BlockTarget.TargetUid
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
                .show()
        }
    }

    private fun toDetailFragment(poThreadId: Long) {
        val action = MainFragmentDirections.actionMainFragmentToDetailDialogFragment(
            poThreadId
        )
        findNavController().navigate(action)
        viewModel.setDetailFlow(poThreadId)
        viewModel.isMainFragment.value = false
    }


    private fun setupToolbar() {
        lifecycleScope.launch {
            val toolbar = binding.mainToolbar
            toolbar.inflateMenu(R.menu.main_toolbar_menu)
            toolbar.setNavigationIcon(R.drawable.ic_round_menu_24)
            toolbar.setNavigationOnClickListener {
                if (drawerLayout.isDrawerOpen(drawer)) {
                    drawerLayout.closeDrawer(drawer)
                } else {
                    drawerLayout.openDrawer(drawer)
                }
            }
            drawer.setNavigationItemSelectedListener {
                it.isChecked = true
                drawerLayout.close()
                true
            }
//        toolbar.title = viewModel.currentSection.value


            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.main_fragment_menu_add -> {
                        newThread()
                        true
                    }
                    R.id.menu_item_refresh -> {
                        adapter.refresh()
                        layoutManager.scrollToPosition(0)
                        Log.e("Simsim", "refresh item pressed")
                        true
                    }
                    R.id.menu_item_search -> {
                        val editText =
                            layoutInflater.inflate(
                                R.layout.search_edit_text,
                                null,
                                false
                            ) as EditText
                        MaterialAlertDialogBuilder(requireContext())
                            .setView(editText)
                            .setPositiveButton("去串") { dialog: DialogInterface, _ ->
                                val threadId = editText.text.toString().toLongOrNull()
                                threadId?.let {
                                    val action =
                                        MainFragmentDirections.actionMainFragmentToDetailDialogFragment(
                                            threadId
                                        )
                                    findNavController().navigate(action)
                                    viewModel.setDetailFlow(threadId)
                                    viewModel.isMainFragment.value = false
                                    dialog.dismiss()
                                } ?: kotlin.run {
                                    Toast.makeText(requireContext(), "去不了啊，请重试", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                            .setNegativeButton("不去了") { dialog: DialogInterface, _ ->
                                dialog.dismiss()

                            }
                            .show()
                        true
                    }
                    R.id.main_menu_setting -> {
                        val action =
                            MainFragmentDirections.actionMainFragmentToSettingsDialogFragment()
                        findNavController().navigate(action)
                        true
                    }
                    R.id.main_menu_star_collection -> {
                        val action =
                            MainFragmentDirections.actionMainFragmentToStaredThreadDialogFragment()
                        findNavController().navigate(action)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    fun doWhenPostSuccess() {
        lifecycleScope.launch {
            viewModel.successPostOrReply.observe(viewLifecycleOwner) { success ->
                success?.let {
                    if (success) {
                        Snackbar.make(binding.mainCoordinatorLayout, "发串成功", Snackbar.LENGTH_LONG)
                            .show()
                    } else {
                        viewModel.errorPostOrReply.observe(viewLifecycleOwner) { error ->
                            error?.let {
                                Snackbar.make(
                                    binding.mainCoordinatorLayout,
                                    "发串失败[$error]",
                                    Snackbar.LENGTH_INDEFINITE
                                )
                                    .show()
                                viewModel.errorPostOrReply.value = null
                            }
                        }
                    }
                    viewModel.successPostOrReply.value = null
                }
            }
        }
    }

}