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
import com.binbo.glvideo.core.GLVideo.Core.tagOfGraph
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.core.graph.component.GifSource
import com.binbo.glvideo.core.graph.executor.GraphExecutor
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.App.ArgKey.ARG_SELECTED_VIDEO_KEY
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentGifToMp4Binding
import com.binbo.glvideo.sample_app.event.VideoFileCreated
import com.binbo.glvideo.sample_app.impl.video.graph.gif_to_mp4.GifToMp4GraphManager
import com.binbo.glvideo.sample_app.ui.video.activity.VideoPreviewActivity
import com.binbo.glvideo.sample_app.utils.bindToLifecycleOwner
import com.binbo.glvideo.sample_app.utils.rxbus.RxBus
import com.binbo.glvideo.sample_app.utils.thirdparty.GlideApp
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 解析出gif的每一帧，再逐帧编码成mp4
 */
class GifToMp4Fragment : Fragment() {

    private var _binding: FragmentGifToMp4Binding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var graphManager: GifToMp4GraphManager? = null

    private var job: Job? = null

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
            if (job == null) {
                job = GraphExecutor.coroutineScope.launch {
                    graphManager = GifToMp4GraphManager("converted", 285, 500, createGifFrameProvider(this@GifToMp4Fragment))
                    graphManager?.run {
                        createMediaGraph()
                        prepare()
                        start()
                        waitUntilDone()
                    }
                }

                job?.invokeOnCompletion { throwable ->
                    runBlocking {
                        graphManager?.stop()
                        graphManager?.release()
                        graphManager?.destroyMediaGraph()
                        graphManager = null
                    }
                    job = null

                    if (throwable != null) {
                        Log.e(tagOfGraph, throwable.message ?: "")
                    }
                }
            }
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
        job?.cancel()
        job = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = GifToMp4Fragment()
    }
}

fun createGifFrameProvider(fragment: Fragment) = object : GifSource.GifFrameProvider {
    override fun getFrames(): Flow<GifSource.GifFrame> = callbackFlow {
        val target = GlideApp.with(fragment)
            .asGif()
            .load(R.raw.sample_gif)
            .into(object : CustomTarget<GifDrawable>() {
                override fun onResourceReady(resource: GifDrawable, transition: Transition<in GifDrawable>?) {
                    kotlin.runCatching {
                        val gifState = resource.constantState!!
                        val frameLoader = gifState.javaClass.getDeclaredField("frameLoader");
                        frameLoader.isAccessible = true
                        val gifFrameLoader = frameLoader.get(gifState)

                        val gifDecoder = gifFrameLoader.javaClass.getDeclaredField("gifDecoder");
                        gifDecoder.isAccessible = true
                        val standardGifDecoder = gifDecoder.get(gifFrameLoader) as StandardGifDecoder
                        (0 until standardGifDecoder.frameCount).forEach { _ ->
                            standardGifDecoder.advance()
                            standardGifDecoder.nextFrame?.let { bitmap ->
                                trySend(GifSource.GifFrame(bitmap, standardGifDecoder.nextDelay))
                            }
                        }
                    }.getOrElse {
                        it.printStackTrace()
                    }

                    close()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    close()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })

        awaitClose { GlideApp.with(fragment).clear(target) }
    }
}