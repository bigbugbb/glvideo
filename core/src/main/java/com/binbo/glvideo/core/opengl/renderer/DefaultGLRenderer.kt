package com.binbo.glvideo.core.opengl.renderer

import android.opengl.EGLContext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.ArrayMap
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import com.binbo.glvideo.core.opengl.drawer.Drawer
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


abstract class DefaultGLRenderer : SurfaceHolder.Callback, GLSurfaceView.Renderer {

    var width: Int = 0
        private set

    var height: Int = 0
        private set

    var isReady: Boolean = false
        private set

    var surfaceSize = Size(0, 0)
        private set

    val drawers = ArrayMap<Class<out Drawer>, Drawer>()

    private var useCustomThread: Boolean = false
    private var renderThread: RenderThread? = null

    internal var surface: Surface? = null

    private var surfaceView: WeakReference<SurfaceView>? = null

    protected abstract var impl: RenderImpl

    override fun onSurfaceCreated(glUnused: GL10?, config: EGLConfig?) {
        // 开启混合，即半透明
        GLES20.glEnable(GLES20.GL_BLEND)
//        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA) // 华为手机上开这个会出问题
        drawers.values.forEach {
            it.release()
            it.onWorldCreated()
        }
        impl.onSurfaceDestroy()
        impl.onSurfaceCreate()
    }

    override fun onSurfaceChanged(glUnused: GL10?, width: Int, height: Int) {
        // 设置窗口大小
        GLES20.glViewport(0, 0, width, height)

        this.width = width
        this.height = height
        this.isReady = true
        this.surfaceSize = Size(width, height)

        drawers.values.forEach {
            it.setViewportSize(width, height)
        }
        impl.onSurfaceChange(width, height)
    }

    override fun onDrawFrame(glUnused: GL10?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        impl.onDrawFrame()
    }

    open fun onSurfaceDestroyed() {
        this.isReady = false
        drawers.values.forEach {
            it.release()
        }
        impl.onSurfaceDestroy()
    }

    fun addDrawer(drawer: Drawer) {
        drawers[drawer::class.java] = drawer
    }

    fun getDrawer(clazz: Class<out Drawer>) = drawers[clazz]

    open fun onTouchPress(normalizedX: Float, normalizedY: Float) {}

    open fun onTouchDragged(normalizedX: Float, normalizedY: Float) {}

    open fun onTouchRelease(normalizedX: Float, normalizedY: Float) {}

    open fun onScroll(normalizedDistanceX: Float, normalizedDistanceY: Float) {}

    open fun onFling(normalizedDistanceX: Float, normalizedDistanceY: Float) {}

    open fun onScale(scaleFactor: Float) {}

    fun queueEvent(event: Runnable) {
        renderThread?.queueEvent(event)
    }

    /**
     * The following methods apply to custom render thread.
     * With useCustomThread open, the renderer does not depend on GLSurfaceView.
     * 包含EGL的初始化，线程与OpenGL上下文绑定，渲染循环，资源销毁等
     */

    fun setUseCustomRenderThread(useCustom: Boolean, sharedContext: EGLContext? = null) {
        require(renderThread == null)
        useCustomThread = useCustom
        renderThread = RenderThread().apply {
            setRenderer(this@DefaultGLRenderer)
            setSharedContext(sharedContext)
            start()
        }
    }

    fun setSurface(surface: SurfaceView) {
        surfaceView = WeakReference(surface)
        surfaceSize = Size(surface.width, surface.height)
        surface.holder.addCallback(this)

        surface.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View) {
                stop()
            }

            override fun onViewAttachedToWindow(v: View) {
            }
        })
    }

    fun setSurface(surface: Surface?, width: Int, height: Int) {
        this.surface = surface
        surfaceSize = Size(width, height)
        renderThread?.onSurfaceChange(width, height)
    }

    fun setRenderMode(mode: Int) {
        renderThread?.setRenderMode(mode)
    }

    fun setRenderWaitingTime(waitingTime: Long) {
        renderThread?.setRenderWaitingTime(waitingTime)
    }

    fun notifySwap(timeUs: Long) {
        renderThread?.notifySwap(timeUs)
    }

    fun stop() {
        renderThread?.onSurfaceStop()
        surface = null
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surface = holder.surface
        setSurface(holder.surface, surfaceSize.width, surfaceSize.height)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surfaceSize = Size(width, height)
        renderThread?.onSurfaceChange(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        renderThread?.onSurfaceDestroy()
    }
}