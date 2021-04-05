package com.simsim.island

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.simsim.island.databinding.MainActivityBinding
import com.simsim.island.ui.main.ImageDetailFragment
import com.simsim.island.ui.main.MainFragment
import com.simsim.island.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding:MainActivityBinding
    private val viewModel:MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getWindowHeightAndActionBarHeight()
        lifecycleScope.launch {
                Log.e("Simsim", "network connected: ${checkNetwork()}")
        }

    }

    override fun onDestroy() {
        super.onDestroy()

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