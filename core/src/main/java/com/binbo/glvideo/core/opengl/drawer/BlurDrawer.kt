package com.binbo.glvideo.core.opengl.drawer

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.binbo.glvideo.core.GLVideo.Core.context
import com.binbo.glvideo.core.opengl.objects.Blur
import com.binbo.glvideo.core.opengl.program.BlurShaderProgram
import com.binbo.glvideo.core.utils.DeviceUtil
import kotlin.math.sqrt


class BlurDrawer : Drawer() {

    private var shaderProgram: BlurShaderProgram? = null

    private var horizontalBlur: Blur? = null
    private var verticalBlur: Blur? = null

    private var textureId = 0

    private var dstWidth: Int = 0
    private var dstHeight: Int = 0

    private var blurRadius: Int = 25
    private var sumWeight: Float = 0f

    var drawVertical: Boolean = true

    init {
        Matrix.setIdentityM(projectionMatrix, 0)
    }

    fun setFrameSize(width: Int, height: Int) {
        dstWidth = width
        dstHeight = height
    }

    override fun onWorldCreated() {
        shaderProgram = BlurShaderProgram(context)
        horizontalBlur = Blur()
        verticalBlur = Blur()
        calculateSumWeight()
        Log.i(TAG, "onWorldCreated: 总权重 sumWeight = $sumWeight")
    }

    override fun setViewportSize(width: Int, height: Int) {
        super.setViewportSize(width, height)
    }

    override fun release() {
        shaderProgram?.deleteProgram()
    }

    override fun draw() {
        super.draw()
        shaderProgram?.useProgram()

        if (drawVertical) {
            shaderProgram?.setUniforms(projectionMatrix, textureId, true, 30, 2f / DeviceUtil.getScreenHeight(context), sumWeight)
            verticalBlur?.bindData(shaderProgram)
            verticalBlur?.draw()
        } else {
            shaderProgram?.setUniforms(projectionMatrix, textureId, false, 30, 2f / DeviceUtil.getScreenWidth(context), sumWeight)
            horizontalBlur?.bindData(shaderProgram)
            horizontalBlur?.draw()
        }

        // 解绑
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    override fun setTextureID(textureId: Int) {
        super.setTextureID(textureId)
        this.textureId = textureId
    }

    /**
     * 计算总权重
     */
    private fun calculateSumWeight() {
        if (blurRadius < 1) {
            setSumWeight(0f)
            return
        }
        var sumWeight = 0f
        val sigma = blurRadius / 3f
        for (i in 0 until blurRadius) {
            val weight = (1 / sqrt(2 * Math.PI * sigma * sigma) * Math.exp((-(i * i) / (2 * sigma * sigma)).toDouble())).toFloat()
            sumWeight += weight
            if (i != 0) {
                sumWeight += weight
            }
        }
        setSumWeight(sumWeight)
    }

    private fun setSumWeight(weight: Float) {
        sumWeight = weight
    }

    companion object {
        const val TAG = "BlurDrawer"
    }
}