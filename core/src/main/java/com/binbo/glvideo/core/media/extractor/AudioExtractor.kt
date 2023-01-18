package com.binbo.glvideo.core.media.extractor

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.binbo.glvideo.core.media.IExtractor
import java.nio.ByteBuffer

class AudioExtractor(context: Context, uri: Uri) : IExtractor {

    private val mMediaExtractor = MMExtractor(context, uri)

    override fun getFormat(): MediaFormat? {
        return mMediaExtractor.getAudioFormat()
    }

    override fun selectSourceTrack() {
        return mMediaExtractor.selectSourceTrack()
    }

    override fun readBuffer(byteBuffer: ByteBuffer): Int {
        return mMediaExtractor.readBuffer(byteBuffer)
    }

    override fun getCurrentTimestamp(): Long {
        return mMediaExtractor.getCurrentTimestamp()
    }

    override fun getSampleFlag(): Int {
        return mMediaExtractor.getSampleFlag()
    }

    override fun seek(pos: Long, mode: Int): Long {
        return mMediaExtractor.seek(pos, mode)
    }

    override fun setStartPos(pos: Long) {
        return mMediaExtractor.setStartPos(pos)
    }

    override fun getStartPos(): Long {
        return mMediaExtractor.getStartPos()
    }

    override fun stop() {
        mMediaExtractor.stop()
    }
}