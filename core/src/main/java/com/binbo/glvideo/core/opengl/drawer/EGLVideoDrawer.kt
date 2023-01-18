package com.binbo.glvideo.core.opengl.drawer

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/21
 * @time 14:12
 */
open class EGLVideoDrawer(
    val videoWidth: Int,
    val videoHeight: Int,
    val viewportWidth: Int,
    val viewportHeight: Int,
    val mode: Int // 0: fit, 1: stretch
) {

    // 顶点坐标
    private val vertexCoords = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    // 纹理坐标
    private val textureCoords = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    var textureId: Int = -1
    var surfaceTexture: SurfaceTexture? = null

    // OpenGL程序ID
    private var programId: Int = -1

    // 矩阵变换接收者
    private var vertexMatrixHandler: Int = -1

    // 顶点坐标接收者
    private var vertexPosHandler: Int = -1

    // 纹理坐标接收者
    private var texturePosHandler: Int = -1

    // 纹理接收者
    private var textureHandler: Int = -1

    // 半透值接收者
    private var alphaHandler: Int = -1

    private var vertexBuffer: FloatBuffer
    private var textureBuffer: FloatBuffer

    private var widthRatio = 1f
    private var heightRatio = 1f

    private var matrix: FloatArray? = null

    private var alpha = 1f

    private var rotation: Int = 0

    init {
        //【步骤1: 初始化顶点坐标】
        val bb = ByteBuffer.allocateDirect(vertexCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        //将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(vertexCoords)
        vertexBuffer.position(0)

        val cc = ByteBuffer.allocateDirect(textureCoords.size * 4)
        cc.order(ByteOrder.nativeOrder())
        textureBuffer = cc.asFloatBuffer()
        textureBuffer.put(textureCoords)
        textureBuffer.position(0)
    }

    private fun initDefMatrix() {
        if (matrix != null) return
        if (videoWidth > 0 && videoHeight > 0 && viewportWidth > 0 && viewportHeight > 0) {
            matrix = FloatArray(16)
            var projectionMatrix = FloatArray(16)
            val originRatio = videoWidth / videoHeight.toFloat()
            val worldRatio = viewportWidth / viewportHeight.toFloat()
            if (viewportWidth > viewportHeight) {
                if (originRatio > worldRatio) {
                    heightRatio = originRatio / worldRatio
                    Matrix.orthoM(projectionMatrix, 0, -widthRatio, widthRatio, -heightRatio, heightRatio, 3f, 5f)
                } else {// 原始比例小于窗口比例，缩放高度度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    widthRatio = worldRatio / originRatio
                    Matrix.orthoM(projectionMatrix, 0, -widthRatio, widthRatio, -heightRatio, heightRatio, 3f, 5f)
                }
            } else {
                if (originRatio > worldRatio) {
                    when (mode) {
                        0 -> heightRatio = originRatio / worldRatio
                        1 -> widthRatio = worldRatio / originRatio
                    }
                    Matrix.orthoM(projectionMatrix, 0, -widthRatio, widthRatio, -heightRatio, heightRatio, 3f, 5f)
                } else {// 原始比例小于窗口比例，缩放高度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    when (mode) {
                        0 -> widthRatio = worldRatio / originRatio
                        1 -> heightRatio = originRatio / worldRatio
                    }
                    Matrix.orthoM(projectionMatrix, 0, -widthRatio, widthRatio, -heightRatio, heightRatio, 3f, 5f)
                }
            }

            // 设置相机位置
            val modelMatrix = FloatArray(16)
            Matrix.setIdentityM(modelMatrix, 0)
            if (rotation != 0) {
                Matrix.rotateM(modelMatrix, 0, -rotation.toFloat(), 0f, 0f, 1f)
            }

            val viewMatrix = FloatArray(16)
            Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5.0f, 0f, 0f, 0f, 0f, 1.0f, 0f)
            // 计算变换矩阵
            Matrix.multiplyMM(matrix, 0, projectionMatrix, 0, viewMatrix, 0)
            Matrix.multiplyMM(matrix, 0, matrix, 0, modelMatrix, 0)
        }
    }

    fun setAlpha(alpha: Float) {
        this.alpha = alpha
    }

    fun setRotation(rotation: Int) {
        this.rotation = rotation
    }

    fun draw() {
        if (textureId > 0) {
            initDefMatrix()
            // 创建、编译并启动OpenGL着色器
            createShaderProgram()
            // 激活并绑定纹理单元
            activateTexture()
            // 绑定图片到纹理单元
            updateTexture()
            // 开始渲染绘制
            doDraw()
        }
    }

    private fun createShaderProgram() {
        if (programId == -1) {
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, getVertexShader())
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, getFragmentShader())

            //创建OpenGL ES程序，注意：需要在OpenGL渲染线程中创建，否则无法渲染
            programId = GLES20.glCreateProgram()
            //将顶点着色器加入到程序
            GLES20.glAttachShader(programId, vertexShader)
            //将片元着色器加入到程序中
            GLES20.glAttachShader(programId, fragmentShader)
            //连接到着色器程序
            GLES20.glLinkProgram(programId)

            vertexMatrixHandler = GLES20.glGetUniformLocation(programId, "uMatrix")
            vertexPosHandler = GLES20.glGetAttribLocation(programId, "aPosition")
            textureHandler = GLES20.glGetUniformLocation(programId, "uTexture")
            texturePosHandler = GLES20.glGetAttribLocation(programId, "aCoordinate")
            alphaHandler = GLES20.glGetAttribLocation(programId, "alpha")
        }
        //使用OpenGL程序
        GLES20.glUseProgram(programId)
    }

    private fun activateTexture() {
        //激活指定纹理单元
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        //绑定纹理ID到纹理单元
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        //将激活的纹理单元传递到着色器里面
        GLES20.glUniform1i(textureHandler, 0)
        //配置边缘过渡参数
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
    }

    private fun updateTexture() {
        surfaceTexture?.updateTexImage()
    }

    private fun doDraw() {
        //启用顶点的句柄
        GLES20.glEnableVertexAttribArray(vertexPosHandler)
        GLES20.glEnableVertexAttribArray(texturePosHandler)
        GLES20.glUniformMatrix4fv(vertexMatrixHandler, 1, false, matrix, 0)
        //设置着色器参数， 第二个参数表示一个顶点包含的数据数量，这里为xy，所以为2
        GLES20.glVertexAttribPointer(vertexPosHandler, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glVertexAttribPointer(texturePosHandler, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
        GLES20.glVertexAttrib1f(alphaHandler, alpha)
        //开始绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    fun release() {
        GLES20.glDisableVertexAttribArray(vertexPosHandler)
        GLES20.glDisableVertexAttribArray(texturePosHandler)
        GLES20.glDeleteProgram(programId)
        programId = -1
    }

    private fun getVertexShader() = """
        attribute vec4 aPosition;
        precision mediump float;
        uniform mat4 uMatrix;
        attribute vec2 aCoordinate;
        varying vec2 vCoordinate;
        attribute float alpha;
        varying float inAlpha;
        void main() {
            gl_Position = uMatrix*aPosition;
            vCoordinate = aCoordinate;
            inAlpha = alpha;
        }
    """

    private fun getFragmentShader() = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vCoordinate;
        varying float inAlpha;
        uniform samplerExternalOES uTexture;
        void main() {
            vec4 color = texture2D(uTexture, vCoordinate);
            gl_FragColor = vec4(color.r, color.g, color.b, inAlpha);
        }
    """

    private fun loadShader(type: Int, shaderCode: String): Int {
        //根据type创建顶点着色器或者片元着色器
        val shader = GLES20.glCreateShader(type)
        //将资源加入到着色器中，并编译
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}