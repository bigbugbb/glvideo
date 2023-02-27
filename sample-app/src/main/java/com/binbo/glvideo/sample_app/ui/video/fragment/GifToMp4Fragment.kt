package com.binbo.glvideo.sample_app.ui.video.fragment

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.core.graph.component.GifSource
import com.binbo.glvideo.core.graph.executor.GraphExecutor
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentGifToMp4Binding
import com.binbo.glvideo.sample_app.impl.video.graph.gif_to_mp4.GifToMp4GraphManager
import com.binbo.glvideo.sample_app.utils.GlideApp
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * 解析出gif的每一帧，再逐帧编码成mp4
 */
class GifToMp4Fragment : Fragment() {

    private var _binding: FragmentGifToMp4Binding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var graphManager: GifToMp4GraphManager

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

        graphManager = GifToMp4GraphManager("converted", 285, 500, createGifFrameProvider(this))

        binding.btnConvert.singleClick {
            lifecycleScope.launch(GraphExecutor.dispatchers) {
                graphManager.createMediaGraph()
                graphManager.prepare()
                graphManager.start()
                graphManager.waitUntilDone()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        runBlocking {
            withContext(GraphExecutor.dispatchers) {
                kotlin.runCatching {
                    graphManager.stop()
                    graphManager.release()
                    graphManager.destroyMediaGraph()
                }
            }
        }

        _binding = null
    }

    private fun createGifFrameProvider(fragment: Fragment) = object : GifSource.GifFrameProvider {
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