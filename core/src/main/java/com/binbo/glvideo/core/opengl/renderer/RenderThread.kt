package com.binbo.glvideo.core.opengl.renderer

import android.opengl.EGLContext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.binbo.glvideo.core.opengl.egl.EGLSurfaceHolder
import com.binbo.glvideo.core.opengl.egl.EGL_RECORDABLE_ANDROID


class RenderThread : Thread() {

    // 渲染状态
    private var state = RenderState.NO_SURFACE

    private var eglSurface: EGLSurfaceHolder? = null

    // 是否绑定了EGLSurface
    private var haveBindEGLContext = false

    // 是否已经新建过EGL上下文，用于判断是否需要生产新的纹理ID
    private var neverCreateEglContext = true

    private var width = 0
    private var height = 0

    private val waitLock = Object()

    private var curTimestamp = 0L
    private var lastTimestamp = 0L

    private var eventQueue = ArrayList<Runnable>()

    private var renderer: DefaultGLRenderer? = null
    private var renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    private var sharedContext: EGLContext? = null
    private var waitingTime: Long = 16L

    fun setRenderer(renderer: DefaultGLRenderer?) {
        this.renderer = renderer
    }

    fun setSharedContext(sharedContext: EGLContext?) {
        this.sharedContext = sharedContext
    }

    fun setRenderWaitingTime(waitingTime: Long) {
        this.waitingTime = waitingTime
    }

    private fun holdOn() {
        synchronized(waitLock) {
            waitLock.wait()
        }
    }

    private fun notifyGo() {
        synchronized(waitLock) {
            waitLock.notify()
        }
    }

    fun setRenderMode(mode: Int) {
        renderMode = mode
    }

    fun onSurfaceCreate() {
        state = RenderState.FRESH_SURFACE
        notifyGo()
    }

    fun onSurfaceChange(width: Int, height: Int) {
        this.width = width
        this.height = height
        state = RenderState.SURFACE_CHANGE
        notifyGo()
    }

    fun onSurfaceDestroy() {
        state = RenderState.SURFACE_DESTROY
        notifyGo()
    }

    fun onSurfaceStop() {
        state = RenderState.STOP
        notifyGo()
    }

    fun notifySwap(timeUs: Long) {
        synchronized(curTimestamp) {
            curTimestamp = timeUs
        }
        notifyGo()
    }

    fun queueEvent(event: Runnable) {
        synchronized(waitLock) {
            eventQueue.add(event)
        }
    }

    fun consumeEvent(): Runnable? {
        return synchronized(waitLock) {
            if (eventQueue.isNotEmpty()) {
                eventQueue.removeAt(0)?.apply { run() }
            } else {
                null
            }
        }
    }

    override fun run() {
        initEGL(sharedContext)
        while (true) {
            when (state) {
                RenderState.FRESH_SURFACE -> {
                    createEGLSurfaceFirst()
                    holdOn()
                }
                RenderState.SURFACE_CHANGE -> {
                    createEGLSurfaceFirst()
                    GLES20.glViewport(0, 0, width, height)
                    renderer?.onSurfaceChanged(null, width, height)
                    state = RenderState.RENDERING
                }
                RenderState.RENDERING -> {
                    var event: Runnable? = consumeEvent()
                    if (event != null) {
                        continue
                    }

                    render()

                    if (renderMode == GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
                        holdOn()
                    }
                }
                RenderState.SURFACE_DESTROY -> {
                    clearEventQueue()
                    renderer?.onSurfaceDestroyed()
                    destroyEGLSurface()
                    state = RenderState.NO_SURFACE
                }
                RenderState.STOP -> {
                    clearEventQueue()
                    renderer?.onSurfaceDestroyed()
                    renderer = null
                    destroyEGLSurface()
                    releaseEGL()
                    return
                }
                else -> {
                    holdOn()
                }
            }

            if (renderMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
                sleep(waitingTime)
            }
        }
    }

    private fun initEGL(sharedContext: EGLContext? = null) {
        eglSurface = EGLSurfaceHolder()
        eglSurface?.init(sharedContext, EGL_RECORDABLE_ANDROID)
    }

    private fun createEGLSurfaceFirst() {
        if (!haveBindEGLContext) {
            haveBindEGLContext = true
            createEGLSurface()
            if (neverCreateEglContext) {
                neverCreateEglContext = false
                GLES20.glClearColor(0f, 0f, 0f, 0f)
                // 开启混合，即半透明
                GLES20.glEnable(GLES20.GL_BLEND)
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            }
            renderer?.onSurfaceCreated(null, null)
        }
    }

    private fun createEGLSurface() {
        eglSurface?.createEGLSurface(renderer?.surface)
        eglSurface?.makeCurrent()
    }

    private fun clearEventQueue() {
        synchronized(waitLock) {
            eventQueue.clear()
        }
    }

    private fun render() {
        val render = if (renderMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
            true
        } else {
            synchronized(curTimestamp) {
                if (curTimestamp > lastTimestamp) {
                    lastTimestamp = curTimestamp
                    true
                } else {
                    false
                }
            }
        }

        if (render) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            renderer?.onDrawFrame(null)
            eglSurface?.setTimestamp(curTimestamp)
            eglSurface?.swapBuffers()
        }
    }

    private fun destroyEGLSurface() {
        eglSurface?.destroyEGLSurface()
        haveBindEGLContext = false
    }

    private fun releaseEGL() {
        eglSurface?.release()
    }

    /**
     * 渲染状态
     */
    enum class RenderState {
        NO_SURFACE, //没有有效的surface
        FRESH_SURFACE, //持有一个未初始化的新的surface
        SURFACE_CHANGE, //surface尺寸变化
        RENDERING, //初始化完毕，可以开始渲染
        SURFACE_DESTROY, //surface销毁
        STOP, //停止绘制
        RELEASE // 强制退出
    }
}