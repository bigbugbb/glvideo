package com.binbo.glvideo.core.opengl.drawer

import android.graphics.SurfaceTexture
import android.opengl.Matrix


interface SurfaceTextureAvailableListener {
    fun onSurfaceTextureAvailable(st: SurfaceTexture)
}

/**
 * 其实drawer跟滤镜没有本质区别，因为项目里一开始画图的需求比较多，所以干脆叫它drawer了。
 */
abstract class Drawer {
    protected val projectionMatrix = FloatArray(16)
    protected val modelMatrix = FloatArray(16)
    protected val viewMatrix = FloatArray(16)
    protected val viewProjectionMatrix = FloatArray(16)
    protected val invertedViewProjectionMatrix = FloatArray(16)
    protected val modelViewProjectionMatrix = FloatArray(16)

    var viewportWidth: Int = 0
        protected set
    var viewportHeight: Int = 0
        protected set

    init {
        Matrix.setIdentityM(modelViewProjectionMatrix, 0)
    }

    open fun onWorldCreated() {}

    open fun setViewportSize(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
    }

    open fun setAlpha(alpha: Float) {}

    open fun draw() {}

    open fun setTextureID(id: Int) {}

    open fun setSurfaceTextureAvailableListener(listener: SurfaceTextureAvailableListener?) {}

    open fun handleTouchPress(normalizedX: Float, normalizedY: Float) {}

    open fun handleTouchDragged(normalizedX: Float, normalizedY: Float) {}

    open fun handleTouchRelease(normalizedX: Float, normalizedY: Float) {}

    open fun release() {}
}