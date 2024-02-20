package com.binbo.glvideo.core.opengl.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES10.GL_RGBA
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLUtils
import java.io.*
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*

object OpenGLUtils {
    private const val TAG = "OpenGLUtils"

    @JvmStatic
    fun createTextureIds(count: Int): IntArray {
        val texture = IntArray(count)
        GLES20.glGenTextures(count, texture, 0) //生成纹理
        return texture
    }

    @JvmStatic
    fun createFBOTexture(width: Int, height: Int): IntArray {
        // 新建纹理ID
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)

        // 绑定纹理ID
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])

        // 根据颜色参数，宽高等信息，为上面的纹理ID，生成一个2D纹理
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)

        // 设置纹理边缘参数
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())

        // 解绑纹理ID
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return textures
    }

    @JvmStatic
    fun createFrameBuffer(): IntArray {
        val fbs = IntArray(1)
        GLES20.glGenFramebuffers(1, fbs, 0)
        return fbs
    }

    @JvmStatic
    fun createFBO(frameBuffers: IntArray, frameBufferTextures: IntArray, width: Int, height: Int) {
        require(frameBuffers.size == frameBufferTextures.size)

        GLES20.glGenFramebuffers(frameBuffers.size, frameBuffers, 0)
        glGenTextures(frameBufferTextures)

        frameBuffers.indices.forEach { i ->
            // 绑定fbo纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTextures[i])
            // 创建一个2d的图像  目标 2d纹理+等级 + 格式 +宽、高+ 格式 + 数据类型(byte) + 像素数据
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)

            // 让fbo与纹理绑定起来，后续的操作就是在操作fbo与这个纹理上了
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[i])
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, frameBufferTextures[i], 0)

            // 解绑
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        }
    }

    @JvmStatic
    fun bindFBO(fb: Int, textureId: Int) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureId, 0)
    }

    @JvmStatic
    fun unbindFBO() {
        // 这里的 GLES20.GL_NONE 其实就是 0 ，也就是系统默认的窗口的 FBO
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, GLES20.GL_NONE)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE)
    }

    @JvmStatic
    fun drawWithFBO(fb: Int, textureId: Int, onDraw: () -> Unit) {
        bindFBO(fb, textureId)
        onDraw.invoke()
        unbindFBO()
    }

    @JvmStatic
    fun deleteFBO(frame: IntArray, texture: IntArray) {
        // 删除Render Buffer
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, GLES20.GL_NONE)
        // 删除Frame Buffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)
        GLES20.glDeleteFramebuffers(frame.size, frame, 0)
        // 删除纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE)
        GLES20.glDeleteTextures(texture.size, texture, 0)
    }

    fun createPBO(pixelBuffers: IntArray, sizeInBytes: Int, read: Boolean) {
        /**
         * 可以使用双PBO上传纹理，速度更快
         */
        GLES30.glGenBuffers(pixelBuffers.size, pixelBuffers, 0)

        pixelBuffers.forEach { pboId ->
            if (read) {
                GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pboId)
                GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, sizeInBytes, null, GLES30.GL_STREAM_READ)
            } else {
                GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, pboId)
                // 设置pbo内存大小
                // 这一步十分重要，第2个参数指定了这个缓冲区的大小，单位是字节，一定要注意
                // 然后第3个参数是初始化用的数据，如果你传个内存指针进去，这个函数就会把你的
                // 数据复制到缓冲区里，我们这里一开始并不需要什么数据，所以传个nullptr就行了
                GLES30.glBufferData(GLES30.GL_PIXEL_UNPACK_BUFFER, sizeInBytes, null, GLES30.GL_STREAM_DRAW)
            }
        }
        GLES30.glBindBuffer(if (read) GLES30.GL_PIXEL_PACK_BUFFER else GLES30.GL_PIXEL_UNPACK_BUFFER, 0)
    }

    fun deletePBO(pixelBuffers: IntArray, read: Boolean) {
        GLES30.glBindBuffer(if (read) GLES30.GL_PIXEL_PACK_BUFFER else GLES30.GL_PIXEL_UNPACK_BUFFER, 0)
        GLES30.glDeleteBuffers(pixelBuffers.size, pixelBuffers, 0)
    }

    // 创建并配置纹理
    @JvmStatic
    fun glGenTextures(textures: IntArray) {
        // 创建
        GLES20.glGenTextures(textures.size, textures, 0)
        // 配置
        textures.forEach { texture ->
            // opengl的操作 面向过程的操作
            // bind 就是绑定 ，表示后续的操作就是在这一个 纹理上进行
            // 后面的代码配置纹理，就是配置bind的这个纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)

            // 设置纹理边缘参数
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())

            // 解绑
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
    }

    @JvmStatic
    fun glGenTexturesWithDimen(textures: IntArray, width: Int, height: Int) {
        // 创建
        GLES20.glGenTextures(textures.size, textures, 0)
        // 配置
        textures.forEach { texture ->
            // opengl的操作 面向过程的操作
            // bind 就是绑定 ，表示后续的操作就是在这一个 纹理上进行
            // 后面的代码配置纹理，就是配置bind的这个纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)

            // 设置纹理边缘参数
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())

            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)

//            GLES20.glGenerateMipmap(GLES30.GL_TEXTURE_2D)

            // 解绑
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
    }

    @JvmStatic
    fun readRawTextFile(context: Context, rawId: Int): String {
        val inputStream = context.resources.openRawResource(rawId)
        val br = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        val sb = StringBuilder()
        try {
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
                sb.append("\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            br.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return sb.toString()
    }

    @JvmStatic
    fun loadProgram(vSource: String?, fSource: String?): Int {
        /**
         * 顶点着色器
         */
        val vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        // 加载着色器代码
        GLES20.glShaderSource(vShader, vSource)
        // 编译（配置）
        GLES20.glCompileShader(vShader)

        // 查看配置 是否成功
        val status = IntArray(1)
        GLES20.glGetShaderiv(vShader, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) {
            // 失败
            "load vertex shader:" + GLES20.glGetShaderInfoLog(vShader)
        }
        /**
         * 片元着色器
         * 流程和上面一样
         */
        val fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        // 加载着色器代码
        GLES20.glShaderSource(fShader, fSource)
        // 编译（配置）
        GLES20.glCompileShader(fShader)

        // 查看配置 是否成功
        GLES20.glGetShaderiv(fShader, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) {
            //失败
            "load fragment shader:" + GLES20.glGetShaderInfoLog(fShader)
        }
        /**
         * 创建着色器程序
         */
        val program = GLES20.glCreateProgram()
        // 绑定顶点和片元
        GLES20.glAttachShader(program, vShader)
        GLES20.glAttachShader(program, fShader)
        // 链接着色器程序
        GLES20.glLinkProgram(program)


        //获得状态
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) { "link program:" + GLES20.glGetProgramInfoLog(program) }
        GLES20.glDeleteShader(vShader)
        GLES20.glDeleteShader(fShader)
        return program
    }

    @JvmStatic
    fun copyAssets2SdCard(context: Context, src: String?, dst: String?) {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            val file = File(dst)
            if (!file.exists()) {
                inputStream = context.assets.open(src!!)
                outputStream = FileOutputStream(file)
                var len: Int
                val buffer = ByteArray(2048)
                while (inputStream.read(buffer).also { len = it } != -1) {
                    outputStream.write(buffer, 0, len)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    @JvmStatic
    fun savePixels(x: Int, y: Int, w: Int, h: Int): Bitmap {
        val b = IntArray(w * (y + h))
        val bt = IntArray(w * h)
        val ib = IntBuffer.wrap(b)
        ib.position(0)
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib)
        var i = 0
        var k = 0
        while (i < h) {
            //remember, that OpenGL bitmap is incompatible with Android bitmap
            // and so, some correction need.
            for (j in 0 until w) {
                val pix = b[i * w + j]
                val pb = pix shr 16 and 0xff
                val pr = pix shl 16 and 0x00ff0000
                val pix1 = pix and -0xff0100 or pr or pb
                bt[(h - k - 1) * w + j] = pix1
            }
            i++
            k++
        }
        return Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888)
    }

    fun saveImage(finalBitmap: Bitmap) {
        val myDir = File("/sdcard/saved_images")
        myDir.mkdirs()
        val generator = Random()
        var n = 10000
        n = generator.nextInt(n)
        val fname = "Image-$n.jpg"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Load Texture from Bitmap
     **/
    fun loadTexture(context: Context, bitmap: Bitmap): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] != 0) {
            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GL_RGBA, bitmap, 0)

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle()
        }

        if (textureHandle[0] == 0) {
            throw RuntimeException("Error loading texture.")
        }

        return textureHandle[0]
    }

    /**
     * Load Texture from Bitmap
     **/
    fun loadTexture(context: Context, resourceId: Int): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] != 0) {
            // Read in the resource
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, BitmapFactory.Options().apply {
                inScaled = false   // No pre-scaling
            })

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle()
        }

        if (textureHandle[0] == 0) {
            throw RuntimeException("Error loading texture.")
        }

        return textureHandle[0]
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(byteBuffer)
        byteBuffer.rewind()
        return byteBuffer
    }

    fun loadTextureToGpuWithPBO(pboIds: IntArray, targetTextureId: Int, width: Int, height: Int, bytesPerPixel: Int, byteBuffer: ByteBuffer) {
        val sizeInByte = width * height * bytesPerPixel

        // 绑定纹理
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, targetTextureId)

        // 使用Pbo
        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, pboIds[0])

        /**
         * 获取PBO对应GPU缓冲区的内存地址
         */
        val buffer = GLES30.glMapBufferRange(GLES30.GL_PIXEL_UNPACK_BUFFER, 0, sizeInByte, GLES30.GL_MAP_WRITE_BIT or GLES30.GL_MAP_INVALIDATE_BUFFER_BIT)
        if (buffer.hasRemaining()) {
            (buffer as ByteBuffer).put(byteBuffer)
        }
        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER)

        // 绑定下一个PBO并启动异步上传
        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, pboIds[1])
        // 将pbo缓冲区中的数据拷贝到纹理，调用 glTexSubImage2D 后立即返回，不影响 CPU 时钟周期
        // 这个函数会判断 GL_PIXEL_UNPACK_BUFFER 这个地方有没有绑定一个缓冲区
        // 如果有，就从这个缓冲区读取数据，而不是pixels参数指定的那个内存
        // 这样glTexSubImage2D就会从我们的缓冲区中读取数据了
        // 这里为什么要用glTexSubImage2D呢，因为如果用glTexImage2D，glTexImage2D会销毁纹理内存重新申请，glTexSubImage2D就仅仅只是更新纹理中的数据，这就提高了速度，并且优化了显存的利用率
        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)

        // 交换PBO，为下一次写入做准备
        pboIds[0] = pboIds[1].also { pboIds[1] = pboIds[0] }

        // 解绑PBO和纹理
        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    fun loadTextureFromGpuWithPBO(pboIds: IntArray, sourceTextureId: Int, width: Int, height: Int, bytesPerPixel: Int, block: (ByteBuffer) -> Unit = {}) {
        val sizeInByte = width * height * bytesPerPixel

        // 绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sourceTextureId)

        // 绑定PBO以异步读取
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pboIds[0])
        GLES30.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, 0)

        // 切换到第二个PBO，准备从中读取或操作数据
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pboIds[1])

        // 将PBO数据映射到CPU可访问的内存(在映射PBO以访问其内容时，如果GPU还没有完成数据的读写，尝试映射可能会导致延迟或阻塞，直到GPU完成读写)
        val buffer = GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER, 0, sizeInByte, GLES30.GL_MAP_READ_BIT) as ByteBuffer?

        // 确保buffer不为null，然后可以读取或处理数据
        buffer?.let {
            // 读取或处理数据
            block.invoke(it)

            // 完成数据处理后，解除映射
            GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER)
        }

        // 交换PBO，为下一次读取做准备
        pboIds[0] = pboIds[1].also { pboIds[1] = pboIds[0] }

        // 解绑PBO和纹理
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    /**
     * 根据纹理Id创建Bitmap
     * @param textureId
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    fun captureRenderBitmap(textureId: Int, imageWidth: Int, imageHeight: Int): Bitmap? {
        val bb: ByteBuffer? = captureRenderResult(textureId, imageWidth, imageHeight)
        val createBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
        createBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bb?.array()))
        return createBitmap
    }

    const val NO_TEXTURE = -1

    /**
     * 读取渲染结果的buffer
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @return 渲染结果的像素Buffer 格式RGBA
     */
    fun captureRenderResult(textureId: Int, imageWidth: Int, imageHeight: Int): ByteBuffer? {
        if (textureId <= 0 || imageWidth <= 0 || imageHeight <= 0) {
            error("Invalid arguments")
        }

        val captureBuffer = ByteBuffer.allocateDirect(imageWidth * imageHeight * 4)
        captureBuffer.position(0)
        val frameBuffer = IntArray(1)
        GLES20.glGenFramebuffers(1, frameBuffer, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0])
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, textureId, 0
        )
        GLES20.glReadPixels(
            0, 0, imageWidth, imageHeight,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, captureBuffer
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glDeleteFramebuffers(frameBuffer.size, frameBuffer, 0)
        return captureBuffer
    }


}