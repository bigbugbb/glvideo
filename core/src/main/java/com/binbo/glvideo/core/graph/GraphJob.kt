package com.binbo.glvideo.core.graph

import android.util.Log
import com.binbo.glvideo.core.GLVideo.Core.tagOfGraph
import com.binbo.glvideo.core.graph.executor.GraphExecutor
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.core.utils.ObserverHolder
import com.binbo.glvideo.core.utils.ObserverHolderDelegate
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference

class GraphJob(val provider: GraphManagerProvider) : ObserverHolder<GraphJob.EventObserver> by ObserverHolderDelegate() {

    private var jobRef = AtomicReference<Job>()

    private var graphManager: BaseGraphManager? = null

    fun run(): Boolean = kotlin.runCatching {
        if (jobRef.compareAndSet(null, GraphExecutor.coroutineScope.launch(start = CoroutineStart.LAZY) {
                graphManager = provider.onGraphManagerRequested()
                graphManager?.run {
                    createMediaGraph()
                    observers.forEach { it.onCreated(this) }
                    prepare()
                    observers.forEach { it.onPrepared(this) }
                    start()
                    observers.forEach { it.onStarted(this) }
                    waitUntilDone()
                }
            }.also { job ->
                job.invokeOnCompletion { throwable ->
                    kotlin.runCatching {
                        /**
                         * sometimes the job could be cancelled even before it had a chance to run
                         */
                        observers.forEach {
                            if (throwable == null) {
                                it.onSuccess(graphManager)
                            } else {
                                it.onFailed(graphManager, throwable)
                            }
                            it.onCompleted(graphManager)
                        }

                        runBlocking {
                            kotlin.runCatching { // in case the graph hasn't been created
                                graphManager?.run {
                                    stop()
                                    observers.forEach { it.onStopped(this) }
                                    release()
                                    observers.forEach { it.onReleased(this) }
                                    destroyMediaGraph()
                                    observers.forEach { it.onDestroyed(this) }
                                }
                            }
                            graphManager = null
                        }
                    }
                    jobRef.set(null)
                }
            })) {
            jobRef.get()?.start() ?: false
        } else {
            false
        }
    }.getOrDefault(false)

    fun cancel() {
        jobRef.get()?.cancel()
        jobRef.set(null)
    }

    interface GraphManagerProvider {
        fun onGraphManagerRequested(): BaseGraphManager
    }

    interface EventObserver {
        suspend fun onCreated(graphManager: BaseGraphManager) {}

        suspend fun onPrepared(graphManager: BaseGraphManager) {}

        suspend fun onStarted(graphManager: BaseGraphManager) {}

        suspend fun onStopped(graphManager: BaseGraphManager) {}

        suspend fun onReleased(graphManager: BaseGraphManager) {}

        suspend fun onDestroyed(graphManager: BaseGraphManager) {}

        fun onSuccess(graphManager: BaseGraphManager?) {}

        fun onFailed(graphManager: BaseGraphManager?, throwable: Throwable) {
            Log.e(tagOfGraph, throwable.message ?: "")
        }

        fun onCompleted(graphManager: BaseGraphManager?) {}
    }
}