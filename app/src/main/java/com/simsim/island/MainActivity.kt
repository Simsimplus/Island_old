package com.simsim.island

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.bumptech.glide.Glide
import com.simsim.island.database.IslandDatabase
import com.simsim.island.databinding.MainActivityBinding
import com.simsim.island.ui.main.MainViewModel
import com.simsim.island.util.LOG_TAG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.prefs.Preferences
import javax.inject.Inject

val Context.sectionDataStore by preferencesDataStore("sections")
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding:MainActivityBinding
    private val viewModel:MainViewModel by viewModels()
    @Inject lateinit var database:IslandDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getWindowHeightAndActionBarHeight()



        lifecycleScope.launchWhenCreated {
            viewModel.doWhenDestroy()
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






}