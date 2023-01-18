package com.binbo.glvideo.core.graph.utils

import android.opengl.EGLContext
import android.opengl.GLES20
import com.binbo.glvideo.core.exception.OutOfSharedTextureException
import com.binbo.glvideo.core.graph.interfaces.IGraphEGLResource
import com.binbo.glvideo.core.opengl.egl.EGLSurfaceHolder
import com.binbo.glvideo.core.opengl.egl.EGL_RECORDABLE_ANDROID
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import kotlinx.coroutines.*
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater


/**
 * This texture pool is used for sharing gl texture among different media objects in the same media graph.
 *
 * @author bigbug
 * @project lobby
 * @date 2022/7/20
 * @time 15:49
 */
open class MediaTexturePool : IGraphEGLResource {

    private val shared = CopyOnWriteArrayList<SharedMediaTexture>()

    private val created = AtomicBoolean(false)
    private val prepared = AtomicBoolean(false)

    /**
     * The thread has its own egl context, the functionality of the shared texture pool
     * depends on it.
     */
    private lateinit var executor: ExecutorService

    lateinit var dispatcher: CoroutineDispatcher
        private set

    lateinit var coroutineScope: CoroutineScope
        private set

    private val eglSurface = EGLSurfaceHolder()

    override val sharedContext: EGLContext // valid after prepared
        get() = eglSurface.getEGLContext()

    override fun create() {
        if (!created.getAndSet(true)) {
            executor = Executors.newFixedThreadPool(1)
            dispatcher = executor.asCoroutineDispatcher()
            coroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
        }
    }

    override suspend fun prepare() = withContext(dispatcher) {
        if (!prepared.getAndSet(true)) {
            eglSurface.init(null, EGL_RECORDABLE_ANDROID)
            eglSurface.createEGLSurface(null, 720, 1280)
            eglSurface.makeCurrent()
        }
    }

    override suspend fun getMediaTextures(
        width: Int,
        height: Int,
        countToGet: Int,
        maxAllowedCount: Int,
        waitTimeout: Long
    ): List<SharedMediaTexture> = withContext(dispatcher) {
        if (!prepared.get()) {
            error("pool is not prepared")
        }

        val resultOfTextures = mutableListOf<SharedMediaTexture>()
        val sharedForDimen = shared.filter { it.width == width && it.height == height }
        run populateResult@{
            sharedForDimen.forEach {
                if (it.compareAndSet(SharedMediaTexture.STATE_NOT_IN_USE, SharedMediaTexture.STATE_IN_USE)) {
                    resultOfTextures += it
                    if (resultOfTextures.size >= countToGet) {
                        return@populateResult
                    }
                }
            }
        }
        // doesn't have enough available textures to use
        if (resultOfTextures.size < countToGet) {
            // alloc more textures if we can
            if (sharedForDimen.size < maxAllowedCount) {
                createMediaTextures(width, height, countToGet - resultOfTextures.size).forEach {
                    if (it.compareAndSet(SharedMediaTexture.STATE_NOT_IN_USE, SharedMediaTexture.STATE_IN_USE)) {
                        resultOfTextures += it
                    }
                }
            } else {
                // wait for available shared textures to close so that we can reuse it
                var remainingTimeToWait = waitTimeout
                while (resultOfTextures.size < countToGet && remainingTimeToWait > 0) {
                    delay(10)
                    remainingTimeToWait -= 10
                    shared.filter { it.width == width && it.height == height }.forEach {
                        if (it.compareAndSet(SharedMediaTexture.STATE_NOT_IN_USE, SharedMediaTexture.STATE_IN_USE)) {
                            resultOfTextures += it
                        }
                    }
                }

                if (resultOfTextures.size < countToGet) {
                    throw OutOfSharedTextureException(width, height, countToGet)
                }
            }
        }
        resultOfTextures
    }

    override suspend fun createMediaTextures(width: Int, height: Int, count: Int): List<SharedMediaTexture> = withContext(dispatcher) {
        if (!prepared.get()) {
            error("pool is not prepared")
        }
        val textures = IntArray(count)
        OpenGLUtils.glGenTexturesWithDimen(textures, width, height)
        textures.map { SharedMediaTexture(it, width, height) }.apply {
            shared.addAll(this)
        }
    }

    override suspend fun release() = withContext(dispatcher) {
        if (prepared.getAndSet(false)) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE)
            GLES20.glDeleteTextures(shared.size, shared.map { it.textureId }.toIntArray(), 0)
            shared.clear()
            eglSurface.destroyEGLSurface()
            eglSurface.release()
        }
    }

    override fun destroy() {
        if (created.getAndSet(false)) {
            kotlin.runCatching { coroutineScope.cancel() }
            executor.shutdown()
        }
    }

    data class SharedMediaTexture(val textureId: Int, val width: Int, val height: Int) : Closeable {

        @Volatile
        private var _state: Int = STATE_NOT_IN_USE

        internal var state: Int
            get() = _state
            set(value) {
                stateUpdater.set(this, value)
            }

        // 包级访问，仅需对SharedGLTexture可见
        internal fun compareAndSet(expect: Int, update: Int): Boolean {
            return stateUpdater.compareAndSet(this, expect, update)
        }

        override fun close() {
            stateUpdater.set(this, STATE_NOT_IN_USE)
        }

        companion object {
            const val STATE_NOT_IN_USE = 0
            const val STATE_IN_USE = 1

            private val stateUpdater = AtomicIntegerFieldUpdater.newUpdater(SharedMediaTexture::class.java, "_state")
        }
    }
}