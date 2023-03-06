package com.binbo.glvideo.sample_app.ui.video.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.binbo.glvideo.core.graph.executor.GraphExecutor
import com.binbo.glvideo.sample_app.App.Const.sampleVideoUri
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentVideoDecodeBinding
import com.binbo.glvideo.sample_app.impl.video.graph.VideoDecodeGraphManager
import com.binbo.glvideo.sample_app.ui.capture.fragment.CameraPreviewCustomFragment
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class VideoDecodeFragment : Fragment() {

    private var _binding: FragmentVideoDecodeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var graphManager: VideoDecodeGraphManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentVideoDecodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        graphManager = VideoDecodeGraphManager(sampleVideoUri, R.raw.sample_video, binding.viewSurface).apply {
            createMediaGraph()
        }

        runBlocking {
            withContext(GraphExecutor.dispatchers) {
                graphManager.prepare()
                graphManager.start()
            }
        }
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
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = CameraPreviewCustomFragment()
    }
}