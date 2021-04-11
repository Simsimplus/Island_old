package com.simsim.island

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.simsim.island.database.IslandDatabase
import com.simsim.island.databinding.MainActivityBinding
import com.simsim.island.model.Emoji
import com.simsim.island.ui.main.MainViewModel
import com.simsim.island.util.LOG_TAG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding:MainActivityBinding
    private val viewModel:MainViewModel by viewModels()
    @Inject lateinit var database:IslandDatabase

    internal val requestPermission=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions->
        permissions.entries.forEach{ entry->
            val permissionName=entry.key
            val isGranted=entry.value
            if (!isGranted){
                Log.e(LOG_TAG,"permission[$permissionName] is denied")
                //todo
            }
        }

    }
    internal val takePicture=registerForActivityResult(ActivityResultContracts.TakePicture()){ success->
        if (success){
            Log.e(LOG_TAG,"camera take photo:success")
            viewModel.cameraTakePictureSuccess.value=true
        }

    }
    internal val pickPicture=registerForActivityResult(ActivityResultContracts.GetContent()){ uri->
        viewModel.pictureUri.value=uri
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getWindowHeightAndActionBarHeight()
        lifecycleScope.launch {
            val emoji="|∀ﾟ, (´ﾟДﾟ`), (;´Д`), (｀･ω･), (=ﾟωﾟ)=, | ω・´), |-` ), |д` ), |ー` ), |∀` ), (つд⊂), (ﾟДﾟ≡ﾟДﾟ), (＾o＾)ﾉ, (|||ﾟДﾟ), ( ﾟ∀ﾟ), ( ´∀`), (*´∀`), (*ﾟ∇ﾟ), (*ﾟーﾟ), (　ﾟ 3ﾟ), ( ´ー`), ( ・_ゝ・), ( ´_ゝ`), (*´д`), (・ー・), (・∀・), (ゝ∀･), (〃∀〃), (*ﾟ∀ﾟ*), ( ﾟ∀。), ( `д´), (`ε´ ), (`ヮ´ ), σ`∀´),  ﾟ∀ﾟ)σ, ﾟ ∀ﾟ)ノ, (╬ﾟдﾟ), (|||ﾟдﾟ), ( ﾟдﾟ), Σ( ﾟдﾟ), ( ;ﾟдﾟ), ( ;´д`), (　д ) ﾟ ﾟ, ( ☉д⊙), (((　ﾟдﾟ))), ( ` ・´), ( ´д`), ( -д-), (>д<), ･ﾟ( ﾉд`ﾟ), ( TдT), (￣∇￣), (￣3￣), (￣ｰ￣), (￣ . ￣), (￣皿￣), (￣艸￣), (￣︿￣), (￣︶￣), ヾ(´ωﾟ｀), (*´ω`*), (・ω・), ( ´・ω), (｀・ω), (´・ω・`), (`・ω・´), ( `_っ´), ( `ー´), ( ´_っ`), ( ´ρ`), ( ﾟωﾟ), (oﾟωﾟo), (　^ω^), (｡◕∀◕｡), /( ◕‿‿◕ )\\, ヾ(´ε`ヾ), (ノﾟ∀ﾟ)ノ, (σﾟдﾟ)σ, (σﾟ∀ﾟ)σ, |дﾟ ), ┃電柱┃, ﾟ(つд`ﾟ), ﾟÅﾟ )　, ⊂彡☆))д`), ⊂彡☆))д´), ⊂彡☆))∀`), (´∀((☆ミつ\n"
            emoji.split(",").mapIndexed { i,s->
                Emoji(emojiIndex = i,emoji=s)
            }.also { emojiList->
                database.emojiDao().insertAllEmojis(emojiList)
            }
        }


        lifecycleScope.launchWhenCreated {
            Log.e(LOG_TAG,"network:${checkNetwork()}")
            viewModel.doWhenDestroy()
        }

        viewModel.shouldTakePicture.observe(this){
            when(it){
                "gallery"->takePictureFromGallery()
                "camera"->takePictureFromCamera()
            }
        }


    }

    private fun getWindowHeightAndActionBarHeight() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        viewModel.windowHeight = displayMetrics.heightPixels
        val tv=TypedValue()
        theme.resolveAttribute(R.attr.actionBarSize,tv,true)
        viewModel.actionBarHeight=TypedValue.complexToDimensionPixelSize(tv.data,resources.displayMetrics)
    }


    private fun checkNetwork(): Boolean {
        val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connMgr.activeNetworkInfo
        return networkInfo?.isConnected == true
    }

    private fun takePictureFromGallery() {
        Log.e(LOG_TAG,"gallery main activity")
    }

    private fun takePictureFromCamera() {
        Log.e(LOG_TAG,"camera main activity")

    }
    @Throws(IOException::class)
    internal fun createImageFile(): Uri {
        // Create an image file name
        val timeStamp: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val photoFile= File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            viewModel.picturePath.value = absolutePath
        }
        val photoUri= FileProvider.getUriForFile(this,"com.simsim.fileProvider",photoFile)
        Log.e(LOG_TAG,"take picture,and it's uri:$photoUri")
        return photoUri
    }



    companion object{
        private const val REQUEST_IMAGE_CAMERA=1
    }


}