package com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.crop

import android.net.Uri
import android.util.Range
import com.binbo.glvideo.core.graph.component.VideoSource
import com.binbo.glvideo.core.media.Frame

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/8
 * @time 19:24
 */
class VideoCropSource(videoUri: Uri, timeline: Range<Long>) :
    VideoSource(videoUri, startPos = timeline.lower, clippingEnabled = true, clippingTimeline = timeline) {

    override val textureWidth: Int
        get() {
            var n = 1
            while (Math.min(videoWidth, videoHeight) / n >= 800) {
                n *= 2
            }
            return videoWidth / n
        }

    override val textureHeight: Int
        get() {
            var n = 1
            while (Math.min(videoWidth, videoHeight) / n >= 800) {
                n *= 2
            }
            return videoHeight / n
        }

    override val timeIntervalUsPerFrame: Long
        get() = 1000L / maxFrameRate * 1000L

    override val frameWindowSize: Int
        get() {
            /**
             * 需要限制帧率，因为手机拍的视频可能远超过30fps，而最终要裁剪生成的视频不需要那么高的帧率，且帧率过高会浪费显存。
             * 原则上app里其他graph的video source也应该限制帧率，但由于其他地方的源视频来源于我们自己生成的视频，所以目前没什么问题。
             * 这里的视频可以来自于手机相册，所以必须加限制。同时需要设置timeIntervalUsPerFrame，表示相邻采集帧的pts间隔必须大于这个值，从而控制帧采集的数量。
             */
            var frameRate = videoMetaData.frameRate.coerceAtMost(maxFrameRate)
            return frameRate * 6
        }

    override fun getFramePresentationTimeUs(frame: Frame, frameIndex: Int): Long {
        return frame.bufferInfo.presentationTimeUs
    }

    companion object {
        const val maxFrameRate = 30
    }
}