package com.binbo.glvideo.core.media.utils

import android.graphics.SurfaceTexture
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import com.binbo.glvideo.core.media.BaseDecoder
import com.binbo.glvideo.core.media.DefaultDecoderStateListener
import com.binbo.glvideo.core.media.Frame
import com.binbo.glvideo.core.media.decoder.VideoDecoder
import com.binbo.glvideo.core.utils.DefaultPoolExecutor

class VideoDecoderHolder {
    var videoDecoder: CustomVideoDecoder? = null
    var surface: Surface? = null

    fun hasAvailableDecodedFrames() = (videoDecoder?.availableFrames ?: 0) > 0
    fun isDecodeFinished() = videoDecoder?.decodeFinished ?: false
    fun hasDecoderError() = videoDecoder?.hasDecoderError ?: true

    fun consumeFrame() {
        if (hasAvailableDecodedFrames()) {
            videoDecoder?.let {
                it.availableFrames--
            }
        }
    }

    fun startVideoDecoderWithSurfaceTexture(uri: Uri, st: SurfaceTexture, onDecoderFinish: (uri: Uri) -> Unit = {}) {
        surface?.release()
        surface = Surface(st).apply {
            startVideoDecoder(uri, this, onDecoderFinish)
        }
    }

    fun startVideoDecoder(uri: Uri, onDecoderFinish: (uri: Uri) -> Unit) {
        surface?.let {
            startVideoDecoder(uri, it, onDecoderFinish)
        }
    }

    fun startVideoDecoder(uri: Uri, surface: Surface, onDecoderFinish: (uri: Uri) -> Unit = {}) {
        videoDecoder?.stop()
        videoDecoder = CustomVideoDecoder(uri, null, surface).apply {
            setStateListener(object : DefaultDecoderStateListener() {

                override fun onDecoderPrepare(decodeJob: BaseDecoder?) {
                    Log.d("createSharedVideo", "decoderPrepare $decodeJob")
                }

                override suspend fun onPostConsumeDecodedFrame(decodeJob: BaseDecoder?, frame: Frame) {
//                    Log.d("createSharedVideo", "decodeOneFrame $availableFrames")
                    ++availableFrames
                }

                override fun onDecoderFinish(decodeJob: BaseDecoder?) {
                    Log.d("createSharedVideo", "decoderFinish $decodeJob")
                    onDecoderFinish.invoke(uri)
                    decodeFinished = true
                }

                override fun onDecoderDestroy(decodeJob: BaseDecoder?) {
                }

                override fun onDecoderError(decodeJob: BaseDecoder?, msg: String) {
                    hasDecoderError = true
                }
            })
            DefaultPoolExecutor.getInstance().execute(this)
        }
        videoDecoder?.goOn()
    }

    fun release() {
        videoDecoder?.stop()
        videoDecoder = null
        surface?.release()
        surface = null
    }

    class CustomVideoDecoder(val uri: Uri, sfv: SurfaceView?, surface: Surface?) : VideoDecoder(uri, sfv, surface) {
        @Volatile
        var decodeFinished = false

        @Volatile
        var availableFrames = 0

        @Volatile
        var hasDecoderError = false

        init {
//            withoutSync()
        }
    }
}