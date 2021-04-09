package com.simsim.island.glide

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class IslandGlideConfigure :AppGlideModule(){
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val diskCacheSize=1024*1024*20L//20MB
        val internalCacheDiskCacheFactory=InternalCacheDiskCacheFactory(context,"IslandImageCache",diskCacheSize)
        builder.setDiskCache(internalCacheDiskCacheFactory)
    }
}