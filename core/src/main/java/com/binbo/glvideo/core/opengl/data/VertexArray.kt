package com.binbo.glvideo.core.opengl.data

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


class VertexArray(vertexData: FloatArray) {
    private val floatBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertexData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(vertexData)

    fun setVertexAttribPointer(dataOffset: Int, attributeLocation: Int, componentCount: Int, stride: Int) {
        floatBuffer.position(dataOffset)
        GLES20.glVertexAttribPointer(attributeLocation, componentCount, GLES20.GL_FLOAT, false, stride, floatBuffer)
        GLES20.glEnableVertexAttribArray(attributeLocation)
        floatBuffer.position(0)
    }

}