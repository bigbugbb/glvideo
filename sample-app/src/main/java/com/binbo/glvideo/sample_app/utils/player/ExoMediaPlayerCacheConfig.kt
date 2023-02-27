package com.binbo.glvideo.sample_app.utils.player

import com.binbo.glvideo.sample_app.App.Companion.context
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache

object ExoMediaPlayerCacheConfig {
    var simpleCache: SimpleCache? = null
    var leastRecentlyUsedCacheEvictor: LeastRecentlyUsedCacheEvictor? = null
    var exoDatabaseProvider: ExoDatabaseProvider? = null
    var exoPlayerCacheSize: Long = 512 * 1024 * 1024

    fun init() {
        if (leastRecentlyUsedCacheEvictor == null) {
            leastRecentlyUsedCacheEvictor = LeastRecentlyUsedCacheEvictor(exoPlayerCacheSize)
        }

        if (exoDatabaseProvider == null) {
            exoDatabaseProvider = ExoDatabaseProvider(context)
        }

        if (simpleCache == null) {
            simpleCache = SimpleCache(context.cacheDir, leastRecentlyUsedCacheEvictor!!, exoDatabaseProvider!!)
        }
    }
}