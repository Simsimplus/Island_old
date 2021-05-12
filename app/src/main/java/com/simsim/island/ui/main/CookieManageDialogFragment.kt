package com.simsim.island.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.simsim.island.R
import com.simsim.island.adapter.CookieRecyclerViewAdapter
import com.simsim.island.databinding.CookieManageDialogFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CookieManageDialogFragment : DialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: CookieManageDialogFragmentBinding
    private lateinit var adapter: CookieRecyclerViewAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.fullscreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = CookieManageDialogFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        lifecycleScope.launch {
            val cookieList = viewModel.getAllCookies()
            adapter = CookieRecyclerViewAdapter(
                requireContext(),
                cookieList.toMutableList(),
                {cookies->
                   lifecycleScope.launch {
                       viewModel.updateCookies(cookies)
                   }
                }
            ) { cookie ->
                lifecycleScope.launch {
                    viewModel.deleteCookie(cookie.cookie)
                }
            }
            binding.cookieRuleRecyclerView.adapter=adapter
            binding.cookieRuleRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.cookieRuleRecyclerView.isMotionEventSplittingEnabled=false
            viewModel.isAnyCookieAvailable().collectLatest { cookieAvailable->
                if (!cookieAvailable){
                    binding.noCookiesTextView.visibility=View.VISIBLE
                    binding.cookieRuleRecyclerView.visibility=View.GONE
                }else{
                    binding.noCookiesTextView.visibility=View.GONE
                    binding.cookieRuleRecyclerView.visibility=View.VISIBLE
                }
            }
        }
    }

    private fun setupToolbar() {
        val toolbar = binding.cookieToolbar
//        toolbar.inflateMenu(R.menu.setting_fragment_menu)
        toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24)
        toolbar.setNavigationOnClickListener {
            dismiss()
        }
        toolbar.title = "饼干页"
    }
}