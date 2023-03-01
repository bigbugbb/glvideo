package com.binbo.glvideo.sample_app.ui.video.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.sample_app.databinding.FragmentVideoBinding
import com.binbo.glvideo.sample_app.ui.capture.viewmodel.CaptureViewModel
import com.binbo.glvideo.sample_app.ui.video.activity.*

class VideoFragment : Fragment() {

    private var _binding: FragmentVideoBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val captureViewModel = ViewModelProvider(this).get(CaptureViewModel::class.java)

        _binding = FragmentVideoBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVideoDecode.singleClick {
            startActivity(Intent(requireContext(), VideoDecodeActivity::class.java))
        }

        binding.btnGifToMp4.singleClick {
            startActivity(Intent(requireContext(), GifToMp4Activity::class.java))
        }

        binding.btnAddVideoBgm.singleClick {
            startActivity(Intent(requireContext(), AddVideoBgmActivity::class.java))
        }

        binding.btnAddVideoEnding.singleClick {
            startActivity(Intent(requireContext(), AddVideoEndingActivity::class.java))
        }

        binding.btnAddWatermark.singleClick {
            startActivity(Intent(requireContext(), AddWatermarkActivity::class.java))
        }

        binding.btnVideoCrop.singleClick {
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
