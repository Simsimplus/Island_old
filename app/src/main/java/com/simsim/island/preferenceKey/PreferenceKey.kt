package com.simsim.island.preferenceKey

import android.content.Context
import android.content.res.Resources
import com.simsim.island.R


class PreferenceKey (val context: Context){

    val cookieInUseKey=getKey(R.string.cookie_in_use_key)
    val cookieFromQRCodeKey=getKey(R.string.cookie_from_QR_code_key)
    val cookieFromWebViewKey=getKey(R.string.cookie_from_web_view_key)
    val enableFabKey=getKey(R.string.enable_fab_key)
    val fabPlaceRightKey=getKey(R.string.fab_place_right_key)
    val fabDefaultSizeKey=getKey(R.string.fab_default_size_key)
    val fabSizeSeekBarKey=getKey(R.string.fab_seek_bar_key)
    val fabSideMarginKey=getKey(R.string.fab_side_distance_key)
    val fabBottomMarginKey=getKey(R.string.fab_bottom_distance_key)
    val fabSwipeFunctionsKey=getKey(R.string.fab_swipe_functions_key)
    val blockRuleManageKey=getKey(R.string.block_rule_manage_key)

    private fun getKey(resId:Int):String{
        val res: Resources =context.resources
        return res.getString(resId)
    }
}

