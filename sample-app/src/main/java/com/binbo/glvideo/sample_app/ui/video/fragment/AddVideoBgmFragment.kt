package com.binbo.glvideo.sample_app.ui.video.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.core.graph.GraphJob
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.App.ArgKey.ARG_SELECTED_VIDEO_KEY
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentAddVideoBgmBinding
import com.binbo.glvideo.sample_app.event.VideoFileCreated
import com.binbo.glvideo.sample_app.impl.video.graph.AddVideoBgmGraphManager
import com.binbo.glvideo.sample_app.ui.video.activity.VideoPreviewActivity
import com.binbo.glvideo.sample_app.utils.bindToLifecycleOwner
import com.binbo.glvideo.sample_app.utils.rxbus.RxBus
import com.binbo.glvideo.sample_app.utils.thirdparty.GlideApp
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

/**
 * 基于gif to mp4，给生成的视频加上音轨
 */
class AddVideoBgmFragment : Fragment() {

    private var _binding: FragmentAddVideoBgmBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var graphJob: GraphJob = GraphJob(object : GraphJob.GraphManagerProvider {
        override fun onGraphManagerRequested(): BaseGraphManager {
            return AddVideoBgmGraphManager("video_with_bgm", 285, 500, createGifFrameProvider(this@AddVideoBgmFragment))
        }
    })

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

        binding.cardConvert.singleClick {
            graphJob.run()
        }

        RxBus.getDefault().onEvent(VideoFileCreated::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                startActivity(Intent(activity, VideoPreviewActivity::class.java).apply {
                    putExtra(ARG_SELECTED_VIDEO_KEY, bundleOf(App.ArgKey.ARG_VIDEO_PATH_KEY to it.videoPath))
                })
            }
            .bindToLifecycleOwner(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        graphJob.cancel()
    }

    companion object {
        @JvmStatic
        fun newInstance() = AddVideoBgmFragment()
    }
}