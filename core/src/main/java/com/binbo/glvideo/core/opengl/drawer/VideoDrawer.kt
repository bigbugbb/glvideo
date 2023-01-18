package com.binbo.glvideo.core.opengl.drawer

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


open class VideoDrawer(var videoWidth: Int = -1, var videoHeight: Int = -1) : Drawer() {

    // 顶点坐标
    private val vertexCoors = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    // 纹理坐标
    private val textureCoors = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    protected var textureId = -1

    protected var surfaceTexture: SurfaceTexture? = null

    protected var listener: SurfaceTextureAvailableListener? = null

    // OpenGL程序ID
    protected var program: Int = -1

    // 矩阵变换接收者
    protected var vertexMatrixHandler: Int = -1

    // 顶点坐标接收者
    protected var vertexPosHandler: Int = -1

    // 纹理坐标接收者
    protected var texturePosHandler: Int = -1

    // 纹理接收者
    protected var textureHandler: Int = -1

    // 半透值接收者
    protected var alphaHandler: Int = -1

    protected lateinit var vertexBuffer: FloatBuffer
    protected lateinit var textureBuffer: FloatBuffer

    protected var widthRatio = 1f
    protected var heightRatio = 1f

    protected var matrix: FloatArray? = null

    protected var alphaValue = 1f

    init {
        //【步骤1: 初始化顶点坐标】
        initPos()
    }

    private fun initPos() {
        val bb = ByteBuffer.allocateDirect(vertexCoors.size * 4)
        bb.order(ByteOrder.nativeOrder())
        //将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(vertexCoors)
        vertexBuffer.position(0)

        val cc = ByteBuffer.allocateDirect(textureCoors.size * 4)
        cc.order(ByteOrder.nativeOrder())
        textureBuffer = cc.asFloatBuffer()
        textureBuffer.put(textureCoors)
        textureBuffer.position(0)
    }

    protected open fun initDefMatrix() {
        if (matrix != null) return
        if (videoWidth != -1 && videoHeight != -1 && viewportWidth != -1 && viewportHeight != -1) {
            matrix = FloatArray(16)
            var prjMatrix = FloatArray(16)
            val originRatio = videoWidth / videoHeight.toFloat()
            val worldRatio = viewportWidth / viewportHeight.toFloat()
            if (viewportWidth > viewportHeight) {
                if (originRatio > worldRatio) {
                    heightRatio = originRatio / worldRatio
                    Matrix.orthoM(
                        prjMatrix, 0,
                        -widthRatio, widthRatio,
                        -heightRatio, heightRatio,
                        3f, 5f
                    )
                } else {// 原始比例小于窗口比例，缩放高度度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    widthRatio = worldRatio / originRatio
                    Matrix.orthoM(
                        prjMatrix, 0,
                        -widthRatio, widthRatio,
                        -heightRatio, heightRatio,
                        3f, 5f
                    )
                }
            } else {
                if (originRatio > worldRatio) {
                    heightRatio = originRatio / worldRatio
                    Matrix.orthoM(
                        prjMatrix, 0,
                        -widthRatio, widthRatio,
                        -heightRatio, heightRatio,
                        3f, 5f
                    )
                } else {// 原始比例小于窗口比例，缩放高度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    widthRatio = worldRatio / originRatio
                    Matrix.orthoM(
                        prjMatrix, 0,
                        -widthRatio, widthRatio,
                        -heightRatio, heightRatio,
                        3f, 5f
                    )
                }
            }

            // 设置相机位置
            val viewMatrix = FloatArray(16)
            Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5.0f, 0f, 0f, 0f, 0f, 1.0f, 0f)
            // 计算变换矩阵
            Matrix.multiplyMM(matrix, 0, prjMatrix, 0, viewMatrix, 0)
        }
    }

    override fun onWorldCreated() {
        if (textureId == -1) {
            val textures = intArrayOf(-1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]
            setTextureID(textureId)
        }
    }

    override fun setAlpha(alpha: Float) {
        alphaValue = alpha
    }

    override fun setTextureID(texId: Int) {
        if (texId != -1) {
            surfaceTexture?.release()
            surfaceTexture = SurfaceTexture(texId).apply {
                listener?.onSurfaceTextureAvailable(this)
            }
        }
    }

    override fun setSurfaceTextureAvailableListener(listener: SurfaceTextureAvailableListener?) {
        this.listener = listener
    }

    override fun draw() {
        if (textureId != -1) {
            initDefMatrix()
            //【步骤2: 创建、编译并启动OpenGL着色器】
            createGLPrg()
            //【步骤3: 激活并绑定纹理单元】
            activateTexture()
            //【步骤4: 绑定图片到纹理单元】
            updateTexture()
            //【步骤5: 开始渲染绘制】
            doDraw()
        }
    }

    private fun createGLPrg() {
        if (program == -1) {
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, getVertexShader())
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, getFragmentShader())

            //创建OpenGL ES程序，注意：需要在OpenGL渲染线程中创建，否则无法渲染
            program = GLES20.glCreateProgram()
            //将顶点着色器加入到程序
            GLES20.glAttachShader(program, vertexShader)
            //将片元着色器加入到程序中
            GLES20.glAttachShader(program, fragmentShader)
            //连接到着色器程序
            GLES20.glLinkProgram(program)

            vertexMatrixHandler = GLES20.glGetUniformLocation(program, "uMatrix")
            vertexPosHandler = GLES20.glGetAttribLocation(program, "aPosition")
            textureHandler = GLES20.glGetUniformLocation(program, "uTexture")
            texturePosHandler = GLES20.glGetAttribLocation(program, "aCoordinate")
            alphaHandler = GLES20.glGetAttribLocation(program, "alpha")
        }
        //使用OpenGL程序
        GLES20.glUseProgram(program)
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
        GLES20.glVertexAttrib1f(alphaHandler, alphaValue)
        //开始绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    override fun release() {
        GLES20.glDisableVertexAttribArray(vertexPosHandler)
        GLES20.glDisableVertexAttribArray(texturePosHandler)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        GLES20.glDeleteProgram(program)
        surfaceTexture?.release()
        surfaceTexture = null
        textureId = -1
        program = -1
    }

    private fun getVertexShader(): String {
        return "attribute vec4 aPosition;" +
                "precision mediump float;" +
                "uniform mat4 uMatrix;" +
                "attribute vec2 aCoordinate;" +
                "varying vec2 vCoordinate;" +
                "attribute float alpha;" +
                "varying float inAlpha;" +
                "void main() {" +
                "    gl_Position = uMatrix*aPosition;" +
                "    vCoordinate = aCoordinate;" +
                "    inAlpha = alpha;" +
                "}"
    }

    private fun getFragmentShader(): String {
        //一定要加换行"\n"，否则会和下一行的precision混在一起，导致编译出错
        return "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;" +
                "varying vec2 vCoordinate;" +
                "varying float inAlpha;" +
                "uniform samplerExternalOES uTexture;" +
                "void main() {" +
                "  vec4 color = texture2D(uTexture, vCoordinate);" +
                "  gl_FragColor = vec4(color.r, color.g, color.b, inAlpha);" +
                "}"
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        //根据type创建顶点着色器或者片元着色器
        val shader = GLES20.glCreateShader(type)
        //将资源加入到着色器中，并编译
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        return shader
    }

    fun translate(dx: Float, dy: Float) {
        Matrix.translateM(matrix, 0, dx * widthRatio * 2, -dy * heightRatio * 2, 0f)
    }

    fun scale(sx: Float, sy: Float) {
        Matrix.scaleM(matrix, 0, sx, sy, 1f)
        widthRatio /= sx
        heightRatio /= sy
    }
}