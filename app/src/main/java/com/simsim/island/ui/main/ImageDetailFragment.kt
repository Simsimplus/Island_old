package com.simsim.island.ui.main

import android.app.Dialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.simsim.island.R
import com.simsim.island.databinding.ImageDetailFragmentBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ImageDetailFragment() : DialogFragment() {
    private val args:ImageDetailFragmentArgs by navArgs()
    internal lateinit var binding: ImageDetailFragmentBinding
    private val viewModel:MainViewModel by activityViewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.fullscreenDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
//        dialog.setOnShowListener {
//            val view=(it as BottomSheetDialog).findViewById<View>(R.id.image_layout)
//            view?.layoutParams?.apply {
//                height=viewModel.windowHeight
//                view.layoutParams=this
//            }
//        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= ImageDetailFragmentBinding.inflate(inflater, container, false)
        val circularProgress=CircularProgressDrawable(requireContext()).apply {
            strokeWidth = 10f
            centerRadius = 30f
        }
            circularProgress.setColorSchemeColors(ContextCompat.getColor(requireContext(),R.color.colorSecondary))
            circularProgress.start()
        val imageRequestListener = object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                circularProgress.stop()
                return false
            }

        }
        Glide.with(binding.root).load(args.imageUrl).addListener(
            imageRequestListener
            ).placeholder(circularProgress).into(binding.imgeDetail)



        return binding.root
    }
    override fun onDetach() {
        super.onDetach()
        viewModel.isMainFragment.value=true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar = binding.imageToolbar
        toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24)
        toolbar.setNavigationOnClickListener {
            dismiss()
        }
        toolbar.title = "图片"
        toolbar.inflateMenu(R.menu.image_toolbar_menu)
        toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.image_share_menu_item->{

                    Log.e("Simsim","image_share_menu_item taped")
                    true
                }
//            R.id.menu_item_search->{}
                else -> false
            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            ImageDetailFragment()
    }

}