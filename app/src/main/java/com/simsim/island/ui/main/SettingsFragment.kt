package com.simsim.island.ui.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.view.isVisible
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.simsim.island.MainActivity
import com.simsim.island.R
import com.simsim.island.dataStore
import com.simsim.island.databinding.SettingsDialogFragmentBinding
import com.simsim.island.dp2PxScale
import com.simsim.island.util.LOG_TAG
import com.simsim.island.util.extractCookie
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

@AndroidEntryPoint
class SettingsFragment(val activity: MainActivity,val viewModel: MainViewModel,val binding:SettingsDialogFragmentBinding): PreferenceFragmentCompat() {
//    private val viewModel:MainViewModel=ViewModelProvider(activity).get(MainViewModel::class.java)
    private val customDataStore:CustomDataStore=CustomDataStore()
    private val dataStore: DataStore<Preferences> =activity.dataStore
    private var dp2PxScale:Float=activity.dp2PxScale()

//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        Log.e(LOG_TAG, "attach PreferenceFragmentCompat using dataStore")
//        val sectionName=viewModel.currentSectionName
//        Log.e(LOG_TAG, "SettingsFragment ${viewModel.currentSectionName?:"null"}")
//        customDataStore=CustomDataStore()
//        dataStore=activity.dataStore
//        dp2PxScale=activity.dp2PxScale()
//    }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.apply {
            Log.e(LOG_TAG, "set CustomDataStore to preferenceDataStore")
            preferenceDataStore=customDataStore
        }
        setPreferencesFromResource(R.xml.settings,rootKey)
        val fabSizeSeekBar=findPreference<SeekBarPreference>("fab_seek_bar_key")
            ?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val fabSize=newValue as Int
                binding.fabAdd.customSize=(fabSize*dp2PxScale).toInt()
                true
            }
        }?:throw Exception("can not find fabSizeSeekBar")

        val fabSizeDefault=findPreference<Preference>("fab_default_size_key")
            ?.apply {
            setOnPreferenceClickListener {
                binding.fabAdd.customSize=FloatingActionButton.NO_CUSTOM_SIZE
                binding.fabAdd.size=FloatingActionButton.SIZE_AUTO
                true
            }
        }?:throw Exception("can not find setFabDefaultSize")

        val fabSwitch=findPreference<SwitchPreference>("enable_fab_key")?.apply {
            var isFabEnable:Boolean by Delegates.observable(this.isChecked){_,_,newValue->
                binding.settingToolbar.menu.findItem(R.id.setting_menu_add).isVisible=!newValue
                fabSizeSeekBar.isVisible=newValue
                fabSizeDefault.isVisible=newValue
                binding.fabAdd.isVisible=newValue
            }
            isFabEnable=isChecked
            setOnPreferenceChangeListener { _, newValue->
                isFabEnable=newValue as Boolean
//                binding.settingToolbar.menu.findItem(R.id.setting_menu_add).isVisible=!isFabEnable
//                fabSizeSeekBar.isVisible=isFabEnable
//                fabSizeDefault.isVisible=isFabEnable
//                binding.fabAdd.isVisible=isFabEnable
                true
            }
        }?:throw Exception("can not find fabSwitch")

    val cookieInUse=findPreference<Preference>("cookie_in_use_key")?:throw Exception("can not find cookieInUse")
        val cookieQR=findPreference<Preference>("cookie_from_QR_code_key")?.apply {
            setOnPreferenceClickListener {
                activity.scanQRCode.launch(Unit)
                viewModel.QRcodeResult.observe(viewLifecycleOwner){
                    preferenceDataStore!!.putString("cookie_from_QR_code_key",it)
                    cookieInUse.summary=it
                }
                true
            }
        }?:throw Exception("can not find cookieQR")

        val cookieWebView=findPreference<Preference>("cookie_from_web_view_key")?.apply {
            setOnPreferenceClickListener {
                true
            }
        }?:throw Exception("can not find cookieWebView")

    }
    inner class CustomDataStore:androidx.preference.PreferenceDataStore(){
        init {
            Log.e(LOG_TAG, "initialize CustomDataStore class")
        }
        override fun putString(key: String, value: String?) {
                value?.let {
                    val stringKey= stringPreferencesKey(key)
                    lifecycleScope.launch {
                        dataStore.edit { settings->
                            settings[stringKey]=value
                            Log.e(LOG_TAG,"save string to dataStore with key:$key:$value")
                        }
                    }
                }
        }

        override fun getString(key: String, defValue: String?): String? {
            return runBlocking {
                val stringKey= stringPreferencesKey(key)
                val dataStoreResult= withContext(Dispatchers.IO){
                    dataStore.data.map {settings ->
                        settings[stringKey]
                    }.first()
                }
                Log.e(LOG_TAG,"get string from dataStore with key:$key:$dataStoreResult")
                dataStoreResult
            }
        }

        override fun putInt(key: String, value: Int) {
            val intKey= intPreferencesKey(key)
            lifecycleScope.launch {
                dataStore.edit { settings->
                    settings[intKey]=value
                    Log.e(LOG_TAG,"save string to dataStore with key:$key:$value")
                }
            }
        }

        override fun getInt(key: String, defValue: Int): Int {
            return runBlocking {
                val intKey= intPreferencesKey(key)
                val dataStoreResult= withContext(Dispatchers.IO){
                    dataStore.data.map {settings ->
                        settings[intKey]?:defValue
                    }.first()
                }
                Log.e(LOG_TAG,"get int from dataStore with key:$key:$dataStoreResult")
                dataStoreResult
            }
        }

        override fun putBoolean(key: String, value: Boolean) {
            val booleanKey= booleanPreferencesKey(key)
            lifecycleScope.launch {
                dataStore.edit { settings->
                    settings[booleanKey]=value
                    Log.e(LOG_TAG,"save boolean to dataStore with key:$key:$value")
                }
            }
        }

        override fun getBoolean(key: String, defValue: Boolean): Boolean {
            return runBlocking {
                val booleanKey= booleanPreferencesKey(key)
                val dataStoreResult= withContext(Dispatchers.IO){
                    dataStore.data.map {settings ->
                        settings[booleanKey]?:defValue
                    }.first()
                }
                Log.e(LOG_TAG,"get boolean from dataStore with key:$key:$dataStoreResult")
                dataStoreResult
            }
        }
    }
}

//        lifecycleScope.launch {
//            dataStore.data.collectLatest {settings->
//                settings[booleanPreferencesKey("enable_fab_key")]?.let { isFabEnable->
//                    binding.settingToolbar.menu.findItem(R.id.setting_menu_add).isVisible=!isFabEnable
//                    fabSizeSeekBar.isVisible=isFabEnable
//                    fabSizeDropDown.isVisible=isFabEnable
//                    binding.fabAdd.isVisible=isFabEnable
//
//                }
//                settings[intPreferencesKey("fab_seek_bar_key")]?.let {fabSize->
//                    binding.fabAdd.customSize=(fabSize*dp2PxScale).toInt()
//                }
//                settings[stringPreferencesKey("fab_drop_down_key")]?.let { fabSize->
//
//                }
//
//            }
//        }