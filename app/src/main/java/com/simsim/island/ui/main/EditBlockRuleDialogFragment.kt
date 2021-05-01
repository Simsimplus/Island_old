package com.simsim.island.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simsim.island.R
import com.simsim.island.databinding.EditBlockRuleDialogFragmentBinding
import com.simsim.island.model.BlockRule
import com.simsim.island.model.BlockTarget
import com.simsim.island.preferenceKey.PreferenceKey
import com.simsim.island.util.LOG_TAG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditBlockRuleDialogFragment : DialogFragment(){
    private val viewModel:MainViewModel by activityViewModels()
    private lateinit var binding: EditBlockRuleDialogFragmentBinding
    private lateinit var toolbar: MaterialToolbar
    private lateinit var preferenceKey: PreferenceKey
    private val args:EditBlockRuleDialogFragmentArgs by navArgs()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.fullscreenDialog)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= EditBlockRuleDialogFragmentBinding.inflate(inflater)
        preferenceKey= PreferenceKey(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupInput()
    }

    private fun setupInput() {
        val nameInput=binding.nameInput
        val ruleInput=binding.ruleInput
        val isRegex=binding.isRegex
        val blockTargetTextView=binding.blockTargetTextView
        val blockTargets=enumValues<BlockTarget>().toList()
        val items= blockTargets.map {
            it.target
        }
        blockTargetTextView.setAdapter(ArrayAdapter(requireContext(),R.layout.spinner_viewholder,items))
        blockTargetTextView.setText(BlockTarget.TargetAll.target,false)
        toolbar.setOnMenuItemClickListener { menuItem->
            when(menuItem.itemId){
                R.id.new_block_rule_menu_save->{
                    val rule=ruleInput.editableText.toString()
                    if (rule.isBlank()){
                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage("规则不能为空")
                            .setNegativeButton("确认",null)
                            .show()
                    }else{
                        var name=nameInput.editableText.toString()
                        if (name.isBlank()){
                            name=rule
                        }
                        val target=blockTargets[items.indexOf(blockTargetTextView.text.toString())]
                        lifecycleScope.launch {
                            val blockRule=BlockRule(
                                index = args.blockRuleIndex,
                                rule=rule,
                                name=name,
                                isRegex = isRegex.isChecked,
                                target = target
                            ).also {
                                Log.e(LOG_TAG,it.toString())
                            }
                            if (args.isNewOne){
                                viewModel.database.blockRuleDao().insertBlockRule(
                                    blockRule
                                )
                            }else{
                                viewModel.database.blockRuleDao().updateBlockRule(
                                    blockRule
                                )
                            }

                        }
                    }


                    true
                }
                else->{
                    false
                }
            }
        }

    }


    private fun setupToolbar() {
        toolbar = binding.newBlockRuleToolbar
        toolbar.inflateMenu(R.menu.edit_block_rule_menu)
        toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24)
        toolbar.setNavigationOnClickListener {
            dismiss()
        }

        toolbar.title = "编辑规则"
    }
}