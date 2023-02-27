package com.binbo.glvideo.sample_app.ui.video.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.core.graph.executor.GraphExecutor
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentAddVideoBgmBinding
import com.binbo.glvideo.sample_app.impl.video.graph.add_video_bgm.AddVideoBgmGraphManager
import com.binbo.glvideo.sample_app.utils.GlideApp
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * 基于gif to mp4，给生成的视频加上音轨
 */
class AddVideoBgmFragment : Fragment() {

    private var _binding: FragmentAddVideoBgmBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var graphManager: AddVideoBgmGraphManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddVideoBgmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GlideApp.with(this)
            .asGif()
            .load(R.raw.sample_gif)
            .into(binding.imageGif)

        graphManager = AddVideoBgmGraphManager("video_with_bgm", 285, 500, createGifFrameProvider(this))

        binding.btnConvert.singleClick {
            lifecycleScope.launch(GraphExecutor.dispatchers) {
                graphManager.createMediaGraph()
                graphManager.prepare()
                graphManager.start()
                graphManager.waitUntilDone()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        runBlocking {
            withContext(GraphExecutor.dispatchers) {
                kotlin.runCatching {
                    graphManager.stop()
                    graphManager.release()
                    graphManager.destroyMediaGraph()
                }
            }
        }

        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = AddVideoBgmFragment()
    }
}