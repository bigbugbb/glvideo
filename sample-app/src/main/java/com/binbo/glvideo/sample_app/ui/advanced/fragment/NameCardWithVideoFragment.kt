package com.binbo.glvideo.sample_app.ui.advanced.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.binbo.glvideo.core.media.BaseMediaEncoder
import com.binbo.glvideo.core.media.encoder.MediaVideoEncoder
import com.binbo.glvideo.core.media.muxer.MediaMuxerManager
import com.binbo.glvideo.core.media.recorder.DefaultGLRecorder
import com.binbo.glvideo.core.media.recorder.GLRecorderConfig
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.App.Const.recordVideoExt
import com.binbo.glvideo.sample_app.App.Const.recordVideoSize
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentNameCardWithVideoBinding
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.renderer.NameCardRenderer
import com.binbo.glvideo.sample_app.utils.FileToolUtils
import com.binbo.glvideo.sample_app.utils.FileToolUtils.getFile
import com.binbo.glvideo.sample_app.utils.FileUseCase.Companion.NAME_CARD_WITH_VIDEO
import com.binbo.glvideo.sample_app.utils.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class NameCardWithVideoFragment : Fragment() {

    private var _binding: FragmentNameCardWithVideoBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var nameCardRecorder: DefaultGLRecorder

    private var videoFilePath = MediaMuxerManager.getCaptureFile(recorderConfig.targetFileDir, recorderConfig.targetFilename, recordVideoExt)

    private val recorderConfig: GLRecorderConfig
        get() = GLRecorderConfig.build {
            width(recordVideoSize.width)
            height(recordVideoSize.height)
            targetFileDir(getFile(NAME_CARD_WITH_VIDEO))
            targetFilename("name_card_with_video")
        }

    private val encoderListener = object : BaseMediaEncoder.MediaEncoderListener { // the inner class holders the weak ref
        override fun onPrepared(encoder: BaseMediaEncoder?) {
            (binding.viewNameCard.renderer as NameCardRenderer).encoder = encoder as? MediaVideoEncoder?
        }

        override fun onStopped(encoder: BaseMediaEncoder?) {
            FileToolUtils.writeVideoToGallery(File(videoFilePath), "video/mp4")
            lifecycleScope.launch {
                App.context.toast(App.context.getString(R.string.video_recording_successful_message), Toast.LENGTH_SHORT)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNameCardWithVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nameCardRecorder = DefaultGLRecorder(requireContext())
        nameCardRecorder.startRecording(recorderConfig, encoderListener)

        lifecycleScope.launch {
            delay(3000)
            nameCardRecorder.stopRecording()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.viewNameCard.onResume()
    }

    override fun onStop() {
        super.onStop()
        binding.viewNameCard.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        nameCardRecorder.stopRecording()
    }

    companion object {
        @JvmStatic
        fun newInstance() = NameCardWithVideoFragment()
    }
}