package com.binbo.glvideo.core.opengl.egl

import android.opengl.EGLContext
import android.opengl.EGLSurface


class EGLSurfaceHolder {

    private val TAG = "EGLSurfaceHolder"

    private lateinit var eglCore: EGLCore

    private var eglSurface: EGLSurface? = null

    fun init(shareContext: EGLContext? = null, flags: Int) {
        eglCore = EGLCore()
        eglCore.init(shareContext, flags)
    }

    fun createEGLSurface(surface: Any?, width: Int = -1, height: Int = -1) {
        eglSurface = if (surface != null) {
            eglCore.createWindowSurface(surface)
        } else {
            eglCore.createOffscreenSurface(width, height)
        }
    }

    fun getEGLContext(): EGLContext {
        return eglCore.getContext()
    }

    fun makeCurrent() {
        if (eglSurface != null) {
            eglCore.makeCurrent(eglSurface!!)
        }
    }

    fun swapBuffers() {
        if (eglSurface != null) {
            eglCore.swapBuffers(eglSurface!!)
        }
    }

    fun setTimestamp(timeMs: Long) {
        if (eglSurface != null) {
            eglCore.setPresentationTime(eglSurface!!, timeMs * 1000)
        }
    }

    fun destroyEGLSurface() {
        if (eglSurface != null) {
            eglCore.destroySurface(eglSurface!!)
            eglSurface = null
        }
    }

    fun release() {
        eglCore.release()
    }
}