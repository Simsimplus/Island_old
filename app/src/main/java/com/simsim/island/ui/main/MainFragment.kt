package com.simsim.island.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.simsim.island.MainActivity
import com.simsim.island.R
import com.simsim.island.adapter.DrawerRecyclerViewAdapter
import com.simsim.island.adapter.MainRecyclerViewAdapter
import com.simsim.island.databinding.MainFragmentBinding
import com.simsim.island.util.LOG_TAG
import com.simsim.island.util.OnSwipeListener
import com.simsim.island.util.threadIdPattern
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@AndroidEntryPoint
class MainFragment : Fragment() {
    private lateinit var binding: MainFragmentBinding
    internal lateinit var adapter: MainRecyclerViewAdapter
    private lateinit var drawAdapter: DrawerRecyclerViewAdapter
    internal lateinit var layoutManager: LinearLayoutManager
    private lateinit var mainFlowJob: Job
//    private var loadingImageId =R.drawable.ic_blue_ocean1




    private val viewModel: MainViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MainFragmentBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        requestPermissions()
        setupFAB()
//        randomRefreshImage()
//        viewModel.newSearchQuery.observe(viewLifecycleOwner){query->
//            if (!query.matches(threadIdPattern)){
//                Snackbar.make(binding.mainCoordinatorLayout,"请直接输入串号，如337845818", Snackbar.LENGTH_SHORT).show()
//            }else{
//                Log.e(LOG_TAG,"receive query thread id:$query")
//                val action = MainFragmentDirections.actionMainFragmentToDetailDialogFragment(
//                    query.toLong()
//                )
//                findNavController().navigate(action)
//                viewModel.setDetailFlow(query.toLong())
//                viewModel.isMainFragment.value = false
//            }
//        }
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

                adapter.refresh()
            },
            onSwipeRight = {
                Log.e(LOG_TAG, "swipe left")
                binding.drawerLayout.openDrawer(binding.navigationView)
            },
            onSwipeTop = {}
        ))
        binding.fabAdd.setOnClickListener {
            newThread()
        }
    }

    private fun newThread() {
        val action=MainFragmentDirections.actionGlobalNewDraftFragment(target = "section",sectionName = viewModel.currentSectionName?:"",fId =viewModel.currentSectionId?:"" )
        findNavController().navigate(action)
        viewModel.isMainFragment.value = false
    }

    private fun setupDrawerSections() {
        lifecycleScope.launch {
            viewModel.database.sectionDao().getAllSection().map {
                it.map { section ->
                    section.sectionName
                }
            }.distinctUntilChanged()
                .collect { sectionList ->
                    val drawerLayout = binding.drawerLayout
                    val drawer = binding.navigationView
                    val drawerMenu = binding.navigationView.menu
                    sectionList.forEach { sectionName ->
                        drawerMenu.add(sectionName)
                    }
                    drawer.setNavigationItemSelectedListener { menuItem ->
                        drawer.setCheckedItem(menuItem)
                        drawerLayout.close()
                        binding.mainToolbar.title = menuItem.title
                        viewModel.setMainFlow(menuItem.title.toString())
                        observeMainFlow()
                        true
                    }

                }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar()
        initialMainFlow()
        setupRecyclerView()
        setupSwipeRefresh()
        setupDrawerSections()
        observingDataChange()
        handleLaunchLoading()
//        setupChips()

        super.onViewCreated(view, savedInstanceState)
    }

//    private fun setupChips() {
//        binding.mainFragmentChips.setOnCheckedChangeListener { group, checkedId ->
//    //            group.check(checkedId)
//            val chip = group.findViewById<Chip>(checkedId)
//            Log.e(LOG_TAG, "chip tapped:${chip.text}")
//
//            viewModel.setMainFlow(chip.text.toString())
//            observeMainFlow()
//
//        }
//    }


    private fun observingDataChange() {

        viewModel.isMainFragment.observe(viewLifecycleOwner) {
            binding.mainRecyclerView.suppressLayout(!it)
        }
    }

    private fun initialMainFlow() {
        lifecycleScope.launch {
            viewModel.database.sectionDao().getAllSection().take(1).collect {
                val sectionName =if (it.isNotEmpty()) it[0].sectionName else "综合版1"
                Log.e(LOG_TAG, "first sectionName:$sectionName")
                viewModel.setMainFlow(sectionName)
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
                when(loadStates.refresh){
                    is LoadState.Loading->{
                        binding.loadingImage.visibility = View.VISIBLE
                        binding.fabAdd.visibility = View.INVISIBLE

                        Glide.with(this@MainFragment).load(viewModel.randomLoadingImage).into(binding.loadingImage)
                    }
                    is LoadState.Error->{
                        binding.loadingImage.visibility = View.VISIBLE
                        binding.fabAdd.visibility = View.INVISIBLE
                        Glide.with(this@MainFragment).load(R.drawable.ic_loading_page_failed).into(binding.loadingImage)
                        Snackbar
                            .make(binding.root,R.string.loading_page_fail_info,Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.loading_fail_retry)){
                                adapter.retry()
                            }
                            .show()
                    }
                    else->{
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

    private fun setupDrawerRecyclerView() {


    }

    private fun setupRecyclerView() {
        adapter = MainRecyclerViewAdapter(this, { imageUrl ->
            val action = MainFragmentDirections.actionGlobalImageDetailFragment(imageUrl)
            findNavController().navigate(action)
        }) { poThread ->
            viewModel.currentPoThread = poThread
            val action = MainFragmentDirections.actionMainFragmentToDetailDialogFragment(
                poThread.threadId
            )
            findNavController().navigate(action)
            viewModel.setDetailFlow(poThread.threadId)
            viewModel.isMainFragment.value = false
        }
        binding.mainRecyclerView.adapter = adapter
//            .withLoadStateFooter(MainLoadStateAdapter(adapter::retry))
        layoutManager = LinearLayoutManager(context)
        binding.mainRecyclerView.layoutManager = layoutManager
    }


    private fun setupToolbar() {
        val toolbar = binding.mainToolbar

        toolbar.setNavigationIcon(R.drawable.ic_round_menu_24)
        toolbar.setNavigationOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(binding.navigationView)) {
                binding.drawerLayout.closeDrawer(binding.navigationView)
            } else {
                binding.drawerLayout.openDrawer(binding.navigationView)
            }
        }
        binding.navigationView.setNavigationItemSelectedListener {
            it.isChecked = true
            binding.drawerLayout.close()
            true
        }
//        toolbar.title = viewModel.currentSection.value


        toolbar.inflateMenu(R.menu.main_toolbar_menu)
//        val searchManager = requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
//        val searchItem=toolbar.menu.findItem(R.id.menu_item_search)
//        val searchView=searchItem.actionView as SearchView
//        searchView.apply {
//            setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
//            isIconified=true
//            setOnQueryTextFocusChangeListener { _, hasFocus ->
//                if (!hasFocus){
//                    searchItem.collapseActionView()
//                    searchView.setQuery("",false)
//                }
//            }
//        }
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
//                R.id.menu_item_refresh -> {
////                    adapter.refresh()
////                    layoutManager.scrollToPosition(0)
//                    Log.e("Simsim", "refresh item pressed")
//                    true
//                }
//            R.id.menu_item_search->{}
                else -> false
            }
        }
    }

}