package com.simsim.island.ui.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.DialogPreference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import com.simsim.island.MainActivity
import com.simsim.island.R
import com.simsim.island.dataStore
import com.simsim.island.util.LOG_TAG
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class SettingsFragment(): PreferenceFragmentCompat() {
    private lateinit var activity: MainActivity
    private lateinit var customDataStore:CustomDataStore
    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(LOG_TAG, "attach PreferenceFragmentCompat using dataStore")
        activity=context as MainActivity
        customDataStore=CustomDataStore(activity)

    }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings,rootKey)
        preferenceManager.apply {
            Log.d(LOG_TAG, "set CustomDataStore to preferenceDataStore")
//            preferenceDataStore=customDataStore
        }
    }
    inner class CustomDataStore(val activity: MainActivity) :PreferenceDataStore(){
        init {
            Log.d(LOG_TAG, "initialize CustomDataStore class")
        }
        val dataStore=activity.dataStore
        override fun putString(key: String, value: String?) {
            Log.d(LOG_TAG, "save string to dataStore:${value?:"null"}")
                value?.let {
                    val stringKey= stringPreferencesKey(key)
                    lifecycleScope.launch {
                        dataStore.edit { settings->
                            settings[stringKey]=value
                            Log.d(LOG_TAG,"save string to dataStore:$value")
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
                Log.d(LOG_TAG,"get string from dataStore:$dataStoreResult")
                dataStoreResult
            }
        }
    }
}