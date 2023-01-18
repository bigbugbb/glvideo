package com.binbo.glvideo.core.opengl.drawer

import android.opengl.GLES20
import android.opengl.Matrix
import com.binbo.glvideo.core.GLVideo.Core.context
import com.binbo.glvideo.core.opengl.objects.Frame
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram


class FrameDrawer : Drawer() {

    private var textureProgram: TextureShaderProgram? = null

    private var frame: Frame? = null

    private var textureId = 0

    private var frameWidth: Int = 0
    private var frameHeight: Int = 0

    private var finalFrameWidth: Int = -1
    private var finalFrameHeight: Int = -1

    private var clipX: Float = 0f
    private var clipY: Float = 0f

    init {
        Matrix.setIdentityM(projectionMatrix, 0)
    }

    fun setFrameSize(width: Int, height: Int) {
        frameWidth = width
        frameHeight = height
        updateClipParams()
    }

    override fun onWorldCreated() {
        textureProgram = TextureShaderProgram(context)
        frame = Frame()
    }

    override fun setViewportSize(width: Int, height: Int) {
        super.setViewportSize(width, height)
        updateClipParams()
    }

    override fun release() {
        textureProgram?.deleteProgram()
    }

    override fun draw() {
        super.draw()

        textureProgram?.useProgram()
        textureProgram?.setUniforms(projectionMatrix, textureId, 1f)
        frame?.updateVertexWithClipping(clipX, clipY)
        frame?.bindData(textureProgram)
        frame?.draw()

        // 解绑
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    override fun setTextureID(textureId: Int) {
        super.setTextureID(textureId)
        this.textureId = textureId
    }

    protected fun updateClipParams() {
        if (viewportWidth == 0 || viewportHeight == 0 || frameWidth == 0 || frameHeight == 0) {
            return
        }
        val viewportRatio = viewportWidth.toFloat() / viewportHeight
        val frameRatio = frameWidth.toFloat() / frameHeight

        if (viewportWidth > viewportHeight) {
            finalFrameWidth = (viewportHeight * frameRatio).toInt()
            finalFrameHeight = viewportHeight

            if (viewportRatio >= frameRatio) {
                clipX = 0f
                clipY = 0f
            } else {
                clipX = (finalFrameWidth - viewportWidth).toFloat() / finalFrameWidth / 2f
                clipY = 0f
            }
        } else { // portrait
            finalFrameWidth = viewportWidth
            finalFrameHeight = (viewportWidth / frameRatio).toInt()

            if (viewportRatio >= frameRatio) {
                clipX = 0f
                clipY = (finalFrameHeight - viewportHeight).toFloat() / finalFrameHeight / 2f
            } else {
                clipX = 0f
                clipY = 0f
            }
        }
    }
}