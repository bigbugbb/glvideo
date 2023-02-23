package com.binbo.glvideo.sample_app.ui.capture.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.sample_app.databinding.FragmentCaptureBinding
import com.binbo.glvideo.sample_app.ui.capture.*
import com.binbo.glvideo.sample_app.ui.capture.viewmodel.CaptureViewModel

class CaptureFragment : Fragment() {

    private var _binding: FragmentCaptureBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val captureViewModel = ViewModelProvider(this).get(CaptureViewModel::class.java)

        _binding = FragmentCaptureBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCameraPreview.singleClick {
            startActivity(Intent(requireContext(), CameraPreviewActivity::class.java))
        }

        binding.btnCameraPreviewCustom.singleClick {
            startActivity(Intent(requireContext(), CameraPreviewCustomActivity::class.java))
        }

        binding.btnCameraPreviewWithEffect.singleClick {
            startActivity(Intent(requireContext(), CameraPreviewEffectsActivity::class.java))
        }

        binding.btnPictureTaking.singleClick {
            startActivity(Intent(requireContext(), PictureTakingActivity::class.java))
        }

        binding.btnVideoRecording.singleClick {
            startActivity(Intent(requireContext(), VideoRecordingActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}