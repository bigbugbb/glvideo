package com.binbo.glvideo.core.opengl.drawer

import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.binbo.glvideo.core.GLVideo.Core.context
import com.binbo.glvideo.core.opengl.objects.CameraSource
import com.binbo.glvideo.core.opengl.program.CameraShaderProgram


class CameraDrawer : SourceDrawer() {

    private var cameraProgram: CameraShaderProgram? = null

    private var cameraSource: CameraSource? = null

    override fun onWorldCreated() {
        super.onWorldCreated()
        cameraProgram = CameraShaderProgram(context)
        cameraSource = CameraSource()
    }

    override fun draw() {
        super.draw()

        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(projectionMatrix)

        cameraProgram?.useProgram()
        cameraProgram?.setUniforms(projectionMatrix, sourceTextureId)
        cameraSource?.bindData(cameraProgram)
        cameraSource?.draw()

        // 解绑
        GLES20.glBindTexture(GLES11Ext.GL_SAMPLER_EXTERNAL_OES, 0)
    }

    override fun release() {
        super.release()
        cameraProgram?.deleteProgram()
    }
}