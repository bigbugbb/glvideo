package com.binbo.glvideo.sample_app.impl.video.drawer

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.ArrayMap
import android.view.LayoutInflater
import com.binbo.glvideo.core.ext.getViewBitmap
import com.binbo.glvideo.core.ext.measureAndLayout
import com.binbo.glvideo.core.opengl.drawer.Drawer
import com.binbo.glvideo.core.opengl.objects.Rectangle
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils.loadTexture
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.R

class VideoEndingDrawer : Drawer() {

    private var videoEnding: Rectangle = Rectangle()
    private var textureProgram: TextureShaderProgram? = null

    private var texturesMap = ArrayMap<Int, Int>(1)

    private val textures: IntArray
        get() = texturesMap.values.toIntArray()

    private var bitmapWidth: Int = -1
    private var bitmapHeight: Int = -1

    init {
        Matrix.setIdentityM(viewProjectionMatrix, 0)
    }

    override fun onWorldCreated() {
        super.onWorldCreated()
        textureProgram = TextureShaderProgram(context)
        createEndingBitmap()?.let {
            texturesMap[0] = loadTexture(context, it)
            bitmapWidth = it.width
            bitmapHeight = it.height
        }
    }

    private fun createEndingBitmap(): Bitmap? {
        val rootView = LayoutInflater.from(context).inflate(R.layout.layout_video_ending, null)
        rootView.measureAndLayout()
        return rootView.getViewBitmap()
    }

    override fun release() {
        textureProgram?.deleteProgram()
        textureProgram = null

        // 删除纹理
        val texturesArray = textures
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE)
        GLES20.glDeleteTextures(texturesArray.size, texturesArray, 0)
    }

    override fun draw() {
        super.draw()
        GLES20.glEnable(GLES20.GL_BLEND)

        // Update the viewProjection matrix, and create an inverted matrix for touch picking.
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA) // 绘制文字纹理时用这个混合模式可以避免边缘的黑边

        texturesMap[0]?.takeIf { it != 0 }?.let { textureId ->
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, textureId, 1f)
            videoEnding?.bindData(textureProgram)
            videoEnding?.draw()
        }

        GLES20.glDisable(GLES20.GL_BLEND)
    }
}