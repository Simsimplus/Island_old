package com.simsim.island.ui.main

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simsim.island.MainActivity
//import com.github.dhaval2404.imagepicker.ImagePicker
import com.simsim.island.R
import com.simsim.island.databinding.NewDraftFragmentBinding
import com.simsim.island.model.Emoji
import com.simsim.island.util.LOG_TAG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NewDraftFragment : DialogFragment() {
    private val viewModel:MainViewModel by activityViewModels()
    private lateinit var binding:NewDraftFragmentBinding
    private val args:NewDraftFragmentArgs by navArgs()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.fullscreenDialog)
    }
    private lateinit var emojiList:List<String>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= NewDraftFragmentBinding.inflate(inflater, container, false)
        lifecycleScope.launch {
            viewModel.database.emojiDao().getAllEmojis().map { emojis->
                emojis.map { emoji ->
                    emoji.emoji
                }
            }.collectLatest {
                emojiList=it
            }
        }
        return binding.root
    }

//    private fun showEmojiBottomSheet() {
//        val gridView=binding.emojiGridView
//        val gridList:List<String> = if (this::emojiList.isInitialized) emojiList else mutableListOf()
//        gridView.adapter=ArrayAdapter(requireContext(),R.layout.emoji_viewholder,gridList)
//        gridView.numColumns=3
//        gridView.setOnItemClickListener { parent, view, position, id ->
//            view as TextView
//            val emojiString=view.text
//            val input=binding.newInputContent
//            val start= input.selectionStart.coerceAtLeast(0)
//            val end= input.selectionEnd.coerceAtLeast(0)
//            Log.e(LOG_TAG,"start:$start,end:$end")
//            Log.e(LOG_TAG,"inputText:${input.text}")
//            input.text?.let { inputText->
//                inputText.replace(start,end,emojiString)
//            }
//        }
//    }

    override fun onDetach() {
        super.onDetach()
        when(args.target){
            "thread"->{viewModel.isMainFragment.value=false}
            "section"->{viewModel.isMainFragment.value=true}
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.pictureUri.removeObservers(viewLifecycleOwner)
        viewModel.cameraTakePictureSuccess.removeObservers(viewLifecycleOwner)
        super.onDismiss(dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar = binding.newDraftDialogToolbar
        toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24)
        toolbar.title = "No." + args.keyWord
        toolbar.setNavigationOnClickListener {
            dismiss()
        }
        toolbar.inflateMenu(R.menu.new_draft_tollbar_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.draft_menu_send -> {

                    Log.e(LOG_TAG,"draft_menu_send")
                    true
                }
                R.id.draft_menu_image_pick -> {
                    Log.e(LOG_TAG,"draft_menu_image_pick")
                    MaterialAlertDialogBuilder(requireContext()).setTitle("选择").setNegativeButton("取消"){ dialogInterface: DialogInterface, buttonId: Int ->
                            dialogInterface.dismiss()
                    }.setItems(arrayOf("拍照","相册")){ dialogInterface: DialogInterface, itemIndex: Int ->
                        dialogInterface.dismiss()
                        when(itemIndex){
                            0-> takePictureFromCamera()
                            1-> takePictureFromGallery()
                        }
                    }.show()
//                    ImagePicker.with(this).crop().compress(2048).start()
                    true
                }
                R.id.draft_emoji_pick -> {
                    Log.e(LOG_TAG,"draft_emoji_pick")
                    showEmojiDialog()
                    true
                }

                else -> {
                    false
                }
            }
        }
    }

    private fun showEmojiDialog() {
        val gridView=GridView(requireContext())
        val gridList:List<String> = if (this::emojiList.isInitialized) emojiList else mutableListOf()
        gridView.adapter=ArrayAdapter(requireContext(),R.layout.emoji_viewholder,gridList)
        gridView.numColumns=3
        gridView.setOnItemClickListener { parent, view, position, id ->
            view as TextView
            val emojiString=view.text
            val input=binding.newInputContent
            val start= input.selectionStart.coerceAtLeast(0)
            val end= input.selectionEnd.coerceAtLeast(0)
            Log.e(LOG_TAG,"start:$start,end:$end")
            Log.e(LOG_TAG,"inputText:${input.text}")
            input.text?.let { inputText->
                inputText.replace(start,end,emojiString)
            }
        }
        MaterialAlertDialogBuilder(requireContext()).setView(gridView).show()

    }

    private fun takePictureFromGallery() {
        viewModel.shouldTakePicture.value="gallery"
        val activity=requireActivity() as MainActivity
        activity.pickPicture.launch("image/*")
        viewModel.pictureUri.observe(viewLifecycleOwner){photoUri->
            viewModel.gallertTakePictureSuccess.value=true
            Glide.with(this).load(photoUri).into(binding.newImagePosted)
            binding.postViewLauout.isVisible = true
            binding.cancleButton.setOnClickListener {
                binding.postViewLauout.visibility = View.GONE
                Glide.with(this).clear(binding.newImagePosted)
            }
            binding.newImagePosted.setOnClickListener {
                val action=NewDraftFragmentDirections.actionGlobalImageDetailFragment(imageUrl = photoUri.toString(),isURI = true)
                findNavController().navigate(action)
            }
        }
    }

    private fun takePictureFromCamera() {
        viewModel.shouldTakePicture.value="camera"
        val activity=requireActivity() as MainActivity
        val photoUri=activity.createImageFile()
        activity.takePicture.launch(photoUri)
        viewModel.cameraTakePictureSuccess.observe(viewLifecycleOwner){ success ->
            if (success){
                viewModel.pictureUri.value=photoUri
                Glide.with(this).load(photoUri).into(binding.newImagePosted)
                binding.postViewLauout.isVisible = true
                binding.cancleButton.setOnClickListener {
                    binding.postViewLauout.visibility = View.GONE
                    Glide.with(this).clear(binding.newImagePosted)
                }
                binding.newImagePosted.setOnClickListener {
                    val action=NewDraftFragmentDirections.actionGlobalImageDetailFragment(imageUrl = photoUri.toString(),isURI = true)
                    findNavController().navigate(action)
                }
            }

        }
    }


    companion object {

        @JvmStatic
        fun newInstance() =
            NewDraftFragment()
    }
}