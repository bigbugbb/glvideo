package com.binbo.glvideo.sample_app.ui.video.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.App.ArgKey.ARG_VIDEO_PATH_KEY
import com.binbo.glvideo.sample_app.databinding.FragmentVideoPreviewBinding
import com.binbo.glvideo.sample_app.utils.player.VideoPlayerDelegate
import com.kk.taurus.playerbase.assist.RelationAssist
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class VideoPreviewFragment : Fragment() {

    private var _binding: FragmentVideoPreviewBinding? = null

    private val binding get() = _binding!!

    private var player: VideoPlayerDelegate = object : VideoPlayerDelegate(RelationAssist(App.context)) {
        override fun onPlayComplete() {
            if (isLoopingEnabled) {
                lifecycleScope.launch {
                    delay(100) // the system player needs some time to reset its inner status
                    assist.seekTo(0)
                    assist.play()
                }
            }
        }
    }.apply { isLoopingEnabled = true }

    private val videoUri: Uri
        get() = Uri.fromFile(File(arguments?.getString(ARG_VIDEO_PATH_KEY) ?: ""))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentVideoPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imageBack.singleClick {
            activity?.finish()
        }

        playVideo(videoUri)
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

        _binding = null
    }

    private fun playVideo(videoUri: Uri) {
        with(player) {
            setDataSource(videoUri)
            attachContainer(binding.viewVideoContainer)
            playVideo(binding.viewVideoContainer)
        }
    }
}