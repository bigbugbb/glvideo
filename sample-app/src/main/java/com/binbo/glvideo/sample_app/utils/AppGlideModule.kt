package com.binbo.glvideo.sample_app.utils

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper
import com.bumptech.glide.load.engine.cache.ExternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.ExternalPreferredCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import java.io.File

@GlideModule
class AppGlideModule : com.bumptech.glide.module.AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
//        builder.setLogLevel(Log.VERBOSE)
        // disk cache
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, 500L * 1024 * 1024))
        builder.setDiskCache(ExternalPreferredCacheDiskCacheFactory(context, 500L * 1024 * 1024))
        // disk cache folder
        builder.setDiskCache {
            val cacheLocation = File(context.externalCacheDir, "glvideo_glide_cache")
            cacheLocation.mkdirs()
            DiskLruCacheWrapper.create(cacheLocation, 500L * 1024 * 1024)
        }
        super.applyOptions(context, builder)
    }
}
