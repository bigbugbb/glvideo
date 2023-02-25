package com.binbo.glvideo.sample_app.ui.advanced.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentVideoCutBinding
import com.binbo.glvideo.sample_app.databinding.FragmentVideoCutSelectBinding
import com.binbo.glvideo.sample_app.ui.advanced.activity.VideoCutActivity
import com.binbo.glvideo.sample_app.ui.advanced.activity.VideoCutActivity.Companion.ARG_SELECT_VIDEO_KEY
import com.binbo.glvideo.sample_app.ui.advanced.fragment.VideoCutFragment.Companion.ARG_VIDEO_PATH_KEY
import com.binbo.glvideo.sample_app.ui.video.activity.VideoDecodeActivity
import com.binbo.glvideo.sample_app.utils.toast
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.config.SelectModeConfig
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 基于gif to mp4，给生成的视频加上音轨
 */
class VideoCutSelectFragment : Fragment() {

    private var _binding: FragmentVideoCutSelectBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val resultCallback = object : OnResultCallbackListener<LocalMedia> {
        override fun onResult(result: ArrayList<LocalMedia>?) {
            val videoDuration = result?.elementAtOrNull(0)?.duration ?: 0
            val videoPath = result?.elementAtOrNull(0)?.realPath ?: ""
            Log.i(TAG, "PictureSelector onResult  $videoDuration")
            if (videoDuration <= 2000) {
                lifecycleScope.launch(Dispatchers.Main) {
                    App.context.toast(App.context.getString(R.string.select_video_time_toast), Toast.LENGTH_SHORT)
                }
            } else {
                startActivity(Intent(requireContext(), VideoCutActivity::class.java).apply {
                    putExtra(ARG_SELECT_VIDEO_KEY, bundleOf(ARG_VIDEO_PATH_KEY to videoPath))
                })
            }
        }

        override fun onCancel() {
            Log.i(TAG, "PictureSelector Cancel")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentVideoCutSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSelectVideo.singleClick {
            PictureSelector.create(this)
                .openSystemGallery(SelectMimeType.ofVideo())
                .setSelectFilterListener { it.mimeType != PictureMimeType.ofMP4() }
                .setSelectionMode(SelectModeConfig.SINGLE)
                .forSystemResult(resultCallback)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "VideoCutSelectFragment"

        @JvmStatic
        fun newInstance() = VideoCutSelectFragment()
    }
}