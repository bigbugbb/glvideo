package com.binbo.glvideo.sample_app.ui.video.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.core.graph.executor.GraphExecutor
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.App.Const.sampleVideoUri
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentAddWatermarkBinding
import com.binbo.glvideo.sample_app.impl.video.graph.add_watermark.AddWatermarkGraphManager
import com.binbo.glvideo.sample_app.utils.player.VideoPlayerDelegate
import com.kk.taurus.playerbase.assist.RelationAssist
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class AddWatermarkFragment : Fragment() {

    private var _binding: FragmentAddWatermarkBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var graphManager: AddWatermarkGraphManager

    private val player = object : VideoPlayerDelegate(RelationAssist(App.context)) {
        override fun onPlayComplete() {
            if (isLoopingEnabled) {
                seekTo(0)
                assist.play()
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

        graphManager = AddWatermarkGraphManager(sampleVideoUri, R.raw.sample_video).apply {
            createMediaGraph()
        }

        binding.btnConvert.singleClick {
            lifecycleScope.launch(GraphExecutor.dispatchers) {
                graphManager.prepare()
                graphManager.start()
                graphManager.waitUntilDone()
            }
        }
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

        runBlocking {
            withContext(GraphExecutor.dispatchers) {
                graphManager.stop()
                graphManager.release()
                graphManager.destroyMediaGraph()
            }
        }

        player.reset()
        player.destroy()

        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = AddWatermarkFragment()
    }
}