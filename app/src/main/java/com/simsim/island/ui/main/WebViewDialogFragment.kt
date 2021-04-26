package com.simsim.island.ui.main

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.simsim.island.R
import com.simsim.island.databinding.WebViewDialogFragmentBinding
import com.simsim.island.util.LOG_TAG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class WebViewDialogFragment :DialogFragment(){
    private val viewModel:MainViewModel by activityViewModels()
    private val args by navArgs<WebViewDialogFragmentArgs>()
    private lateinit var binding: WebViewDialogFragmentBinding
    private lateinit var webView:WebView
    private lateinit var cookieManager: CookieManager
    private var cookieMap:Map<String,String> = mapOf()
    private var snackBar:Snackbar?=null
    var shouldDismiss=true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.fullscreenDialog)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        cookieManager=CookieManager.getInstance()
        binding= WebViewDialogFragmentBinding.inflate(inflater)
        webView=binding.webView
        webView.loadUrl(args.url)
        webView.webViewClient= object :WebViewClient(){
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (Uri.parse(url).host == "adnmb3.com"){
                    url?.let {
                        webView.loadUrl(url)
                    }
                    return false
                }
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    startActivity(this)
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val cookie=cookieManager.getCookie("adnmb3.com")
                cookie?.let {
                    cookieMap=cookieManager.getCookie("adnmb3.com").split("; ").map {
                        val list=it.split("=")
                        list[0] to list[1]
                    }.toMap()
                    if (cookieMap.containsKey("memberUserspapapa") && cookieMap.containsKey("PHPSESSID")){
                        CoroutineScope(Dispatchers.IO).launch{
                            cookieManager.flush()
                        }
                        Log.e(LOG_TAG, "got login cookie:$cookieMap")
                        lifecycleScope.launch{
                            if (snackBar==null) {
                                snackBar=Snackbar
                                    .make(binding.webViewCoordinatorLayout, "登录成功，导入饼干中",3000).setAction("重新登录"){
                                        snackBar=null
                                        cookieManager.removeAllCookies {
                                            webView.loadUrl(args.url)
                                        }
                                        shouldDismiss=false
                                    }

                            }else{
                                snackBar!!.show()
                            }
                            delay(3000)
                            if (shouldDismiss){

                                dismiss()
                            }
                        }
                    }
                }
            }
        }
        webView.settings.apply {
//            cacheMode=WebSettings.LOAD_NO_CACHE
            javaScriptEnabled=true
            builtInZoomControls=true
        }
        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.loginCookies.value=cookieMap
        super.onDismiss(dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
    }
    private fun setupToolbar() {
        val toolbar = binding.webViewToolbar
        toolbar.inflateMenu(R.menu.web_view_toolbar_menu)
        toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24)
        toolbar.setNavigationOnClickListener {
            dismiss()
        }
        toolbar.setOnMenuItemClickListener { menuItem->
            when(menuItem.itemId){
                R.id.web_view_menu_relogin->{
                    snackBar=null
                    cookieManager.removeAllCookies {
                        webView.loadUrl(args.url)
                    }
                    true
                }
                else->{
                    false
                }
            }
        }
        toolbar.title = "用户系统"}
}