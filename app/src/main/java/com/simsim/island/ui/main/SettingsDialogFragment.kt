package com.simsim.island.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
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
    private lateinit var fab:FloatingActionButton
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
        fab=binding.fabAdd
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
                fab.setOnClickListener {
                    Log.e(LOG_TAG,"fab clicked")
                }
                fab.setOnTouchListener(OnSwipeListener(
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
                        fab.customSize = FloatingActionButton.NO_CUSTOM_SIZE
                        fab.size = FloatingActionButton.SIZE_AUTO
                    }else{
                        settings[intPreferencesKey("fab_seek_bar_key")]?.let { fabCustomSize->
                            fab.customSize = (fabCustomSize * requireContext().dp2PxScale()).toInt()
                        }
                    }

                }
                (settings[booleanPreferencesKey("fab_place_right_key")]?:true).let { placeRight->
                    val sideMargin=settings[intPreferencesKey("fab_side_distance_key")]?:0
                    val bottomMargin=settings[intPreferencesKey("fab_side_bottom_key")]?:0
                    val layoutParams=fab.layoutParams as CoordinatorLayout.LayoutParams
                    layoutParams.bottomMargin=((30+bottomMargin)*requireContext().dp2PxScale()).toInt()
                    if (placeRight){
                        layoutParams.gravity= Gravity.BOTTOM or Gravity.RIGHT
                        layoutParams.rightMargin=((30+sideMargin)*requireContext().dp2PxScale()).toInt()
                    }else{
                        layoutParams.gravity= Gravity.BOTTOM or Gravity.LEFT
                        layoutParams.leftMargin=((30+sideMargin)*requireContext().dp2PxScale()).toInt()
                    }
                    fab.layoutParams=layoutParams
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