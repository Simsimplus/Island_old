package com.simsim.island.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.simsim.island.R
import com.simsim.island.databinding.NewDraftFragmentBinding


class NewDraftFragment(val target: String,val targetKeyWord: String) : Fragment() {
    private val viewModel:MainViewModel by activityViewModels()
    private lateinit var binding:NewDraftFragmentBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= NewDraftFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDetach() {
        super.onDetach()
        when(target){
            "thread"->{viewModel.isMainFragment.value=false}
            "section"->{viewModel.isMainFragment.value=true}
        }

    }

    companion object {
        @JvmStatic
        fun newInstance(target: String, targetKeyWord: String) =
            NewDraftFragment(target, targetKeyWord)
    }
}