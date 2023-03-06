package com.binbo.glvideo.sample_app.impl.advanced.namecard.drawer

import android.opengl.GLES20
import android.opengl.Matrix
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.App.Const.frameRate
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardLeft
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardPaddingLeft
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardPaddingTop
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardTop
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.sloganHeight
import com.binbo.glvideo.sample_app.impl.advanced.namecard.ext.createSloganBitmap
import com.binbo.glvideo.sample_app.impl.advanced.namecard.objects.Slogan
import java.lang.Integer.min


class OffscreenNameCardDrawer : NameCardDrawer() {

    private val angleValues = listOf(
        0.0,
        0.0,
        0.088824034,
        0.40095806,
        0.8667505,
        1.5093219,
        2.4427307,
        3.4586549,
        4.648794,
        6.1944866,
        7.750736,
        9.476985,
        11.620081,
        13.701689,
        15.947407,
        18.667263,
        21.254168,
        23.997746,
        27.268164,
        30.335472,
        33.550476,
        37.339966,
        40.858124,
        44.513596,
        48.78565,
        52.720783,
        56.781517,
        61.495007,
        65.80921,
        70.2361,
        75.345634,
        79.99735,
        84.74777,
        90.204124,
        95.14858,
        100.81095,
        105.92742,
        111.11699,
        117.03626,
        122.36407,
        127.748764,
        133.86792,
        139.35577,
        144.88374,
        151.14388,
        156.73892,
        162.35692,
        168.69772,
        174.34607,
        180.0,
        186.36041,
        192.00768,
        197.64308,
        203.96185,
        209.55363,
        215.11626,
        221.33252,
        226.81497,
        232.25124,
        238.30515,
        243.62547,
        248.88301,
        254.71626,
        259.82324,
        264.8514,
        270.4078,
        275.25223,
        280.00266,
        285.22867,
        289.7639,
        294.19077,
        299.03613,
        303.2185,
        307.27924,
        311.69724,
        315.4864,
        319.14188,
        323.09,
        326.44952,
        329.66452,
        333.1047,
        336.00226,
        338.74582,
        341.645,
        344.05258,
        346.2983,
        348.62848,
        350.523,
        352.24927,
        353.98804,
        355.3512,
        356.54135,
        357.67197,
        358.49066,
        359.13324,
        359.6448,
        359.91116,
        360.0
    )

    private var slogan: Slogan? = null

    private var frames = 0

    override val offsetY = 0f

    override val elementRotationX: Float
        get() = 0f

    override val elementRotationY: Float
        get() {
            val i = min((angleValues.size.toFloat() / frameRate * frames).toInt(), angleValues.lastIndex)
            return angleValues[i].toFloat()
        }

    override val elementRotationZ: Float
        get() = 0f

    private val textAlpha: Float
        get() = when {
            frames < frameRate * 4 -> 1f
            frames < frameRate * 4.5 -> 1f - (frames - frameRate * 4) / (frameRate * 0.5f) * 1f
            else -> 0f
        }

    private val sloganAlpha: Float
        get() = when {
            frames < frameRate * 5 -> 0f
            frames < frameRate * 5.5 -> (frames - frameRate * 5) / (frameRate * 0.5f) * 1f
            else -> 1f
        }

    private val avatarX: Float
        get() = when {
            frames < frameRate * 4 -> 0f
            frames < frameRate * 4.5 -> (frames - frameRate * 4) / (frameRate * 0.5f) * -0.5f
            else -> -0.5f
        }

    private val avatarY: Float
        get() = when {
            frames < frameRate * 4 -> 0.14f
            frames < frameRate * 4.5 -> 0.14f + (frames - frameRate * 4) / (frameRate * 0.5f) * (0.76f - 0.14f)
            else -> 0.76f
        }

    private val avatarScale: Float
        get() = when {
            frames < frameRate * 4 -> 1f
            frames < frameRate * 4.5 -> 1f - (frames - frameRate * 4) / (frameRate * 0.5f) * 0.34f
            else -> 0.66f
        }

    override fun handleTouchPress(normalizedX: Float, normalizedY: Float) {}

    override fun handleTouchDragged(normalizedX: Float, normalizedY: Float) {}

    override fun handleTouchRelease(normalizedX: Float, normalizedY: Float) {}

    override fun positionAvatarInScene() {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, elementRotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationZ, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, avatarScale, avatarScale, 1f)
        Matrix.translateM(modelMatrix, 0, avatarX, avatarY, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    private fun positionSloganInScene() {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, elementRotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationZ, 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, missionCardLeft + missionCardPaddingLeft - 0.02f, missionCardTop - missionCardPaddingTop - 0.56f - sloganHeight, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    override fun onWorldCreated() {
        super.onWorldCreated()

        createSloganBitmap()?.let {
            texturesMap[sloganId] = OpenGLUtils.loadTexture(context, it)
            slogan = Slogan(sloganHeight * it.width / it.height.toFloat(), sloganHeight)
        }
    }

    override fun draw() {
        GLES20.glEnable(GLES20.GL_BLEND)

        // Update the viewProjection matrix, and create an inverted matrix for touch picking.
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0)

        // The color of name card border has alpha channel
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA) // Add this line breaks everything on some phone (like HUAWEI)

        // Draw the name card.
        positionMissionCardInScene()
        missionCardProgram?.useProgram()
        nameCard?.let {
            missionCardProgram?.setUniforms(
                modelViewProjectionMatrix,
                it.cornerRadius,
                it.borderSize,
                viewportWidth,
                (viewportWidth / NameCardConfig.missionCardAspectRatio).toInt()
            )
            it.bindData(missionCardProgram)
            it.draw()
        }

        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA) // 绘制文字纹理时用这个混合模式可以避免边缘的黑边

        // Draw the name card nickname
        texturesMap[nicknameId]?.takeIf { it != 0 }?.let { nicknameTextureId ->
            positionNicknameInScene()
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, nicknameTextureId, textAlpha)
            nickname?.bindData(textureProgram)
            nickname?.draw()
        }

        // Draw the name card user handler
        texturesMap[userHandlerId]?.takeIf { it != 0 }?.let { userHandlerTextureId ->
            positionUserHandlerInScene()
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, userHandlerTextureId, textAlpha)
            userHandler?.bindData(textureProgram)
            userHandler?.draw()
        }

        // Draw the moving name card nickname
        texturesMap[movingNicknameId]?.takeIf { it != 0 }?.let { movingNicknameTextureId ->
            positionMovingNicknameInScene()
            movingNickname?.updateByFrames(frames)
            clipTextureProgram?.useProgram()
            clipTextureProgram?.setUniforms(
                modelViewProjectionMatrix, movingNicknameTextureId, textAlpha,
                movingNickname?.leftOutPortion ?: 0f, 1 - (movingNickname?.rightOutPortion ?: 1f),
                0f, 1f
            )
            movingNickname?.bindData(clipTextureProgram)
            movingNickname?.draw()
        }

        // Draw the avatar
        texturesMap[avatarId]?.takeIf { it != 0 }?.let { avatarTextureId ->
            positionAvatarInScene()
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, avatarTextureId, 1f)
            avatar?.bindData(textureProgram)
            avatar?.draw()
        }

        // Draw the slogan
        texturesMap[sloganId]?.takeIf { it != 0 }?.let { sloganTextureId ->
            positionSloganInScene()
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, sloganTextureId, sloganAlpha)
            slogan?.bindData(textureProgram)
            slogan?.draw()
        }

        // Draw the footer
        texturesMap[footerId]?.takeIf { it != 0 }?.let { footerTextureId ->
            positionFooterInScene()
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, footerTextureId, 1f)
            footer?.bindData(textureProgram)
            footer?.draw()
        }

        frames++

        GLES20.glDisable(GLES20.GL_BLEND)
    }

    companion object {
        const val sloganId = 100
    }
}