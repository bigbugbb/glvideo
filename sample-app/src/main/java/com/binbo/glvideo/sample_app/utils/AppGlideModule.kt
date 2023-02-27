package com.binbo.glvideo.sample_app.utils

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper
import com.bumptech.glide.load.engine.cache.ExternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import java.io.File

@GlideModule
class AppGlideModule : com.bumptech.glide.module.AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
//        builder.setLogLevel(Log.VERBOSE)
        // 磁盘缓存
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, 500 * 1024 * 1024)) // 内部磁盘缓存
        builder.setDiskCache(ExternalCacheDiskCacheFactory(context, 500 * 1024 * 1024)) // 磁盘缓存到外部存储
        // 指定缓存目录
        builder.setDiskCache {
            val cacheLocation = File(context.externalCacheDir, "PgGildeCache")
            cacheLocation.mkdirs()
            DiskLruCacheWrapper.get(cacheLocation, 1024 * 1024 * 500.toLong())
        }
        super.applyOptions(context, builder)
    }
}
