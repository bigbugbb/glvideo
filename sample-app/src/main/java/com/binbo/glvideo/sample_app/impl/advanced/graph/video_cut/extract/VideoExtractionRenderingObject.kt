package com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.extract

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.util.Range
import androidx.core.math.MathUtils.clamp
import com.binbo.glvideo.core.ext.dip
import com.binbo.glvideo.core.ext.nowSystemClock
import com.binbo.glvideo.core.graph.EndOfStream
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaQueue
import com.binbo.glvideo.core.graph.event.DecodeMoreFrames
import com.binbo.glvideo.core.graph.event.DecodedVideoFrame
import com.binbo.glvideo.core.graph.event.VideoMetaDataRetrieved
import com.binbo.glvideo.core.graph.simple.SimpleMediaObject
import com.binbo.glvideo.core.media.utils.VideoMetaData
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.drawer.TimelineBarDrawer
import com.binbo.glvideo.core.opengl.renderer.DefaultGLRenderer
import com.binbo.glvideo.core.opengl.renderer.RenderImpl
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.core.widget.VideoExtractionSurfaceView
import com.binbo.glvideo.sample_app.event.TimelineUpdatedEvent
import com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.extract.VideoExtractionConfig.maxCachedFrames
import com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.extract.VideoExtractionConfig.maxExtractDuration
import com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.extract.VideoExtractionConfig.minExtractDuration
import com.binbo.glvideo.sample_app.utils.doClickVibrator
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.Long.min
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.abs

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/14
 * @time 11:36
 */
class VideoExtractionRenderingObject(private val surfaceViewRef: WeakReference<VideoExtractionSurfaceView>) : SimpleMediaObject() {

    private var renderer: VideoExtractionRenderer? = null

    @Volatile
    internal var meta: VideoMetaData? = null

    val visibleTimeRange: Range<Long>
        get() = if (renderer == null) {
            Range(0L, 10000000L)
        } else {
            Range(renderer!!.timeRangeLower, renderer!!.timeRangeUpper)
        }

    override suspend fun onPrepare() {
        super.onPrepare()
        renderer = VideoExtractionRenderer(this).apply {
            addDrawer(FrameDrawer())
            addDrawer(TimelineBarDrawer())
            surfaceViewRef.get()?.setRenderer(this)
            updateInitialTimeline()
        }
    }

    override suspend fun onStart() {
        super.onStart()
        renderer?.run {
            setUseCustomRenderThread(true, graph?.eglResource?.sharedContext)
            setSurface(surfaceViewRef.get()!!)
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
            setRenderWaitingTime(1)
        }
    }

    override suspend fun onStop() {
        super.onRelease()
        renderer?.stop()
        renderer = null
    }

    override suspend fun onReceiveEvent(event: BaseGraphEvent<MediaData>) {
        when (event) {
            is VideoMetaDataRetrieved -> meta = event.meta
            is VideoPlayingPosUpdated -> renderer?.onVideoPlayingPosUpdate(event.timestampUs)
        }
    }
}

private class VideoExtractionRenderer(private val renderingObject: VideoExtractionRenderingObject) : DefaultGLRenderer() {

    private val textureQueue: BaseMediaQueue<MediaData>
        get() = renderingObject.inputQueues[0]

    private var leftBarSelected: Boolean = false
    private var rightBarSelected: Boolean = false

    private var suspendFrameLoading: Boolean = false
    private var videoPlayingTimestampUs: Long = -1L
    private var scaleFactor: Float = 1f

    internal val timeRangeLower: Long
        get() = abs(translationX) * intervalUsPerPixel

    internal val timeRangeUpper: Long
        get() = timeRangeLower + contentWidth * intervalUsPerPixel

    private val videoDurationUs: Long
        get() = renderingObject.meta?.totalDurationUs ?: 0

    private val frameWidth: Int
        get() = (height / 16f * 9f + 0.5f).toInt()

    private val frameHeight: Int
        get() = height

    private val contentWidth: Int
        get() = width - contentPaddingHorizontal * 2

    private val contentPaddingHorizontal: Int // 纹理绘制需要对齐到16，不然画面会出现问题
        get() {
            val padding = dip(24) // timelineBarDrawer!!.barWidth.toInt() + dip(12)
            return padding + padding % 16
        }

    private val maxDurationPerPage: Long
        get() = when {
            videoDurationUs > 300000000L -> (15000000L * scaleFactor).toLong()
            videoDurationUs > 600000000L -> (20000000L * scaleFactor).toLong()
            videoDurationUs > 1200000000L -> (30000000L * scaleFactor).toLong()
            else -> (min(videoDurationUs, 10000000L) * scaleFactor).toLong()
        }

    private val maxAllowedTranslationX: Int
        get() = (videoDurationUs / intervalUsPerPixel - contentWidth).toInt().coerceAtLeast(0)

    private var translationX: Int = 0

    private val frameDrawer: FrameDrawer?
        get() = drawers[FrameDrawer::class.java] as? FrameDrawer?

    private val timelineBarDrawer: TimelineBarDrawer?
        get() = drawers[TimelineBarDrawer::class.java] as? TimelineBarDrawer

    private val intervalUsPerPixel: Long
        get() = maxDurationPerPage / contentWidth

    private val playingTimestampUs: Long
        get() = videoPlayingTimestampUs - abs(translationX) * intervalUsPerPixel

    override fun onTouchPress(normalizedX: Float, normalizedY: Float) {
        val x = (normalizedX + 1) / 2 * width - contentPaddingHorizontal

        timelineBarDrawer?.run {
            val leftBarX = startTimestampUs / intervalUsPerPixel
            val rightBarX = stopTimestampUs / intervalUsPerPixel
            // 左右各多出25%的空间便于选择
            leftBarSelected = x >= leftBarX - barWidth * 1.25f && x < leftBarX + barWidth * 0.25f
            rightBarSelected = x <= rightBarX + barWidth * 1.25f && x > rightBarX - barWidth * 0.25f

            if (leftBarSelected || rightBarSelected) {
                doClickVibrator()
            }
        }

        // fling时突然停下，因为没有了后续的onScroll更新，需要重置相关flag并seek到最后位置
        if (suspendFrameLoading) {
            suspendFrameLoading = false
            renderingObject.run {
                graph?.eventCoroutineScope?.launch {
                    timelineBarDrawer?.let {
                        val offset = abs(translationX) * intervalUsPerPixel
                        val timeline = Range(it.startTimestampUs + offset, it.stopTimestampUs + offset)
                        broadcast(TimelineUpdatedEvent(timeline, videoDurationUs, suspendFrameLoading))
                    }
                }
            }
        }

        Log.d(tagOfExtract, "leftBarSelected = $leftBarSelected, rightBarSelected = $rightBarSelected, x = $x")
    }

    override fun onScroll(normalizedDistanceX: Float, normalizedDistanceY: Float) {
        val offsetX = (normalizedDistanceX * width).toInt()
        when {
            leftBarSelected -> timelineBarDrawer?.run {
                var newTimestampUs =
                    clamp(startTimestampUs + offsetX * intervalUsPerPixel, stopTimestampUs - maxExtractDuration, stopTimestampUs - minExtractDuration)
                newTimestampUs = newTimestampUs.coerceAtLeast(0L)
                timelineBarDrawer?.updateStartTime(newTimestampUs)
                Log.d(tagOfExtract, "startTimestampUs = $newTimestampUs, stopTimestampUs = $stopTimestampUs")
            }
            rightBarSelected -> timelineBarDrawer?.run {
                var newTimestampUs =
                    clamp(stopTimestampUs + offsetX * intervalUsPerPixel, startTimestampUs + minExtractDuration, startTimestampUs + maxExtractDuration)
                newTimestampUs = newTimestampUs.coerceAtMost(maxDurationPerPage)
                timelineBarDrawer?.updateStopTime(newTimestampUs)
                Log.d(tagOfExtract, "startTimestampUs = $startTimestampUs, stopTimestampUs = $newTimestampUs")
            }
            else -> {
                translationX += offsetX
                translationX = translationX.coerceAtMost(0).coerceAtLeast(-maxAllowedTranslationX)
                Log.d(tagOfExtract, "scaleFactor = $scaleFactor translationX = $translationX")
            }
        }
        // Log.d(tagOfExtract, "translationX = $translationX")
    }

    override fun onFling(normalizedDistanceX: Float, normalizedDistanceY: Float) {
        val offsetX = (normalizedDistanceX * width).toInt()
        suspendFrameLoading = abs(offsetX) >= width * 0.015
        leftBarSelected = false
        rightBarSelected = false
        Log.d(tagOfExtract, "suspendFrameLoading = $suspendFrameLoading, offset = $offsetX")
        onScroll(normalizedDistanceX, normalizedDistanceY)
    }

    override fun onScale(f: Float) {
//        timelineBarDrawer?.run {
//            val deltaTimestampUs = abs(stopTimestampUs - startTimestampUs) / 2
//            val middleTimestampUs = (startTimestampUs + stopTimestampUs) / 2
//            val startTimeInPixel = startTimestampUs / intervalUsPerPixel
//            val stopTimeInPixel = stopTimestampUs / intervalUsPerPixel
//            val middleTimeInPixel = ((startTimeInPixel + stopTimeInPixel) / 2 + 0.5).toLong()
//            val oldIntervalUsPerPixel = intervalUsPerPixel
//            scaleFactor = clamp(scaleFactor * (1f / f), 0.6f, 3f)
//            var newStartTimestampUs = clamp(middleTimeInPixel * intervalUsPerPixel - deltaTimestampUs, middleTimestampUs - maxExtractDuration / 2, middleTimestampUs - minExtractDuration / 2)
//            newStartTimestampUs = newStartTimestampUs.coerceAtLeast(0L)
//            updateStartTime(newStartTimestampUs)
//            var newStopTimestampUs = clamp(middleTimeInPixel * intervalUsPerPixel + deltaTimestampUs, middleTimestampUs + minExtractDuration / 2, middleTimestampUs + maxExtractDuration / 2)
//            newStopTimestampUs = newStopTimestampUs.coerceAtMost(maxDurationPerPage)
//            updateStopTime(newStopTimestampUs)
//            Log.d(tagOfExtract, "scaleFactor = $scaleFactor xxx=${intervalUsPerPixel / oldIntervalUsPerPixel.toFloat()}")
//        }
    }

    fun onVideoPlayingPosUpdate(timestampUs: Long) {
        if (!suspendFrameLoading) {
            videoPlayingTimestampUs = timestampUs
        }
    }

    fun updateInitialTimeline() {
        val durationPerPage = videoDurationUs.coerceAtMost(maxDurationPerPage)
        val offsetTimestampUs = ((durationPerPage - maxExtractDuration) / 2).coerceAtLeast(0L)
        timelineBarDrawer?.updateStartTime(offsetTimestampUs)
        timelineBarDrawer?.updateStopTime(durationPerPage - offsetTimestampUs)
    }

    override var impl: RenderImpl = object : RenderImpl {

        private val frameBuffers = IntArray(1)
        private val frameBufferTextures = IntArray(1)

        private val frameCapacity = maxCachedFrames

        private val frames = TreeSet<DecodedVideoFrame>()
        private var framesList = listOf<DecodedVideoFrame>()

        private var lastCheckMoreTime = 0L
        private var lastSeekPos = 0L

        private var lastStartTimestampUs: Long = 0
        private var lastStopTimestampUs: Long = 1
        private var lastTranslationX: Int = -1

        override fun onSurfaceChange(width: Int, height: Int) {
            OpenGLUtils.createFBO(frameBuffers, frameBufferTextures, contentWidth, height)
        }

        override fun onSurfaceDestroy() {
            OpenGLUtils.deleteFBO(frameBuffers, frameBufferTextures)
        }

        override fun onDrawFrame() {
            captureFrames()
            drawFrames()
            drawTimelineBar()
        }

        private fun drawTimelineBar() {
            timelineBarDrawer?.run {
                broadcastTimelineChanges()
                setPlayingTimestamp(playingTimestampUs)
                setIntervalUsPerPixel(intervalUsPerPixel)
                setContentPaddingHorizontal(contentPaddingHorizontal)
                draw()
            }
        }

        private inline fun broadcastTimelineChanges() {
            timelineBarDrawer?.run {
                if (lastStartTimestampUs != startTimestampUs || lastStopTimestampUs != stopTimestampUs || lastTranslationX != translationX) {
                    renderingObject.run {
                        graph?.eventCoroutineScope?.launch {
                            val offset = abs(translationX) * intervalUsPerPixel
                            broadcast(TimelineUpdatedEvent(Range(startTimestampUs + offset, stopTimestampUs + offset), videoDurationUs, suspendFrameLoading))
                        }
                    }
                    lastStartTimestampUs = startTimestampUs
                    lastStopTimestampUs = stopTimestampUs
                    lastTranslationX = translationX
                }
            }
        }

        private fun drawFrames() {
            if (frames.isNotEmpty()) {
                /**
                 * 把像素偏移转成时间偏移，intervalUsPerPixel表示每个像素的时间长度(Us)
                 */
                val timeRangeLower = (abs(translationX) - abs(translationX) % frameWidth) * intervalUsPerPixel
                val timeRangeUpper = timeRangeLower + contentWidth * intervalUsPerPixel
                val timeInSec = Range(timeRangeLower / 1000000f, timeRangeUpper / 1000000f)

                val head = framesList.indexOfLast { it.timestampUs <= timeRangeLower }
                val tail = framesList.indexOfLast { it.timestampUs <= timeRangeUpper }

                val intervalUsPerFrame = frameWidth * intervalUsPerPixel
                val framesPerPage = (contentWidth.toFloat() / frameWidth + 0.5f).toInt()
                val xOffset = translationX % frameWidth

//                Log.d(tagOfExtract, "qs = ${frames.size} fpp = $framesPerPage tx = $translationX $timeInSec")

                OpenGLUtils.drawWithFBO(frameBuffers[0], frameBufferTextures[0]) {
                    (0..framesPerPage).forEach { i ->
                        val frameTimestampUs = timeRangeLower + intervalUsPerFrame * i
                        kotlin.runCatching {
                            val frameForTime = framesList.last { it.timestampUs <= frameTimestampUs }
                            if (abs(frameForTime.timestampUs - frameTimestampUs) > 2500000L) {
                                onEncounterMissingFrame(timeRangeLower, timeRangeUpper)
                            }

                            GLES20.glViewport(frameWidth * i + xOffset, 0, frameWidth, frameHeight)

                            frameDrawer?.run {
                                setTextureID(frameForTime.textureId)
                                draw()
                            }
                        }.getOrElse {
                            Log.d(tagOfExtract, "$it $i $frameTimestampUs $timeInSec")
                            onEncounterMissingFrame(timeRangeLower, timeRangeUpper) // 从后往前拖可能触发到这里
                        }
                    }
                }

                GLES20.glViewport(contentPaddingHorizontal, 0, contentWidth, height)

                frameDrawer?.setTextureID(frameBufferTextures[0])
                frameDrawer?.draw()

                GLES20.glViewport(0, 0, width, height)
//                 val bitmap = OpenGLUtils.savePixels(0, 0, width, height)

                onTryToPreloadFrames(head, tail)
            }
        }

        private fun onEncounterMissingFrame(timeRangeLower: Long, timeRangeUpper: Long) {
            val seekPos = (timeRangeLower - 2000000L).coerceAtLeast(0L)
            if (nowSystemClock - lastCheckMoreTime >= 1000 && abs(seekPos - lastSeekPos) >= 2000000L && !suspendFrameLoading) {
                runBlocking {
                    Log.d(tagOfExtract, "load more frames onEncounterMissingFrame pqs=${frames.size} pos=$seekPos")
                    renderingObject.broadcast(DecodeMoreFrames(seekPos))
                    lastSeekPos = seekPos
                    lastCheckMoreTime = nowSystemClock
                }
            }
        }

        private fun onTryToPreloadFrames(head: Int, tail: Int) {
            val seekPos = when {
                head <= frames.size * 0.2 -> (frames.first().timestampUs - 8000000L).coerceAtLeast(0L)
                tail >= frames.size * 0.8 -> frames.last().timestampUs
                else -> -1L
            }

            if (seekPos < 0) return

            if (nowSystemClock - lastCheckMoreTime >= 2000 && abs(seekPos - lastSeekPos) >= 2000000L && !suspendFrameLoading) {
                runBlocking {
                    Log.d(tagOfExtract, "load more frames onTryToPreloadFrames h=$head t=$tail  pqs=${frames.size} pos=$seekPos")
                    renderingObject.broadcast(DecodeMoreFrames(seekPos))
                }
                lastSeekPos = seekPos
                lastCheckMoreTime = nowSystemClock
            }
        }

        private fun captureFrames() {
            var changed = false

            while (textureQueue.isNotEmpty()) {
                when (val d = textureQueue.poll()) {
                    is DecodedVideoFrame -> {
                        /**
                         * 这里根据pts来确认帧是否存在，可能会有问题（某些pts错误的视频文件），但相信大部分情况问题不大
                         */
                        if (frames.contains(d)) {
                            d.sharedTexture.close()
                        } else {
//                            Log.i(tagOfExtract, "pq size: ${pq.size}, capture frame with pts ${d.timestampUs} keyframe: ${d.keyframe}")
                            changed = frames.add(d) // 加入后会根据pts自动排序

                            /**
                             * 新加入帧离哪一端更近，就剔除掉另一端最远的一帧
                             */
                            if (frames.size > frameCapacity) {
                                val first = frames.first()
                                val last = frames.last()
                                if (abs(d.timestampUs - first.timestampUs) < abs(d.timestampUs - last.timestampUs)) {
//                                    Log.i(tagOfExtract, "pq size: ${frames.size}, remove last cached frame (${last.timestampUs})")
                                    frames.remove(last.apply { sharedTexture.close() })
                                } else {
//                                    Log.i(tagOfExtract, "pq size: ${frames.size}, remove first cached frame (${first.timestampUs})")
                                    frames.remove(first.apply { sharedTexture.close() })
                                }
                            }
                        }
                    }
                    is EndOfStream -> renderingObject.setEndOfStream()
                    else -> Log.e(tagOfExtract, "Unknown media data type")
                }
            }

            if (changed) {
                framesList = frames.toList()
            }
        }
    }
}

data class VideoPlayingPosUpdated(val timestampUs: Long) : BaseGraphEvent<MediaData>()