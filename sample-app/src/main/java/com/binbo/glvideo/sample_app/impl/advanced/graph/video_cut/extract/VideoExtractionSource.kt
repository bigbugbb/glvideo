package com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.extract

import android.net.Uri
import com.binbo.glvideo.core.graph.component.VideoSource
import com.binbo.glvideo.core.media.Frame
import com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.extract.VideoExtractionConfig.extractHeight
import com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.extract.VideoExtractionConfig.extractWidth
import com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.extract.VideoExtractionConfig.maxCachedFrames

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/13
 * @time 20:03
 */
open class VideoExtractionSource(videoUri: Uri) : VideoSource(videoUri) {

    override val videoDrawerMode: Int
        get() = 1

    override val textureWidth: Int
        get() = extractWidth

    override val textureHeight: Int
        get() = extractHeight

    override val viewportWidth: Int
        get() = extractWidth

    override val viewportHeight: Int
        get() = extractHeight

    override val timeIntervalUsPerFrame: Long
        get() = 1000L / 3 * 1000

    override val textureCount: Int
        get() = maxCachedFrames + frameWindowSize

    override val stopAfterWindowFilled: Boolean
        get() = true

    override val frameWindowSize: Int
        get() = maxCachedFrames / 5

    override fun getFramePresentationTimeUs(frame: Frame, frameIndex: Int): Long {
        return frame.bufferInfo.presentationTimeUs
    }
}