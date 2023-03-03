package com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.drawer

import android.opengl.GLES20
import android.os.SystemClock
import com.binbo.glvideo.core.opengl.drawer.Drawer
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.cardVideoSize
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.nameCardAspectRatio
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.objects.ClippedVideoFrame


class VideoHorizontalClippedFrameDrawer : Drawer() {

    private var videoTextureId: Int = -1

    private var clippedVideoFrame: ClippedVideoFrame? = null

    private var textureProgram: TextureShaderProgram? = null

    private var offsetX: Float = 0f
    private var lastUpdateTime: Long = 0
    private val delta = 0.001f // 单位时间移动的距离
    private var direction = 1 // 1表示向右平移，-1表示向左

    val clippedWidth: Int
        get() = (clippedHeight * nameCardAspectRatio).toInt()

    val clippedHeight: Int
        get() = (viewportWidth.toFloat() * cardVideoSize.height / cardVideoSize.width).toInt()

    override fun onWorldCreated() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        clippedVideoFrame = ClippedVideoFrame(0f, 1f, 1f, 0f)

        textureProgram = TextureShaderProgram(context)
    }

    override fun setTextureID(textureId: Int) {
        videoTextureId = textureId
    }

    override fun release() {
        textureProgram?.deleteProgram()
        textureProgram = null
    }

    override fun draw() {
        if (videoTextureId != -1) {
            val now = SystemClock.uptimeMillis()
            if (now - lastUpdateTime > 16) {
                offsetX += delta * direction
                lastUpdateTime = now

                val normalizedWidth = clippedWidth.toFloat() / viewportWidth
                clippedVideoFrame?.updateClipping(offsetX, normalizedWidth + offsetX, 0f, 1f)

                if (offsetX - delta < 0f || offsetX + normalizedWidth + delta > 1f) {
                    direction *= -1
                }
            }

            GLES20.glViewport(0, 0, clippedWidth, clippedHeight)
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, videoTextureId, 1f)
            clippedVideoFrame?.bindData(textureProgram)
            clippedVideoFrame?.draw()
            GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        }
    }
}

