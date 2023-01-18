package com.binbo.glvideo.core.media.decoder

import android.media.MediaCodec
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.binbo.glvideo.core.GLVideo.Core.context
import com.binbo.glvideo.core.media.BaseDecoder
import com.binbo.glvideo.core.media.IExtractor
import com.binbo.glvideo.core.media.extractor.VideoExtractor
import java.nio.ByteBuffer

open class VideoDecoder(uri: Uri, var surfaceView: SurfaceView?, var surface: Surface?) : BaseDecoder(uri) {
    private val TAG = "VideoDecoder"

    override fun check(): Boolean {
        if (surfaceView == null && surface == null) {
            Log.w(TAG, "SurfaceView和Surface都为空，至少需要一个不为空")
            mStateListener?.onDecoderError(this, "显示器为空")
            return false
        }
        return true
    }

    override fun initExtractor(uri: Uri): IExtractor {
        return VideoExtractor(context, uri)
    }

    override fun initSpecParams(format: MediaFormat) {
        mExtractor?.setStartPos(mStartPos)
    }

    override fun configCodec(codec: MediaCodec, format: MediaFormat, notifyDecode: Boolean): Boolean {
        if (surface != null) {
            codec.configure(format, surface, null, 0)
            if (notifyDecode) {
                notifyDecode()
            }
        } else if (surfaceView?.holder?.surface != null) {
            surface = surfaceView?.holder?.surface
            configCodec(codec, format, notifyDecode)
        } else {
            surfaceView?.holder?.addCallback(object : SurfaceHolder.Callback2 {
                override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                }

                override fun surfaceCreated(holder: SurfaceHolder) {
                    surface = holder.surface
                    configCodec(codec, format, notifyDecode)
                }
            })

            return false
        }
        return true
    }

    override fun initRender(): Boolean {
        return true
    }

    override fun render(outputBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
    }

    override fun seekAndPlay(pos: Long, mode: Int): Long {
        if (mIsEOS) {
            reset()
        }
        val p = mExtractor?.seek(pos, mode)
        mCodec?.flush()
        mIsEOS = false
        mBufferInfo = MediaCodec.BufferInfo()
        return 0
    }

    override fun doneDecode() {
    }
}