package com.binbo.glvideo.core.opengl.drawer

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import java.lang.ref.WeakReference

open abstract class SourceDrawer : Drawer() {

    protected var sourceTextureId: Int = -1
    protected var surfaceTexture: SurfaceTexture? = null
    protected var surfaceTextureAvailableListener: WeakReference<SurfaceTextureAvailableListener>? = null

    override fun onWorldCreated() {
        if (sourceTextureId == -1) {
            val textures = intArrayOf(-1)
            GLES20.glGenTextures(1, textures, 0)
            sourceTextureId = textures[0]
            setTextureID(sourceTextureId)
        }
    }

    override fun setTextureID(textureId: Int) {
        if (textureId != -1) {
            surfaceTexture = SurfaceTexture(textureId).apply {
                surfaceTextureAvailableListener?.get()?.onSurfaceTextureAvailable(this)
            }
        }
    }

    override fun setSurfaceTextureAvailableListener(listener: SurfaceTextureAvailableListener?) {
        surfaceTextureAvailableListener = WeakReference<SurfaceTextureAvailableListener>(listener)
    }

    override fun release() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDeleteTextures(1, intArrayOf(sourceTextureId), 0)
        surfaceTexture?.release()
        surfaceTexture = null
        sourceTextureId = -1
    }
}