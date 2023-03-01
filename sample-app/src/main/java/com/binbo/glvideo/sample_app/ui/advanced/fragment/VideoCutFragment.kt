package com.binbo.glvideo.sample_app.ui.advanced.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Range
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.binbo.glvideo.core.ext.setVisible
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.core.graph.event.StartFrameBuffering
import com.binbo.glvideo.core.graph.event.StopFrameBuffering
import com.binbo.glvideo.core.graph.executor.GraphExecutor
import com.binbo.glvideo.sample_app.App.ArgKey.ARG_VIDEO_PATH_KEY
import com.binbo.glvideo.sample_app.databinding.FragmentVideoCutBinding
import com.binbo.glvideo.sample_app.event.CreateVideoCutFileFailed
import com.binbo.glvideo.sample_app.event.CreateVideoCutFileSuccess
import com.binbo.glvideo.sample_app.event.TimelineUpdatedEvent
import com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.crop.VideoCropGraphManager
import com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.extract.VideoExtractionConfig
import com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.extract.VideoExtractionGraphManager
import com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.extract.tagOfExtract
import com.binbo.glvideo.sample_app.utils.bindToLifecycleOwner
import com.binbo.glvideo.sample_app.utils.doClickVibrator
import com.binbo.glvideo.sample_app.utils.rxbus.HeartBeatEvent
import com.binbo.glvideo.sample_app.utils.rxbus.RxBus
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class VideoCutFragment : Fragment() {

    private var job: Job? = null

    private var webpPath = ""
    private var degree = 0
    private var loading = false

    private var cropGraphManager: VideoCropGraphManager? = null
    private var extractGraphManager: VideoExtractionGraphManager? = null

    private val videoUri: Uri
        get() = Uri.fromFile(File(arguments?.getString(ARG_VIDEO_PATH_KEY) ?: ""))

    private val timeline: Range<Long>
        get() = extractGraphManager?.timeline ?: Range(0L, 2000000L)

    private var _binding: FragmentVideoCutBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVideoCutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        extractGraphManager = VideoExtractionGraphManager(videoUri, binding.viewVideoContainer, binding.viewFrames).apply {
            lifecycle.addObserver(this)
        }

        binding.cardCancel.singleClick {
            activity?.finish()
        }

        binding.cardConfirm.singleClick {
            onConfirmed()
        }

        binding.imageRotate.singleClick {
            doClickVibrator()
            degree = (degree + 90) % 360
            extractGraphManager?.setVideoRotation(degree)
        }

        RxBus.getDefault().onEvent(StartFrameBuffering::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { onStartFrameBuffering(it) }
            .bindToLifecycleOwner(this)

        RxBus.getDefault().onEvent(StopFrameBuffering::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { onStopFrameBuffering(it) }
            .bindToLifecycleOwner(this)

        RxBus.getDefault().onEvent(TimelineUpdatedEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { onTimelineUpdated(it) }
            .bindToLifecycleOwner(this)

        RxBus.getDefault().onEvent(HeartBeatEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { onHeartBeatEvent(it) }
            .bindToLifecycleOwner(this)

        RxBus.getDefault().onEvent(CreateVideoCutFileSuccess::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { onCreateVideoCutFileSuccess(it) }
            .bindToLifecycleOwner(this)

        RxBus.getDefault().onEvent(CreateVideoCutFileFailed::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { onCreateVideoCutFileFailed(it) }
            .bindToLifecycleOwner(this)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        job = null
    }

    private fun onConfirmed() {
        if (job == null) {
            binding.viewLoading.setVisible(true)

            job = GraphExecutor.coroutineScope.launch {
                Log.d(tagOfExtract, "onConfirmed timeline(${timeline.lower}, ${timeline.upper}, $degree)")
                cropGraphManager = VideoCropGraphManager(videoUri, timeline, degree)
                cropGraphManager?.run {
                    createMediaGraph()
                    prepare()
                    start()
                    waitUntilDone()
                }
            }

            job?.invokeOnCompletion { throwable ->
                runBlocking {
                    cropGraphManager?.stop()
                    cropGraphManager?.release()
                    cropGraphManager?.destroyMediaGraph()
                    cropGraphManager = null
                }
                job = null

                if (throwable != null) {
                    RxBus.getDefault().send(CreateVideoCutFileFailed(throwable))
                }
            }
        }
    }

    private fun onStartFrameBuffering(event: StartFrameBuffering) {
        loading = true
    }

    private fun onStopFrameBuffering(event: StopFrameBuffering) {
        loading = false
    }

    private fun onTimelineUpdated(event: TimelineUpdatedEvent) {
        val timelineDuration = (timeline.upper - timeline.lower) / 1000000f
        binding.textDuration.text =
            String.format("%.1fs / %.1fs", timelineDuration, event.videoDurationTs.coerceAtMost(VideoExtractionConfig.maxExtractDuration) / 1000000f)
    }

    private fun onHeartBeatEvent(event: HeartBeatEvent) {
        extractGraphManager?.let {
            binding.viewGraduation.timeRange = it.visibleTimeRange
        }
    }

    private fun onCreateVideoCutFileSuccess(event: CreateVideoCutFileSuccess) {
        binding.viewLoading.setVisible(false)
        webpPath = event.webpPath
        activity?.finish()
    }

    private fun onCreateVideoCutFileFailed(event: CreateVideoCutFileFailed) {
        binding.viewLoading.setVisible(false)
        webpPath = ""
    }
}