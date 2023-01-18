package com.binbo.glvideo.core.graph.base

import android.util.ArrayMap
import android.util.Log
import androidx.annotation.IntDef
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.binbo.glvideo.core.GLVideo.Core.tagOfGraph
import com.binbo.glvideo.core.exception.MediaException
import com.binbo.glvideo.core.graph.VisitGraphComplete
import com.binbo.glvideo.core.graph.VisitGraphError
import com.binbo.glvideo.core.graph.VisitGraphSuccess
import com.binbo.glvideo.core.graph.interfaces.*
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.core.graph.utils.MediaTexturePool
import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 * @author bigbug
 * @project lobby
 * @date 2022/7/12
 * @time 19:30
 */

@IntDef(
    GraphState.IDLE,
    GraphState.PREPARED,
    GraphState.STARTED,
    GraphState.FLUSHING,
    GraphState.STOPPED
)
@Retention(AnnotationRetention.SOURCE)
annotation class GraphState {
    companion object {
        const val IDLE = 0
        const val PREPARED = 1
        const val STARTED = 2
        const val FLUSHING = 3
        const val STOPPED = 4
    }
}

@IntDef(
    Command.COMMAND_PREPARE,
    Command.COMMAND_START,
    Command.COMMAND_BEGIN_FLUSH,
    Command.COMMAND_END_FLUSH,
    Command.COMMAND_STOP,
    Command.COMMAND_RELEASE
)
@Retention(AnnotationRetention.SOURCE)
annotation class Command {
    companion object {
        const val COMMAND_PREPARE = 1
        const val COMMAND_START = 2
        const val COMMAND_BEGIN_FLUSH = 3
        const val COMMAND_END_FLUSH = 4
        const val COMMAND_STOP = 5
        const val COMMAND_RELEASE = 6
    }
}

abstract class BaseGraphEvent<D : IMediaData>(val from: BaseMediaObject<D>? = null, val to: BaseMediaObject<D>? = null)

abstract class BaseMediaGraph<D : IMediaData>(val graphManager: BaseGraphManager) : IMediaGraph<D>, LifecycleOwner {

    @Volatile var _state: Int = GraphState.IDLE

    override var state: Int
        get() = _state
        set(value) {
            _state = value
        }

    override val mediaObjects = ArrayMap<String, BaseMediaObject<D>>()

    override lateinit var mediaSource: BaseMediaSource<D>
    override lateinit var mediaSink: BaseMediaSink<D>

    override lateinit var visitor: IGraphVisitor
    override lateinit var eglResource: IGraphEGLResource

    private val created = AtomicBoolean(false)

    private lateinit var eventExecutor: ExecutorService

    lateinit var eventDispatcher: CoroutineDispatcher
        private set

    lateinit var eventCoroutineScope: CoroutineScope
        private set

    override fun create() {
        if (!created.getAndSet(true)) {
            visitor = createGraphVisitor()
            eventExecutor = Executors.newFixedThreadPool(1)
            eventDispatcher = eventExecutor.asCoroutineDispatcher()
            eventCoroutineScope = CoroutineScope(SupervisorJob() + eventDispatcher)
            eglResource = createGraphEGLResource().apply {
                create()
            }
            onCreate()
        }
    }

    override fun destroy() {
        if (created.getAndSet(false)) {
            kotlin.runCatching { eventCoroutineScope.cancel() }
            eventExecutor.shutdown()
            eglResource.destroy()
            onDestroy()
        }
    }

    protected open fun createGraphVisitor(): IGraphVisitor {
        return BaseGraphVisitor(this)
    }

    protected open fun createGraphEGLResource(): IGraphEGLResource {
        return MediaTexturePool()
    }

    override fun onCreate() {
    }

    override fun onDestroy() {
    }

    override fun onVisitGraphSuccess(@DirType dirType: Int, @Command command: Int) {
    }

    override fun onVisitGraphError(e: MediaException) {
        Log.d("MediaGraph", "onVisitGraphError $e")
    }

    override fun onVisitGraphComplete(@DirType dirType: Int, @Command command: Int) {

    }

    override fun addObject(mediaObject: BaseMediaObject<D>) {
        mediaObjects[mediaObject.uuid] = mediaObject
        mediaObject.onAddedToGraph(this)

        when (mediaObject) {
            is BaseMediaSource<D> -> mediaSource = mediaObject
            is BaseMediaSink<D> -> mediaSink = mediaObject
        }
    }

    override fun removeObject(mediaObject: BaseMediaObject<D>) {
        mediaObjects.remove(mediaObject.uuid)
        mediaObject.onRemovedFromGraph(this)
    }

    override fun findObject(uuid: String): BaseMediaObject<D>? {
        return mediaObjects[uuid]
    }

    override suspend fun prepare(@DirType dirType: Int) {
        if (state >= GraphState.PREPARED) {
            Log.d(tagOfGraph, "prepare graph when state is $state")
            return
        }
        eglResource.prepare()
        doVisit(dirType, Command.COMMAND_PREPARE, this::onVisitGraphSuccess, this::onVisitGraphError)
        state = GraphState.PREPARED
    }

    override suspend fun start(@DirType dirType: Int) {
        if (state >= GraphState.STARTED) {
            Log.d(tagOfGraph, "start graph when state is $state")
            return
        }
        doVisit(dirType, Command.COMMAND_START, this::onVisitGraphSuccess, this::onVisitGraphError)
        state = GraphState.STARTED
    }

    override suspend fun beginFlush(@DirType dirType: Int) {
        if (state != GraphState.STARTED) {
            Log.d(tagOfGraph, "beginFlush when state is $state")
            return
        }
        doVisit(dirType, Command.COMMAND_BEGIN_FLUSH, this::onVisitGraphSuccess, this::onVisitGraphError)
        state = GraphState.FLUSHING
    }

    override suspend fun endFlush(@DirType dirType: Int) {
        if (state != GraphState.FLUSHING) {
            Log.d(tagOfGraph, "endFlush when state is $state")
            return
        }
        doVisit(dirType, Command.COMMAND_END_FLUSH, this::onVisitGraphSuccess, this::onVisitGraphError)
        state = GraphState.STARTED
    }

    override suspend fun stop(@DirType dirType: Int) {
        if (state >= GraphState.STOPPED) {
            Log.d(tagOfGraph, "stop graph when state is $state")
            return
        }
        doVisit(dirType, Command.COMMAND_STOP, this::onVisitGraphSuccess, this::onVisitGraphError)
        state = GraphState.STOPPED
    }

    override suspend fun release(@DirType dirType: Int) {
        if (state != GraphState.PREPARED && state != GraphState.STOPPED) {
            Log.d(tagOfGraph, "release graph when state is $state")
            return
        }
        doVisit(dirType, Command.COMMAND_RELEASE, this::onVisitGraphSuccess, this::onVisitGraphError)
        eglResource.release()
        state = GraphState.IDLE
    }

    protected open suspend fun doVisit(
        @DirType dirType: Int,
        @Command command: Int,
        onSuccess: VisitGraphSuccess? = this::onVisitGraphSuccess,
        onError: VisitGraphError? = this::onVisitGraphError,
        onComplete: VisitGraphComplete? = this::onVisitGraphComplete
    ) {
        try {
            visitor.visit(dirType, command)
            onSuccess?.invoke(dirType, command)
        } catch (e: MediaException) {
            onError?.invoke(e)
        } catch (t: Throwable) {
            onError?.invoke(MediaException(t.message ?: "Fatal error", t))
        } finally {
            onComplete?.invoke(dirType, command)
        }
    }

    override suspend fun execCommand(mediaObject: IMediaObject<D>, @Command command: Int) {
        when (command) {
            Command.COMMAND_PREPARE -> {
                Log.d(tagOfGraph, "onPrepare ${mediaObject.name}")
                mediaObject.onPrepare()
            }
            Command.COMMAND_START -> {
                Log.d(tagOfGraph, "onStart ${mediaObject.name}")
                mediaObject.onStart()
            }
            Command.COMMAND_BEGIN_FLUSH -> {
                Log.d(tagOfGraph, "onBeginFlush ${mediaObject.name}")
                mediaObject.onBeginFlush()
            }
            Command.COMMAND_END_FLUSH -> {
                Log.d(tagOfGraph, "onEndFlush ${mediaObject.name}")
                mediaObject.onEndFlush()
            }
            Command.COMMAND_STOP -> {
                Log.d(tagOfGraph, "onStop ${mediaObject.name}")
                mediaObject.onStop()
            }
            Command.COMMAND_RELEASE -> {
                Log.d(tagOfGraph, "onRelease ${mediaObject.name}")
                mediaObject.onRelease()
            }
        }
    }


    /**
     * Implementation of LifecycleOwner
     */
    private val lifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = lifecycleRegistry
}

