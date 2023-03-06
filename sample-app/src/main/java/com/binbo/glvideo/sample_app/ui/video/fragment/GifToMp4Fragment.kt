package com.binbo.glvideo.sample_app.ui.video.fragment

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.core.graph.GraphJob
import com.binbo.glvideo.core.graph.component.GifSource
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.App.ArgKey.ARG_SELECTED_VIDEO_KEY
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentGifToMp4Binding
import com.binbo.glvideo.sample_app.event.VideoFileCreated
import com.binbo.glvideo.sample_app.impl.video.graph.GifToMp4GraphManager
import com.binbo.glvideo.sample_app.ui.video.activity.VideoPreviewActivity
import com.binbo.glvideo.sample_app.utils.bindToLifecycleOwner
import com.binbo.glvideo.sample_app.utils.rxbus.RxBus
import com.binbo.glvideo.sample_app.utils.thirdparty.GlideApp
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * 解析出gif的每一帧，再逐帧编码成mp4
 */
class GifToMp4Fragment : Fragment() {

    private var _binding: FragmentGifToMp4Binding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var graphJob: GraphJob = GraphJob(object : GraphJob.GraphManagerProvider {
        override fun onGraphManagerRequested(): BaseGraphManager {
            return GifToMp4GraphManager("converted", 285, 500, createGifFrameProvider(this@GifToMp4Fragment))
        }
    })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGifToMp4Binding.inflate(inflater, container, false)
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
        graphJob.cancel()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = GifToMp4Fragment()
    }
}

fun createGifFrameProvider(fragment: Fragment, gifRawId: Int = R.raw.sample_gif) = object : GifSource.GifFrameProvider {
    override fun getFrames(): Flow<GifSource.GifFrame> = callbackFlow {
        val tag = "GifSource"
        val target = GlideApp.with(fragment)
            .asGif()
            .load(gifRawId)
            .into(object : CustomTarget<GifDrawable>() {
                override fun onResourceReady(resource: GifDrawable, transition: Transition<in GifDrawable>?) {
                    kotlin.runCatching {
                        val gifState = resource.constantState!!
                        val frameLoader = gifState.javaClass.getDeclaredField("frameLoader")
                        frameLoader.isAccessible = true
                        val gifFrameLoader = frameLoader.get(gifState)

                        val gifDecoder = gifFrameLoader.javaClass.getDeclaredField("gifDecoder");
                        gifDecoder.isAccessible = true
                        val standardGifDecoder = gifDecoder.get(gifFrameLoader) as StandardGifDecoder
                        (0 until standardGifDecoder.frameCount).forEach { i ->
                            standardGifDecoder.advance()
                            standardGifDecoder.nextFrame?.let { bitmap ->
                                trySend(GifSource.GifFrame(bitmap, standardGifDecoder.nextDelay))
                                    .onSuccess { Log.i(tag, "$i onSuccess") }
                                    .onFailure { t: Throwable? -> Log.i(tag, "$i onFailure $t") }
                            }
                        }
                    }.getOrElse {
                        it.printStackTrace()
                    }

                    close()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    cancel(CancellationException("load gif failed"))
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })

        awaitClose { GlideApp.with(fragment).clear(target) }
        Log.i(tag, "awaitClose return")
    }
}