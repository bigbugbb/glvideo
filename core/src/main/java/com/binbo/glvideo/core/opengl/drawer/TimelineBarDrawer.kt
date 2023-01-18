package com.binbo.glvideo.core.opengl.drawer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.ArrayMap
import android.view.LayoutInflater
import android.view.View
import androidx.core.math.MathUtils.clamp
import com.binbo.glvideo.core.GLVideo.Core.context
import com.binbo.glvideo.core.R
import com.binbo.glvideo.core.opengl.objects.Rectangle
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.core.utils.dip
import com.binbo.glvideo.core.utils.getViewBitmap
import com.binbo.glvideo.core.utils.rotate

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/19
 * @time 11:45
 */
class TimelineBarDrawer : Drawer() {

    private var leftBar: Rectangle? = null
    private var rightBar: Rectangle? = null
    private var borderline: Rectangle? = null
    private var playingThumb: Rectangle? = null

    protected var textureProgram: TextureShaderProgram? = null

    protected var texturesMap = ArrayMap<Int, Int>()

    protected val textures: IntArray
        get() = texturesMap.values.toIntArray()

    private var barAspectRatio: Float = 1f

    var barWidth: Float = 1f
    var barHeight: Float = 1f

    var startTimestampUs: Long = 1000000L
    var stopTimestampUs: Long = 5000000L

    private var playingTimestampUs: Long = -1L

    private var intervalUsPerPixel: Long = 0L
    private var contentPaddingHorizontal: Int = 0

    init {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setIdentityM(projectionMatrix, 0)
        Matrix.setIdentityM(viewProjectionMatrix, 0)
    }

    fun updateStartTime(timestampUs: Long) {
        startTimestampUs = timestampUs
    }

    fun updateStopTime(timestampUs: Long) {
        stopTimestampUs = timestampUs
    }

    fun setPlayingTimestamp(timestampUs: Long) {
        playingTimestampUs = timestampUs
    }

    fun setIntervalUsPerPixel(intervalUsPerPixel: Long) {
        this.intervalUsPerPixel = intervalUsPerPixel
    }

    fun setContentPaddingHorizontal(contentPaddingHorizontal: Int) {
        this.contentPaddingHorizontal = contentPaddingHorizontal
    }

    private fun positionBarInScene(offset: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, offset, 0f, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    private fun positionBorderlineInScene(offsetX: Float, offsetY: Float) {
        // 90 degrees to lie flat on the XZ plane.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, offsetX, offsetY, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    private fun positionPlayingThumbInScene(offset: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, offset, 0f, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    override fun onWorldCreated() {
        createTimelineBarBitmap()?.let {
            barAspectRatio = it.width / it.height.toFloat()
            val rotatedBitmap = it.rotate(180f)
            texturesMap[0] = OpenGLUtils.loadTexture(context, it)
            texturesMap[1] = OpenGLUtils.loadTexture(context, rotatedBitmap)
            leftBar = Rectangle(); rightBar = Rectangle()
        }

        createTimelinePlayingThumbBitmap()?.let {
            texturesMap[2] = OpenGLUtils.loadTexture(context, it)
            playingThumb = Rectangle()
        }

        createBorderlineBitmap(1, 1)?.let {
            texturesMap[3] = OpenGLUtils.loadTexture(context, it)
            borderline = Rectangle()
        }

        textureProgram = TextureShaderProgram(context)
    }

    override fun setViewportSize(width: Int, height: Int) {
        super.setViewportSize(width, height)
        barWidth = barAspectRatio * height
        barHeight = height.toFloat()
        leftBar?.updateVertices(barWidth / width, barHeight / height)
        rightBar?.updateVertices(barWidth / width, barHeight / height)
        playingThumb?.updateVertices(dip(3f) / width, 1f)
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

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA) // Add this line breaks everything on some phone (like HUAWEI)

        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)

        val normalizedBarWidth = barWidth / viewportWidth
        val startOffsetX = (startTimestampUs.toFloat() / intervalUsPerPixel + contentPaddingHorizontal) / viewportWidth * 2 - 1
        val stopOffsetX = (stopTimestampUs.toFloat() / intervalUsPerPixel + contentPaddingHorizontal) / viewportWidth * 2 - 1
        val playingOffSetX =
            clamp((playingTimestampUs.toFloat() / intervalUsPerPixel + contentPaddingHorizontal) / viewportWidth * 2 - 1, startOffsetX, stopOffsetX)

        // Draw the border lines
        val borderHeight = 4f / viewportHeight
        texturesMap[3]?.takeIf { it != 0 }?.let { textureId ->
            positionBorderlineInScene((stopOffsetX + startOffsetX) / 2f, 1f - borderHeight)
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, textureId, 1f)
            borderline?.updateVertices((stopOffsetX - startOffsetX) / 2f, borderHeight)
            borderline?.bindData(textureProgram)
            borderline?.draw()
        }
        texturesMap[3]?.takeIf { it != 0 }?.let { textureId ->
            positionBorderlineInScene((stopOffsetX + startOffsetX) / 2f, -1 + borderHeight)
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, textureId, 1f)
            borderline?.updateVertices((stopOffsetX - startOffsetX) / 2f, borderHeight)
            borderline?.bindData(textureProgram)
            borderline?.draw()
        }

        // Draw the playing thumb to indicate playback progress
        if (playingOffSetX > startOffsetX || startOffsetX < stopOffsetX) {
            texturesMap[2]?.takeIf { it != 0 }?.let { textureId ->
                positionPlayingThumbInScene(playingOffSetX)
                textureProgram?.useProgram()
                textureProgram?.setUniforms(modelViewProjectionMatrix, textureId, 1f)
                playingThumb?.bindData(textureProgram)
                playingThumb?.draw()
            }
        }

        // Draw the left bar
        texturesMap[0]?.takeIf { it != 0 }?.let { textureId ->
            positionBarInScene(startOffsetX - normalizedBarWidth)
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, textureId, 1f)
            leftBar?.bindData(textureProgram)
            leftBar?.draw()
        }

        // Draw the right bar
        texturesMap[1]?.takeIf { it != 0 }?.let { textureId ->
            positionBarInScene(stopOffsetX + normalizedBarWidth)
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, textureId, 1f)
            rightBar?.bindData(textureProgram)
            rightBar?.draw()
        }

        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun createTimelineBarBitmap(): Bitmap? {
        val rootView = LayoutInflater.from(context).inflate(R.layout.layout_timeline_bar, null)
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
        rootView.measure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = rootView.measuredWidth
        val measuredHeight = rootView.measuredHeight
        rootView.layout(0, 0, measuredWidth, measuredHeight)
        return rootView.getViewBitmap()
    }

    private fun createBorderlineBitmap(width: Int, height: Int): Bitmap {
        val paint = Paint().apply {
            style = Paint.Style.FILL
            isFilterBitmap = true
            isDither = true
            color = Color.WHITE
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    private fun createTimelinePlayingThumbBitmap(): Bitmap? {
        val rootView = LayoutInflater.from(context).inflate(R.layout.layout_timeline_playing_thumb, null)
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
        rootView.measure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = rootView.measuredWidth
        val measuredHeight = rootView.measuredHeight
        rootView.layout(0, 0, measuredWidth, measuredHeight)
        return rootView.getViewBitmap()
    }

    companion object {
        const val TAG = "TimelineBarDrawer"
    }
}