package com.binbo.glvideo.sample_app.impl.advanced.namecard.drawer

/**
 * @author bigbug
 * @project lobby
 * @date 2022/11/28
 * @time 16:54
 */
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.ArrayMap
import com.binbo.glvideo.core.opengl.drawer.Drawer
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram
import com.binbo.glvideo.core.opengl.utils.MatrixHelper
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.App.Const.frameRate
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardBottom
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardLeft
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.watermarkHeight
import com.binbo.glvideo.sample_app.impl.advanced.namecard.ext.createWatermarkBitmap
import com.binbo.glvideo.sample_app.impl.advanced.namecard.objects.Watermark

/**
 * @author bigbug
 * @project lobby
 * @date 2022/11/9
 * @time 16:10
 */
class WatermarkDrawer : Drawer() {

    private var watermark: Watermark? = null

    private var textureProgram: TextureShaderProgram? = null

    private var texturesMap = ArrayMap<Int, Int>(1)

    private var frames = 0

    private val textures: IntArray
        get() = texturesMap.values.toIntArray()

    private val textureAlpha: Float
        get() = when {
            frames < frameRate -> 0f
            frames < frameRate * 1.75 -> (frames - frameRate) / (frameRate * 0.75f) * 1f
            else -> 1f
        }

    init {
        Matrix.setIdentityM(viewProjectionMatrix, 0)
    }

    private fun positionWatermarkInScene() {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, missionCardLeft - 0.1f, missionCardBottom - 0.45f, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    override fun onWorldCreated() {
        createWatermarkBitmap()?.let {
            texturesMap[watermarkId] = OpenGLUtils.loadTexture(context, it)
            watermark = Watermark(watermarkHeight * it.width / it.height.toFloat(), watermarkHeight)
        }

        textureProgram = TextureShaderProgram(context)
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

        texturesMap[watermarkId]?.takeIf { it != 0 }?.let { textureId ->
            positionWatermarkInScene()
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, textureId, textureAlpha)
            watermark?.bindData(textureProgram)
            watermark?.draw()
        }

        frames++

        GLES20.glDisable(GLES20.GL_BLEND)
    }

    override fun setViewportSize(width: Int, height: Int) {
        super.setViewportSize(width, height)
        // Set the OpenGL viewport to fill the entire surface.
        GLES20.glViewport(0, 0, width, height)
        MatrixHelper.perspectiveM(projectionMatrix, 45f, width.toFloat() / height.toFloat(), 1f, 10f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 4f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    companion object {
        const val TAG = "Watermark"

        const val watermarkId = 1
    }
}