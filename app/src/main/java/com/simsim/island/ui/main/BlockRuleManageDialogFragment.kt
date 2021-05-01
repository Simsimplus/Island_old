package com.simsim.island.ui.main

import android.annotation.SuppressLint
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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.simsim.island.R
import com.simsim.island.adapter.BlockRuleRecyclerViewAdapter
import com.simsim.island.dataStore
import com.simsim.island.databinding.BlockRuleDialogFragmentBinding
import com.simsim.island.dp2PxScale
import com.simsim.island.preferenceKey.PreferenceKey
import com.simsim.island.util.LOG_TAG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BlockRuleManageDialogFragment : DialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: BlockRuleDialogFragmentBinding
    private lateinit var fab: FloatingActionButton
    private lateinit var preferenceKey: PreferenceKey
    private lateinit var adapter: BlockRuleRecyclerViewAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.fullscreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BlockRuleDialogFragmentBinding.inflate(inflater)
        fab = binding.fabAdd
        preferenceKey = PreferenceKey(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupFAB()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFAB() {
        lifecycleScope.launch {
            requireContext().dataStore.data.collectLatest { settings ->
                fab.setOnClickListener {
                    addNewBlockRule()
                    Log.e(LOG_TAG, "BlockRuleManageDialogFragment fab clicked")
                }
                settings[booleanPreferencesKey(preferenceKey.enableFabKey)]?.let { enable ->
                    binding.blockRuleToolbar.menu.findItem(R.id.block_rule_menu_add).isVisible =
                        !enable
                    fab.isVisible = enable
                }

                settings[booleanPreferencesKey(preferenceKey.fabDefaultSizeKey)]?.let { setSizeDefault ->
                    if (setSizeDefault) {
                        fab.customSize = FloatingActionButton.NO_CUSTOM_SIZE
                        fab.size = FloatingActionButton.SIZE_AUTO
                    } else {
                        settings[intPreferencesKey(preferenceKey.fabSizeSeekBarKey)]?.let { fabCustomSize ->
                            fab.customSize = (fabCustomSize * requireContext().dp2PxScale()).toInt()
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

    }

    private fun setupRecyclerView() {
        lifecycleScope.launch {
            adapter = BlockRuleRecyclerViewAdapter(
                requireContext(),
                list = listOf(),
                editButtonClickListener = { blockRule ->
                    lifecycleScope.launch {
                        val action =
                            BlockRuleManageDialogFragmentDirections.actionBlockRuleDialogFragmentToEditBlockRuleDialogFragment(
                                isNewOne = false,
                                blockRuleIndex = blockRule.index
                            )
                        findNavController().navigate(action)
                    }
                },
                switchOnCheckedChangeListener = { blockRule ->
                    lifecycleScope.launch {
                        viewModel.database.blockRuleDao().updateBlockRule(blockRule)
                    }
                },
                deleteButtonClickListener = { blockRule ->
                    lifecycleScope.launch {
                        viewModel.database.blockRuleDao().deleteBlockRule(blockRule)
                    }
                },
            )
            binding.blockRuleRecyclerView.adapter = adapter
            binding.blockRuleRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.blockRuleRecyclerView.isMotionEventSplittingEnabled = false
            viewModel.database.blockRuleDao().getAllBlockRulesFlow()
                .collectLatest { blockRuleList ->
                    adapter.submitList(blockRuleList)
                }
        }
    }

    private fun setupToolbar() {
        val toolbar = binding.blockRuleToolbar
        toolbar.inflateMenu(R.menu.block_rule_menu)
        toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24)
        toolbar.setNavigationOnClickListener {
            dismiss()
        }
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.block_rule_menu_add -> {
                    addNewBlockRule()
                    true
                }
                else -> {
                    false
                }
            }
        }
        toolbar.title = "屏蔽页"
    }

    private fun addNewBlockRule() {
        val action =
            BlockRuleManageDialogFragmentDirections.actionBlockRuleDialogFragmentToEditBlockRuleDialogFragment(
                isNewOne = true,
                blockRuleIndex = 0
            )
        findNavController().navigate(action)
    }
}