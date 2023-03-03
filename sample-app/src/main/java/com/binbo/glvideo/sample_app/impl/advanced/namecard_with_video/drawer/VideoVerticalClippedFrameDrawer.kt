package com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.drawer

import android.opengl.GLES20
import com.binbo.glvideo.core.opengl.drawer.Drawer
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.cardVideoSize
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.objects.ClippedVideoFrame


class VideoVerticalClippedFrameDrawer : Drawer() {

    private var videoTextureId: Int = -1

    private var clippedVideoFrame: ClippedVideoFrame? = null

    private var textureProgram: TextureShaderProgram? = null

    val clippedWidth: Int
        get() = viewportWidth

    val clippedHeight: Int
        get() = (viewportWidth.toFloat() * cardVideoSize.height / cardVideoSize.width).toInt()

    override fun onWorldCreated() {
        clippedVideoFrame = ClippedVideoFrame(0f, 1f, 0f, 1f)
        textureProgram = TextureShaderProgram(context)
    }

    override fun setViewportSize(width: Int, height: Int) {
        super.setViewportSize(width, height)
        val top = (viewportHeight - clippedHeight) / 2f / viewportHeight
        val bottom = (viewportHeight + clippedHeight) / 2f / viewportHeight
        clippedVideoFrame?.updateClipping(0f, 1f, top, bottom)
    }

    override fun setTextureID(textureId: Int) {
        videoTextureId = textureId
    }

    override fun release() {
        textureProgram?.deleteProgram()
        textureProgram = null
    }

    override fun draw() {
        if (videoTextureId != -1) {
            GLES20.glViewport(0, 0, clippedWidth, clippedHeight)
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, videoTextureId, 1f)
            clippedVideoFrame?.bindData(textureProgram)
            clippedVideoFrame?.draw()
            GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        }
    }
}

