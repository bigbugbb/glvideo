package com.binbo.glvideo.core.graph.executor

import com.binbo.glvideo.core.GLVideo.Core.tagOfGraph
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/5
 * @time 22:54
 */
class GraphExecutor : Executor {

    internal val executorService = Executors.newFixedThreadPool(2, object : ThreadFactory {
        private val threadId = AtomicInteger(0)

        override fun newThread(r: Runnable): Thread {
            return Thread(r).apply {
                name = String.format(Locale.US, tagOfGraph + "graph_%d", threadId.getAndIncrement())
            }
        }
    })

    override fun execute(command: Runnable) {
        executorService.execute(command)
    }

    companion object {
        @Volatile
        private var executor: Executor? = null

        val instance: Executor
            get() {
                executor = executor ?: synchronized(GraphExecutor::class.java) {
                    executor ?: GraphExecutor()
                }
                return executor!!
            }

        val dispatchers: CoroutineDispatcher = (instance as GraphExecutor).executorService.asCoroutineDispatcher()

        val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers)
    }
}