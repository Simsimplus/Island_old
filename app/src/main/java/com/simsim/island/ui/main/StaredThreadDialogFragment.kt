package com.simsim.island.ui.main


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.simsim.island.R
import com.simsim.island.adapter.MainRecyclerViewAdapter
import com.simsim.island.databinding.StaredThreadDialogFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StaredThreadDialogFragment : DialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: StaredThreadDialogFragmentBinding
    private lateinit var adapter: MainRecyclerViewAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.fullscreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = StaredThreadDialogFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        lifecycleScope.launch {
            launch{
                val threadList = viewModel.getAllSavedPoThread()
                adapter = MainRecyclerViewAdapter(
                    fragment = this@StaredThreadDialogFragment,
                    imageClickListener = { imageUrl ->
                        val action =
                            StaredThreadDialogFragmentDirections.actionGlobalImageDetailFragment(
                                imageUrl,
                                false
                            )
                        findNavController().navigate(action)
                    },
                    popupMenuItemClickListener = null,
                    clickListener = { poThread ->
                    viewModel.currentPoThread = poThread
                    toDetailFragment(poThread.threadId)
                    }
                )
                binding.starThreadRecyclerView.adapter = adapter
                binding.starThreadRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.starThreadRecyclerView.isMotionEventSplittingEnabled = false
                launch {
                    viewModel.setSavedPoThreadFlow().collectLatest {
                        adapter.submitData(it)
                    }
                    viewModel.savedPoThreadDataSetChanged.observe(viewLifecycleOwner){
                        if (it){
                            lifecycleScope.launch {
                                viewModel.setSavedPoThreadFlow().collectLatest {
                                    adapter.submitData(it)
                                }
                            }
                            viewModel.savedPoThreadDataSetChanged.value=false
                        }
                    }
                }
            }
        }

    }
    private fun toDetailFragment(poThreadId: Long) {
        val action = StaredThreadDialogFragmentDirections.actionStaredThreadDialogFragmentToStaredDetailDialogFragment(
            poThreadId
        )
        findNavController().navigate(action)
        viewModel.isMainFragment.value = false
    }

    private fun setupToolbar() {
        val toolbar = binding.starThreadToolbar
//        toolbar.inflateMenu(R.menu.setting_fragment_menu)
        toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24)
        toolbar.setNavigationOnClickListener {
            dismiss()
        }
        toolbar.title = "我的收藏"
    }
}