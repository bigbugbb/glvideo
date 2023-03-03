package com.binbo.glvideo.sample_app.impl.advanced.namecard.drawer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.ArrayMap
import com.binbo.glvideo.core.opengl.drawer.Drawer
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.impl.advanced.namecard.objects.Background

/**
 * @author bigbug
 * @project lobby
 * @date 2022/11/8
 * @time 18:31
 */
class BackgroundDrawer : Drawer() {

    private var background: Background? = null

    private var textureProgram: TextureShaderProgram? = null

    private var texturesMap = ArrayMap<Int, Int>(1)

    private val textures: IntArray
        get() = texturesMap.values.toIntArray()

    init {
        Matrix.setIdentityM(viewProjectionMatrix, 0)
    }

    override fun onWorldCreated() {
        createBackgroundBitmap()?.let {
            texturesMap[backgroundId] = OpenGLUtils.loadTexture(context, it)
            background = Background()
        }

        textureProgram = TextureShaderProgram(context)
    }

    override fun setViewportSize(width: Int, height: Int) {
        super.setViewportSize(width, height)
        // Set the OpenGL viewport to fill the entire surface.
//        GLES20.glViewport(0, 0, width, height)
//        MatrixHelper.perspectiveM(projectionMatrix, 45f, width.toFloat() / height.toFloat(), 1f, 10f)
//        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 4f, 0f, 0f, 0f, 0f, 1f, 0f)
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

        // Draw the pink element
        texturesMap[backgroundId]?.takeIf { it != 0 }?.let { textureId ->
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, textureId, 1f)
            background?.bindData(textureProgram)
            background?.draw()
        }

        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun createBackgroundBitmap(): Bitmap? {
        return BitmapFactory.decodeResource(context.resources, R.drawable.ic_mission_card_background)
    }

    companion object {
        const val TAG = "Background"

        const val backgroundId = 1
    }
}