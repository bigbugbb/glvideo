package com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.drawer

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.ArrayMap
import com.binbo.glvideo.core.opengl.drawer.Drawer
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram
import com.binbo.glvideo.core.opengl.utils.MatrixHelper
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.ext.createBlurredElementBitmap
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.objects.BlurredGreen
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.objects.BlurredPink
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.objects.BlurredPurple

class BlurredElementDrawer : Drawer() {

    private var blurredPink: BlurredPink? = null
    private var blurredPurple: BlurredPurple? = null
    private var blurredGreen: BlurredGreen? = null

    private var textureProgram: TextureShaderProgram? = null

    private var texturesMap = ArrayMap<Int, Int>(3)

    private val textures: IntArray
        get() = texturesMap.values.toIntArray()

    private val pinkX: Float
        get() = blurredPink?.x ?: 0f

    private val pinkY: Float
        get() = blurredPink?.y ?: 0f

    private val purpleX: Float
        get() = blurredPurple?.x ?: 0f

    private val purpleY: Float
        get() = blurredPurple?.y ?: 0f

    private val greenX: Float
        get() = blurredGreen?.x ?: 0f

    private val greenY: Float
        get() = blurredGreen?.y ?: 0f


    init {
        Matrix.setIdentityM(viewProjectionMatrix, 0)
    }

    private fun positionPinkElementInScene() {
        // 90 degrees to lie flat on the XZ plane.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, pinkX, pinkY, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    private fun positionPurpleElementInScene() {
        // 90 degrees to lie flat on the XZ plane.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, purpleX, purpleY, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    private fun positionGreenElementInScene() {
        // 90 degrees to lie flat on the XZ plane.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, greenX, greenY, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    override fun onWorldCreated() {
        createBlurredElementBitmap(R.layout.layout_blurred_pink)?.let {
            texturesMap[pinkElementId] = OpenGLUtils.loadTexture(context, it)
            blurredPink = BlurredPink()
        }

        createBlurredElementBitmap(R.layout.layout_blurred_purple)?.let {
            texturesMap[purpleElementId] = OpenGLUtils.loadTexture(context, it)
            blurredPurple = BlurredPurple()
        }

        createBlurredElementBitmap(R.layout.layout_blurred_green)?.let {
            texturesMap[greenElementId] = OpenGLUtils.loadTexture(context, it)
            blurredGreen = BlurredGreen()
        }

        textureProgram = TextureShaderProgram(context)
    }

    override fun setViewportSize(width: Int, height: Int) {
        super.setViewportSize(width, height)
        // Set the OpenGL viewport to fill the entire surface.
        GLES20.glViewport(0, 0, width, height)
        MatrixHelper.perspectiveM(projectionMatrix, 45f, width.toFloat() / height.toFloat(), 1f, 10f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 4f, 0f, 0f, 0f, 0f, 1f, 0f)
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
        texturesMap[pinkElementId]?.takeIf { it != 0 }?.let { elementTextureId ->
            positionPinkElementInScene()
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, elementTextureId, 1f)
            blurredPink?.update()
            blurredPink?.bindData(textureProgram)
            blurredPink?.draw()
        }

        // Draw the purple element
        texturesMap[purpleElementId]?.takeIf { it != 0 }?.let { elementTextureId ->
            positionPurpleElementInScene()
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, elementTextureId, 1f)
            blurredPurple?.update()
            blurredPurple?.bindData(textureProgram)
            blurredPurple?.draw()
        }

        // Draw the green element
        texturesMap[greenElementId]?.takeIf { it != 0 }?.let { elementTextureId ->
            positionGreenElementInScene()
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, elementTextureId, 1f)
            blurredGreen?.update()
            blurredGreen?.bindData(textureProgram)
            blurredGreen?.draw()
        }

        GLES20.glDisable(GLES20.GL_BLEND)
    }

    companion object {
        const val TAG = "BlurredElementDrawer"

        const val pinkElementId = 1
        const val purpleElementId = 2
        const val greenElementId = 3
    }
}