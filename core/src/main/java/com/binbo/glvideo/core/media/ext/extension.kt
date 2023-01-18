package com.binbo.glvideo.core.media.ext

import android.opengl.EGL14
import com.binbo.glvideo.core.media.config.EncoderSurfaceRenderConfig
import com.binbo.glvideo.core.media.encoder.MediaVideoEncoder

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/14
 * @time 11:21
 */
fun setupEncoderSurfaceRender(encoder: MediaVideoEncoder?, width: Int = 1080, height: Int = 1920) {
    encoder?.let {
        EGL14.eglGetCurrentContext()?.let { eglContext ->
            if (it.recorderGLRender.eglContext != eglContext) {
                it.setupSurfaceRender(EncoderSurfaceRenderConfig(eglContext, width = width, height = height))
            }
        }
    }
}