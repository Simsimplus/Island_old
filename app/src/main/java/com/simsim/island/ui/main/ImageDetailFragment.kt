package com.simsim.island.ui.main

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.simsim.island.MainActivity
import com.simsim.island.R
import com.simsim.island.databinding.ImageDetailFragmentBinding
import com.simsim.island.util.LOG_TAG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.nio.ByteBuffer

@AndroidEntryPoint
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
        val imageUrl=if (args.isURI) Uri.parse(args.imageUrl) else args.imageUrl
        Glide.with(binding.root).load(imageUrl).addListener(
            imageRequestListener
            ).placeholder(circularProgress).into(binding.imageDetail)



        return binding.root
    }
    override fun onDetach() {
        super.onDetach()
        viewModel.isMainFragment.value=true//todo
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
                    Log.e("Simsim", "image_share_menu_item taped")
                    shareImage()
                    true
                }
                R.id.image_save_menu_item->{
                    saveImageToPictureFolder()

                    true
                }
//            R.id.menu_item_search->{}
                else -> false
            }
        }
    }

    private fun shareImage() {
        CoroutineScope(Dispatchers.IO).launch {
            val imageUrl = if (args.isURI) Uri.parse(args.imageUrl) else args.imageUrl
            val imageFile = Glide.with(binding.root).asFile().load(imageUrl).submit().get()
            val imageUri =
                FileProvider.getUriForFile(requireContext(), "com.simsim.fileProvider", imageFile)
            Log.e(LOG_TAG, "image share uri:$imageUri")

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
            }
            requireActivity().startActivity(Intent.createChooser(intent, "分享图片"))
        }
    }

    private fun saveImageToPictureFolder() {
        CoroutineScope(Dispatchers.IO).launch {
            val imageUrl = args.imageUrl
            when {
                imageUrl.endsWith("gif") -> {
                    val photoStream = (requireActivity() as MainActivity).saveImage(type = "gif")
                    val image = Glide.with(this@ImageDetailFragment).asGif().load(imageUrl).submit()
                        .get() as GifDrawable
                    val byteBuffer = image.buffer
                    val bytes = ByteArray(byteBuffer.capacity())
                    (byteBuffer.duplicate().clear() as ByteBuffer).get(bytes)
                    photoStream.write(bytes, 0, bytes.size)
                    photoStream.close()
                }
                imageUrl.endsWith("jpg") -> {
                    val photoStream = (requireActivity() as MainActivity).saveImage(type = "jpg")
                    val image =
                        Glide.with(this@ImageDetailFragment).asBitmap().load(imageUrl).submit()
                            .get()
                    image.compress(Bitmap.CompressFormat.JPEG, 100, photoStream)
                }
                imageUrl.endsWith("png") -> {
                    val photoStream = (requireActivity() as MainActivity).saveImage(type = "png")
                    val image =
                        Glide.with(this@ImageDetailFragment).asBitmap().load(imageUrl).submit()
                            .get()
                    image.compress(Bitmap.CompressFormat.PNG, 100, photoStream)
                }
            }
            Log.e(LOG_TAG, "save images")
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            ImageDetailFragment()
    }

}