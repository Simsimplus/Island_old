package com.simsim.island.ui.main

import android.app.Dialog
import android.os.Bundle
import android.view.DragEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.simsim.island.R
import com.simsim.island.databinding.ImageDetailFragmentBinding

class ImageDetailFragment(val imageUrl:String) : BottomSheetDialogFragment() {
    internal lateinit var binding: ImageDetailFragmentBinding
    private val viewModel:MainViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val view=(it as BottomSheetDialog).findViewById<View>(R.id.image_layout)
            view?.layoutParams?.apply {
                height=viewModel.windowHeight
                view.layoutParams=this
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= ImageDetailFragmentBinding.inflate(inflater, container, false)
//        val url= arguments?.getCharSequence("imageUrl")
        Glide.with(this).load(imageUrl).into(binding.imgeDetail)
        // Inflate the layout for this fragment

        (dialog as BottomSheetDialog).behavior.apply {
            peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
//            isFitToContents=false
//            expandedOffset=33
            state=BottomSheetBehavior.STATE_EXPANDED
        }
        return binding.root
    }

    override fun onDetach() {
        super.onDetach()
        viewModel.isMainFragment.value=true
    }



    companion object {

        @JvmStatic
        fun newInstance(url:String) =
            ImageDetailFragment(url)
    }

}