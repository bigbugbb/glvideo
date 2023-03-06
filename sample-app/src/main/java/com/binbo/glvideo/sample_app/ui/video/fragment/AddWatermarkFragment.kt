package com.binbo.glvideo.sample_app.ui.video.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.core.graph.GraphJob
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.App.ArgKey.ARG_SELECTED_VIDEO_KEY
import com.binbo.glvideo.sample_app.App.Const.sampleVideoUri
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentAddWatermarkBinding
import com.binbo.glvideo.sample_app.event.VideoFileCreated
import com.binbo.glvideo.sample_app.impl.video.graph.AddWatermarkGraphManager
import com.binbo.glvideo.sample_app.ui.video.activity.VideoPreviewActivity
import com.binbo.glvideo.sample_app.utils.bindToLifecycleOwner
import com.binbo.glvideo.sample_app.utils.player.VideoPlayerDelegate
import com.binbo.glvideo.sample_app.utils.rxbus.RxBus
import com.kk.taurus.playerbase.assist.RelationAssist
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AddWatermarkFragment : Fragment() {

    private var _binding: FragmentAddWatermarkBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var graphJob: GraphJob = GraphJob(object : GraphJob.GraphManagerProvider {
        override fun onGraphManagerRequested(): BaseGraphManager {
            return AddWatermarkGraphManager(sampleVideoUri, R.raw.sample_video)
        }
    })

    private val player = object : VideoPlayerDelegate(RelationAssist(App.context)) {
        override fun onPlayComplete() {
            if (isLoopingEnabled) {
                lifecycleScope.launch {
                    delay(100) // the system player needs some time to reset its inner status
                    assist.seekTo(0)
                    assist.play()
                }
            }
        }
    }.apply {
        isLoopingEnabled = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddWatermarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            with(player) {
                setDataSource(sampleVideoUri)
                attachContainer(binding.viewVideoContainer)
                playVideo(binding.viewVideoContainer)
            }
        }

        binding.cardConvert.singleClick {
            graphJob.run()
        }

        RxBus.getDefault().onEvent(VideoFileCreated::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                startActivity(Intent(activity, VideoPreviewActivity::class.java).apply {
                    putExtra(ARG_SELECTED_VIDEO_KEY, bundleOf(App.ArgKey.ARG_VIDEO_PATH_KEY to it.videoPath))
                })
            }
            .bindToLifecycleOwner(this)
    }

    override fun onStart() {
        super.onStart()
        player.resume()
    }

    override fun onStop() {
        super.onStop()
        player.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        player.reset()
        player.destroy()

        graphJob.cancel()

        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = AddWatermarkFragment()
    }
}