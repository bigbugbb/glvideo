package com.binbo.glvideo.sample_app.ui.video.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.binbo.glvideo.core.graph.GraphJob
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.sample_app.App.Const.sampleVideoUri
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentVideoDecodeBinding
import com.binbo.glvideo.sample_app.impl.video.graph.VideoDecodeGraphManager
import com.binbo.glvideo.sample_app.ui.capture.fragment.CameraPreviewCustomFragment

class VideoDecodeFragment : Fragment() {

    private var _binding: FragmentVideoDecodeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var graphJob: GraphJob = GraphJob(object : GraphJob.GraphManagerProvider {
        override fun onGraphManagerRequested(): BaseGraphManager {
            return VideoDecodeGraphManager(sampleVideoUri, R.raw.sample_video, binding.viewSurface)
        }
    })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentVideoDecodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        graphJob.execute()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        graphJob.cancel()
        _binding = null
    }


    companion object {
        @JvmStatic
        fun newInstance() = CameraPreviewCustomFragment()
    }
}