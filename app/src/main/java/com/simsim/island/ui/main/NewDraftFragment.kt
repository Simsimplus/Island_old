package com.simsim.island.ui.main

import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.simsim.island.MainActivity
import com.simsim.island.R
import com.simsim.island.databinding.NewDraftFragmentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        binding.draftToolbar.inflateMenu(R.menu.new_draft_tollbar_menu)
//    }

    override fun onDetach() {
        super.onDetach()
        when(target){
            "thread"->{viewModel.isMainFragment.value=false}
            "section"->{viewModel.isMainFragment.value=true}
        }

    }
    private val actionModeCallback=object : ActionMode.Callback{
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.new_draft_tollbar_menu,menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return if (item!=null){
                return when(item.itemId){
                    R.id.draft_emoji_pick->{
                        true
                    }
                    R.id.draft_menu_image_pick->{
                        true
                    }
                    R.id.draft_menu_send->{
                        true
                    }
                    else->{false}
                }
            }else{false}
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            parentFragmentManager.popBackStack()
        }

    }

    companion object {
        @JvmStatic
        fun newInstance(target: String, targetKeyWord: String) =
            NewDraftFragment(target, targetKeyWord)
    }
}