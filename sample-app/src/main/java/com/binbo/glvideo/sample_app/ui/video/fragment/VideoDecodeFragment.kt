package com.binbo.glvideo.sample_app.ui.video.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.binbo.glvideo.core.graph.executor.GraphExecutor
import com.binbo.glvideo.core.utils.FileToolUtils
import com.binbo.glvideo.core.utils.FileUseCase
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.databinding.FragmentVideoDecodeBinding
import com.binbo.glvideo.sample_app.impl.video.graph.video_decode.VideoDecodeGraphManager
import com.binbo.glvideo.sample_app.ui.capture.fragment.CameraPreviewCustomFragment
import com.binbo.glvideo.sample_app.utils.FileUtil
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

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

        val dir = FileToolUtils.getFile(FileUseCase.VIDEO_TO_DECODE, "").apply { deleteRecursively() }
        FileUtil.copyAssets(App.context, "maksim.mp4", "video", dir.absolutePath, null)
        val file = FileToolUtils.getFile(FileUseCase.VIDEO_TO_DECODE, "maksim.mp4")

        graphManager = VideoDecodeGraphManager(Uri.fromFile(file), binding.viewSurface)

        runBlocking {
            withContext(GraphExecutor.dispatchers) {
                graphManager.createMediaGraph()
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