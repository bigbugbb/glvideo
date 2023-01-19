package com.binbo.glvideo.core.opengl.drawer

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.ArrayMap
import android.view.LayoutInflater
import android.view.View
import com.binbo.glvideo.core.opengl.objects.LayoutObject
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.core.ext.getViewBitmap


open class LayoutDrawer(protected val context: Context, protected val layoutResId: Int) : Drawer() {

    protected var layoutObject: LayoutObject? = null
    protected var textureProgram: TextureShaderProgram? = null

    protected var texturesMap = ArrayMap<Int, Int>(1)

    private val textures: IntArray
        get() = texturesMap.values.toIntArray()

    var bitmapWidth: Int = -1
    var bitmapHeight: Int = -1

    init {
        Matrix.setIdentityM(viewProjectionMatrix, 0)
    }

    override fun onWorldCreated() {
        super.onWorldCreated()
        textureProgram = TextureShaderProgram(context)
        layoutObject = LayoutObject()
        createBitmapFromLayout()?.let {
            texturesMap[0] = OpenGLUtils.loadTexture(context, it)
            bitmapWidth = it.width
            bitmapHeight = it.height
        }
    }

    protected open fun createBitmapFromLayout(): Bitmap? {
        val rootView = LayoutInflater.from(context).inflate(layoutResId, null)
        setupView(rootView)
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
        rootView.measure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = rootView.measuredWidth
        val measuredHeight = rootView.measuredHeight
        rootView.layout(0, 0, measuredWidth, measuredHeight)
        return rootView.getViewBitmap()
    }

    open fun setupView(rootView: View) {}

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
            layoutObject?.bindData(textureProgram)
            layoutObject?.draw()
        }

        val bitmap = OpenGLUtils.savePixels(0, 0, viewportWidth, viewportHeight)

        GLES20.glDisable(GLES20.GL_BLEND)
    }
}