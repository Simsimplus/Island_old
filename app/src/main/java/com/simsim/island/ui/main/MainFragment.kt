package com.simsim.island.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.*
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.simsim.island.MainActivity
import com.simsim.island.R
import com.simsim.island.adapter.DrawerRecyclerViewAdapter
import com.simsim.island.adapter.MainRecyclerViewAdapter
import com.simsim.island.databinding.MainFragmentBinding
import com.simsim.island.model.SectionGroup
import com.simsim.island.util.LOG_TAG
import com.simsim.island.util.OnSwipeListener
import com.simsim.island.util.TARGET_SECTION
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : Fragment() {
    private lateinit var binding: MainFragmentBinding
    internal lateinit var adapter: MainRecyclerViewAdapter
    internal lateinit var layoutManager: LinearLayoutManager
    private lateinit var mainFlowJob: Job
    private lateinit var drawerLayout:DrawerLayout
    private lateinit var drawer:NavigationView
//    private var loadingImageId =R.drawable.ic_blue_ocean1


    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MainFragmentBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        drawerLayout=binding.drawerLayout
        drawer=binding.sectionDrawer
        requestPermissions()
        setupFAB()
        initialMainFlow()
        return binding.root
    }
    private fun requestPermissions() {
        (requireActivity() as MainActivity).requestPermission.launch(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFAB() {
        binding.fabAdd.setOnTouchListener(OnSwipeListener(
            requireContext(),
            onSwipeBottom = {
                layoutManager.scrollToPosition(0)
            },
            onSwipeLeft = {
                if (drawerLayout.isDrawerOpen(drawer)){
                    drawerLayout.closeDrawer(drawer)
                }else{
                    adapter.refresh()
                }
            },
            onSwipeRight = {
                Log.e(LOG_TAG, "swipe left")
                drawerLayout.openDrawer(drawer)
            },
            onSwipeTop = {}
        ))
        binding.fabAdd.setOnClickListener {
            newThread()
        }
    }

    private fun newThread() {
        val action = MainFragmentDirections.actionGlobalNewDraftFragment(
            target = TARGET_SECTION,
            sectionName = viewModel.currentSectionName ?: "",
            fId = viewModel.currentSectionId
        )
        findNavController().navigate(action)
        viewModel.isMainFragment.value = false
    }

    private fun setupDrawerSections() {
        lifecycleScope.launch {
            viewModel.database.sectionDao().getAllSection().distinctUntilChanged()
                .collect { sectionList ->
//                    val drawerLayout = binding.drawerLayout
//                    val drawer = binding.navigationView
                    val adapters = mutableListOf<DrawerRecyclerViewAdapter>()
                    val concatAdapterConfig=ConcatAdapter
                        .Config
                        .Builder()
                        .setIsolateViewTypes(false)
                        .build()
                    val sectionGroupBy = sectionList.groupBy {
                            it.group
                        }
                    sectionGroupBy.forEach { (group, list) ->
                        val groupAdapter = DrawerRecyclerViewAdapter(SectionGroup(group, list)){ section->
                            drawerLayout.close()
                            binding.mainToolbar.title = section.sectionName
                            viewModel.currentSectionId=section.fId
                            viewModel.setMainFlow(
                                section.sectionName,
                                section.sectionUrl
                            )
                            observeMainFlow()
                        }
                        adapters.add(groupAdapter)
                    }
                    val concatAdapter=ConcatAdapter(concatAdapterConfig,adapters)
                    with(binding.drawerRecyclerView){
                        layoutManager=LinearLayoutManager(requireContext())
                        adapter=concatAdapter
                    }

                }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar()

        handleSavedInstanceState()
        setupRecyclerView()
        setupSwipeRefresh()
        setupDrawerSections()
        observingDataChange()
        handleLaunchLoading()
        doWhenPostSuccess()
//        setupChips()

        super.onViewCreated(view, savedInstanceState)
    }

    private fun handleSavedInstanceState() {
        viewModel.savedInstanceState.observe(viewLifecycleOwner){
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
            viewModel.database.sectionDao().getAllSection().take(1).collect {
                val sectionId = if (it.isNotEmpty()) it[0].fId else "4"
                val sectionName = if (it.isNotEmpty()) it[0].sectionName else "综合版1"
                val sectionUrl =
                    if (it.isNotEmpty()) it[0].sectionUrl else "https://adnmb3.com/m/f/%E7%BB%BC%E5%90%88%E7%89%881"
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
            adapter.loadStateFlow.collectLatest { loadStates ->
                when (loadStates.refresh) {
                    is LoadState.Loading -> {
                        binding.loadingImage.visibility = View.VISIBLE
                        binding.fabAdd.visibility = View.INVISIBLE

                        Glide.with(this@MainFragment).load(viewModel.randomLoadingImage)
                            .into(binding.loadingImage)
                    }
                    is LoadState.Error -> {
                        binding.loadingImage.visibility = View.VISIBLE
                        binding.fabAdd.visibility = View.INVISIBLE
                        viewModel.database.threadDao().isThereAnyPoThreadInDB().collectLatest {
                            if (it){
                                binding.loadingImage.visibility = View.INVISIBLE
                                binding.fabAdd.visibility = View.VISIBLE
                            }
                        }
                        Glide.with(this@MainFragment).load(viewModel.randomLoadingImage)
                            .into(binding.loadingImage)
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
                        binding.loadingImage.visibility = View.GONE
                        binding.fabAdd.visibility = View.VISIBLE
                        binding.fabAdd.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        val swipeRefreshLayout = binding.swipeFreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            Log.e("Simsim", "main recycler view refresh by swipeRefreshLayout")
            adapter.refresh()
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }



    private fun setupRecyclerView() {
        adapter = MainRecyclerViewAdapter(this, { imageUrl ->
            val action = MainFragmentDirections.actionGlobalImageDetailFragment(imageUrl)
            findNavController().navigate(action)
        }) { poThread ->
            viewModel.currentPoThread = poThread
            toDetailFragment(poThread.threadId)
        }
        binding.mainRecyclerView.adapter = adapter
//            .withLoadStateFooter(MainLoadStateAdapter(adapter::retry))
        layoutManager = LinearLayoutManager(context)
        binding.mainRecyclerView.layoutManager = layoutManager
    }
    private fun toDetailFragment(poThreadId:Long){
        val action = MainFragmentDirections.actionMainFragmentToDetailDialogFragment(
            poThreadId
        )
        findNavController().navigate(action)
        viewModel.setDetailFlow(poThreadId)
        viewModel.isMainFragment.value = false
    }



    private fun setupToolbar() {
        val toolbar = binding.mainToolbar

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


        toolbar.inflateMenu(R.menu.main_toolbar_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_item_refresh -> {
                    adapter.refresh()
                    layoutManager.scrollToPosition(0)
                    Log.e("Simsim", "refresh item pressed")
                    true
                }
            R.id.menu_item_search->{
                val editText=layoutInflater.inflate(R.layout.search_edit_text,null,false) as EditText
                MaterialAlertDialogBuilder(requireContext())
                    .setView(editText)
                    .setPositiveButton("去串"){ dialog: DialogInterface, _ ->
                        val threadId=editText.text.toString().toLongOrNull()
                        threadId?.let {
                            val action =MainFragmentDirections.actionMainFragmentToDetailDialogFragment(threadId)
                            findNavController().navigate(action)
                            viewModel.setDetailFlow(threadId)
                            viewModel.isMainFragment.value = false
                            dialog.dismiss()
                        }?: kotlin.run {
                            Toast.makeText(requireContext(),"去不了啊，请重试",Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("不去了"){dialog: DialogInterface, _ ->
                        dialog.dismiss()

                    }
                    .show()
                true
            }
                R.id.main_menu_setting->{
                    val action=MainFragmentDirections.actionMainFragmentToSettingsDialogFragment()
                    findNavController().navigate(action)
                    true
                }
                else -> false
            }
        }
    }
    fun doWhenPostSuccess(){
        lifecycleScope.launch {
            viewModel.successPost.observe(viewLifecycleOwner){success->
                if (success){
                    Snackbar.make(binding.mainCoordinatorLayout,"发串成功",Snackbar.LENGTH_LONG)
                        .show()
                }else{
                    //todo
                }
            }
        }
    }

}