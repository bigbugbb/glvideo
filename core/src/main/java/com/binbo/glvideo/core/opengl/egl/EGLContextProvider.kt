package com.binbo.glvideo.core.opengl.egl

import android.opengl.EGLContext

/**
 * @author bigbug
 * @project lobby
 * @date 2022/11/14
 * @time 14:39
 */
interface IEGLContextProvider {
    var eglSurface: EGLSurfaceHolder?

    fun attachEGLContext(shareContext: EGLContext?, vw: Int, vh: Int)
    fun detachEGLContext()
}

class EGLContextProvider : IEGLContextProvider {

    override var eglSurface: EGLSurfaceHolder? = null

    /**
     * create EGL context for GL operations
     */
    override fun attachEGLContext(shareContext: EGLContext?, vw: Int, vh: Int) {
        if (eglSurface == null) {
            eglSurface = EGLSurfaceHolder()
            eglSurface?.init(shareContext, EGL_RECORDABLE_ANDROID)
            eglSurface?.createEGLSurface(null, vw, vh)
            eglSurface?.makeCurrent()
        }
    }

    override fun detachEGLContext() {
        eglSurface?.destroyEGLSurface()
        eglSurface?.release()
        eglSurface = null
    }
}