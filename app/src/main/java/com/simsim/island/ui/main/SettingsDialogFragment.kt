package com.simsim.island.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.simsim.island.MainActivity
import com.simsim.island.R
import com.simsim.island.dataStore
import com.simsim.island.databinding.SettingsDialogFragmentBinding
import com.simsim.island.dp2PxScale
import com.simsim.island.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class SettingsDialogFragment :DialogFragment(){
    private val viewModel:MainViewModel by activityViewModels()
    private lateinit var binding:SettingsDialogFragmentBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.fullscreenDialog)
        Log.e(LOG_TAG, "SettingsDialogFragment ${viewModel.currentSectionName?:"null"}")

    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= SettingsDialogFragmentBinding.inflate(inflater)
        childFragmentManager.commit {
            add(R.id.setting_fragment_container,SettingsFragment(requireActivity() as MainActivity,viewModel,binding))
            addToBackStack("setting")
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupFAB()
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun setupFAB() {
        lifecycleScope.launch {
            requireContext().dataStore.data.collectLatest { settings->
                val swipeUp=settings[stringPreferencesKey(SWIPE_UP)]
                val swipeDown=settings[stringPreferencesKey(SWIPE_DOWN)]
                val swipeLeft=settings[stringPreferencesKey(SWIPE_LEFT)]
                val swipeRight=settings[stringPreferencesKey(SWIPE_RIGHT)]
                binding.fabAdd.setOnTouchListener(OnSwipeListener(
                    requireContext(),
                    onSwipeTop = {
                        getFABSwipeFunction(swipeString =swipeUp ).invoke()
                    },
                    onSwipeBottom = {
                        getFABSwipeFunction(swipeString =swipeDown ).invoke()
                    },
                    onSwipeLeft = {
                        getFABSwipeFunction(swipeString =swipeLeft ).invoke()
                    },
                    onSwipeRight = {
                        getFABSwipeFunction(swipeString =swipeRight ).invoke()
                    }
                ))

                settings[booleanPreferencesKey("fab_default_size_key")]?.let { setSizeDefault->
                    if (setSizeDefault){
                        binding.fabAdd.customSize = FloatingActionButton.NO_CUSTOM_SIZE
                        binding.fabAdd.size = FloatingActionButton.SIZE_AUTO
                    }else{
                        settings[intPreferencesKey("fab_seek_bar_key")]?.let { fabCustomSize->
                            binding.fabAdd.customSize = (fabCustomSize * requireContext().dp2PxScale()).toInt()
                        }
                    }

                }
            }

        }

    }
    private fun getFABSwipeFunction(swipeString:String?):()->Unit{
        val array=requireActivity().resources.getStringArray(R.array.swipe_function)
        return when(swipeString){
            array[1]->{
                {
                    Toast.makeText(requireContext(),"功能：$swipeString",Toast.LENGTH_SHORT).show()
                }
            }
            array[2]->{
                {
                    Toast.makeText(requireContext(),"功能：$swipeString",Toast.LENGTH_SHORT).show()
                }
            }
            array[3]->{
                {
                    Toast.makeText(requireContext(),"功能：$swipeString",Toast.LENGTH_SHORT).show()
                }
            }
            else->{{
                Toast.makeText(requireContext(),"功能：无",Toast.LENGTH_SHORT).show()
            }}
        }
    }
    private fun setupToolbar() {
        val toolbar = binding.settingToolbar
        toolbar.inflateMenu(R.menu.setting_fragment_menu)
        toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24)
        toolbar.setNavigationOnClickListener {
            dismiss()
        }
        toolbar.title = "设置"}
}