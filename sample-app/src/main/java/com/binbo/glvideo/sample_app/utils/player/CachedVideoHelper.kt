package com.binbo.glvideo.sample_app.utils.player;

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.util.Util.getUserAgent
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean


class CachedVideoHelper : CacheDataSource.EventListener, TransferListener {

    private val TAG by lazy { this::class.java.canonicalName }

    private var totalBytesToRead = 0L
    private var bytesReadFromCache: Long = 0
    private var bytesReadFromNetwork: Long = 0

    private val cancel: AtomicBoolean = AtomicBoolean(false)

    @WorkerThread
    fun getCachedVideo(context: Context, url: String, tempFile: File, cache: Cache? = ExoMediaPlayerCacheConfig.simpleCache): Boolean {
        var isSuccessful = false
        val upstreamDataSource = DefaultHttpDataSourceFactory(getUserAgent(context, context.packageName)).createDataSource()
        val dataSource = CacheDataSource(
            cache!!,
            // If the cache doesn't have the whole content, the missing data will be read from upstream
            upstreamDataSource,
            FileDataSource(),
            // Set this to null if you don't want the downloaded data from upstream to be written to cache
            CacheDataSink(cache!!, CacheDataSink.DEFAULT_BUFFER_SIZE.toLong()),
            /* flags= */ 0,
            /* eventListener= */ this
        )

        // Listen to the progress of the reads from cache and the network.
        dataSource.addTransferListener(this)

        var outFile: FileOutputStream? = null
        var bytesRead = 0

        // Total bytes read is the sum of these two variables.
        totalBytesToRead = C.LENGTH_UNSET.toLong()
        bytesReadFromCache = 0
        bytesReadFromNetwork = 0

        try {
            outFile = FileOutputStream(tempFile)
            totalBytesToRead = dataSource.open(DataSpec(Uri.parse(url)))
            // Just read from the data source and write to the file.
            val data = ByteArray(16384)

            Log.d(TAG, "<<<<Starting fetch...")
            while (bytesRead != C.RESULT_END_OF_INPUT) {
                if (cancel.get()) {
                    throw IOException("video cached data fetching process has been canceled")
                }

                bytesRead = dataSource.read(data, 0, data.size)
                if (bytesRead != C.RESULT_END_OF_INPUT) {
                    outFile.write(data, 0, bytesRead)
                }
            }
            isSuccessful = true
        } catch (e: IOException) {
            // error processing
        } finally {
            dataSource.close()
            outFile?.flush()
            outFile?.close()
        }

        return isSuccessful
    }

    fun cancel() {
        cancel.set(true)
    }

    override fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long) {
        Log.d(TAG, "<<<<Cache read? Yes, (byte read) $cachedBytesRead (cache size) $cacheSizeBytes")
    }

    override fun onCacheIgnored(reason: Int) {
        Log.d(TAG, "<<<<Cache ignored. Reason = $reason")
    }

    private fun reportProgress(bytesTransferred: Int, isNetwork: Boolean) {
        val percentComplete = 100 * (bytesReadFromNetwork + bytesReadFromCache).toFloat() / totalBytesToRead
        val completed = "%.1f".format(percentComplete)
        Log.d(TAG, "<<<<Bytes transferred: $bytesTransferred isNetwork=$isNetwork $completed% completed")
    }

    override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
        Log.d(TAG, "<<<<Initializing isNetwork=$isNetwork")
    }

    override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
        Log.d(TAG, "<<<<Transfer is starting isNetwork=$isNetwork")
    }

    override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
        // Report progress here.
        if (isNetwork) {
            bytesReadFromNetwork += bytesTransferred
        } else {
            bytesReadFromCache += bytesTransferred
        }

        reportProgress(bytesTransferred, isNetwork)
    }

    override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
        reportProgress(0, isNetwork)
        Log.d(TAG, "<<<<Transfer has ended isNetwork=$isNetwork")
    }
}