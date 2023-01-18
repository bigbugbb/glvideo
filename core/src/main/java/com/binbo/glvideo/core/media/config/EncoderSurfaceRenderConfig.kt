package com.binbo.glvideo.core.media.config

import android.opengl.EGL14
import android.opengl.EGLContext

data class EncoderSurfaceRenderConfig(
    var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT, // 这个eglContext来自GLThread的eglContext，用于在不同GLThread共享资源
    var width: Int = 1080,
    var height: Int = 1920,
    var frameRate: Int = 25,
    var totalFrames: Int = Int.MAX_VALUE
)