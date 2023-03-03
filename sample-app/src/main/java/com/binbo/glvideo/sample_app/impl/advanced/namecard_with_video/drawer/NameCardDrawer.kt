package com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.drawer

import android.graphics.Color
import android.graphics.Typeface
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.SystemClock
import android.util.ArrayMap
import android.util.Log
import com.binbo.glvideo.core.ext.createTypeface
import com.binbo.glvideo.core.opengl.drawer.Drawer
import com.binbo.glvideo.core.opengl.program.ClipTextureShaderProgram
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram
import com.binbo.glvideo.core.opengl.utils.Geometry
import com.binbo.glvideo.core.opengl.utils.MatrixHelper
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.core.utils.DeviceUtil
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.avatarRadius
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.footerHeight
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.movingNicknameHeight
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.nameCardAspectRatio
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.nameCardBottom
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.nameCardLeft
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.nameCardPaddingBottom
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.nameCardPaddingLeft
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.nameCardPaddingTop
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.nameCardRight
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.nameCardTop
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.nicknameHeight
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.titleHeight
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.userHandlerHeight
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.ext.createAvatarBitmap
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.ext.createFooterBitmap
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.ext.createTextAsBitmap
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.ext.createTitleBitmap
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.objects.*
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.program.NameCardShaderProgram
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.utils.NameCardRotationManager
import kotlin.math.abs


class NameCardDrawer : Drawer() {

    private var title: Title? = null
    private var avatar: Avatar? = null
    private var nickname: Nickname? = null
    private var movingNickname: MovingNickname? = null
    private var userHandler: UserHandler? = null
    private var footer: Footer? = null
    private var nameCard: NameCard? = null

    private var textureProgram: TextureShaderProgram? = null
    private var nameCardProgram: NameCardShaderProgram? = null
    private var clipTextureProgram: ClipTextureShaderProgram? = null

    private var texturesMap = ArrayMap<Int, Int>()

    private val textures: IntArray
        get() = texturesMap.values.toIntArray()

    private val userHandlerText: String
        get() = "@BinBo"

    private val userNickname: String
        get() = "bigbug"

    private val titleTypeface: Typeface
        get() = createTypeface(Typeface.DEFAULT_BOLD, 900, false)

    private val nicknameTypeface: Typeface
        get() = createTypeface(Typeface.DEFAULT_BOLD, 900, true)

    private val userHandlerTypeface: Typeface
        get() = createTypeface(Typeface.DEFAULT_BOLD, 900, true)

    private var nameCardPressed = false
    private var previousTouchedPoint: Geometry.Point? = null
    private val rotationManager = NameCardRotationManager()

    private var startingTime: Long = SystemClock.uptimeMillis()
    private val isStarting: Boolean
        get() = SystemClock.uptimeMillis() - startingTime < 3000 // 录制3s视频，期间不希望用户与卡片交互

    override fun handleTouchPress(normalizedX: Float, normalizedY: Float) {
        if (isStarting) return
        val ray = convertNormalized2DPointToRay(normalizedX, normalizedY)
        // Define a plane representing our air hockey table.
        val plane = Geometry.Plane(Geometry.Point(0f, 0f, 0f), Geometry.Vector(0f, 0f, 1f))
        // Find out where the touched point intersects the plane representing our name card.
        val touchedPoint = Geometry.intersectionPoint(ray, plane)

        previousTouchedPoint = touchedPoint

        nameCardPressed = touchedPoint.x > nameCardLeft && touchedPoint.x < nameCardRight &&
                touchedPoint.y > nameCardBottom && touchedPoint.y < nameCardTop
        Log.d(TAG, "touched: $touchedPoint pressed: $nameCardPressed")

        if (nameCardPressed) {
            rotationManager.cancelRotationBack()
        }
    }

    override fun handleTouchDragged(normalizedX: Float, normalizedY: Float) {
        if (nameCardPressed) {
            val ray = convertNormalized2DPointToRay(normalizedX, normalizedY)
            // Define a plane representing our air hockey table.
            val plane = Geometry.Plane(Geometry.Point(0f, 0f, 0f), Geometry.Vector(0f, 0f, 1f))
            // Find out where the touched point intersects the plane
            // representing our table. We'll move the mallet along this plane.
            val touchedPoint = Geometry.intersectionPoint(ray, plane)

            val diffX = touchedPoint.x - previousTouchedPoint!!.x
            if (abs(diffX) > 0.0025f) {
                rotationManager.rotateY += if (diffX > 0) 0.75f else -0.75f
                rotationManager.rotateY = clamp(rotationManager.rotateY, -25f, 25f)
            }

            val diffY = touchedPoint.y - previousTouchedPoint!!.y
            if (abs(diffY) > 0.0025f) {
                rotationManager.rotateX += if (diffY > 0) -0.75f else 0.75f
                rotationManager.rotateX = clamp(rotationManager.rotateX, -25f, 25f)
            }

            previousTouchedPoint = touchedPoint
        }
    }

    override fun handleTouchRelease(normalizedX: Float, normalizedY: Float) {
        nameCardPressed = false
        rotationManager.rotateBack()
    }

    private fun convertNormalized2DPointToRay(normalizedX: Float, normalizedY: Float): Geometry.Ray {
        // We'll convert these normalized device coordinates into world-space
        // coordinates. We'll pick a point on the near and far planes, and draw a
        // line between them. To do this transform, we need to first multiply by
        // the inverse matrix, and then we need to undo the perspective divide.
        val nearPointNdc = floatArrayOf(normalizedX, normalizedY, -1f, 1f)
        val farPointNdc = floatArrayOf(normalizedX, normalizedY, 1f, 1f)
        val nearPointWorld = FloatArray(4)
        val farPointWorld = FloatArray(4)
        Matrix.multiplyMV(nearPointWorld, 0, invertedViewProjectionMatrix, 0, nearPointNdc, 0)
        Matrix.multiplyMV(farPointWorld, 0, invertedViewProjectionMatrix, 0, farPointNdc, 0)

        // Why are we dividing by W? We multiplied our vector by an inverse
        // matrix, so the W value that we end up is actually the *inverse* of
        // what the projection matrix would create. By dividing all 3 components
        // by W, we effectively undo the hardware perspective divide.
        divideByW(nearPointWorld)
        divideByW(farPointWorld)

        // We don't care about the W value anymore, because our points are now
        // in world coordinates.
        val nearPointRay = Geometry.Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2])
        val farPointRay = Geometry.Point(farPointWorld[0], farPointWorld[1], farPointWorld[2])
        return Geometry.Ray(nearPointRay, Geometry.vectorBetween(nearPointRay, farPointRay))
    }

    private fun divideByW(vector: FloatArray) {
        vector[0] /= vector[3]
        vector[1] /= vector[3]
        vector[2] /= vector[3]
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return Math.min(max, Math.max(value, min))
    }

    private fun positionNameCardInScene() {
        // 90 degrees to lie flat on the XZ plane.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateZ, 0f, 0f, 1f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    private fun positionTitleInScene() {
        // 90 degrees to lie flat on the XZ plane.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateZ, 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, nameCardLeft + nameCardPaddingLeft, nameCardTop - titleHeight - nameCardPaddingTop, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    private fun positionNicknameInScene() {
        // 90 degrees to lie flat on the XZ plane.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateZ, 0f, 0f, 1f)
        if (userHandlerText.isNullOrBlank()) {
            Matrix.translateM(modelMatrix, 0, nameCardLeft + nameCardPaddingLeft + 0.005f, -0.49f, 0f)
        } else {
            Matrix.translateM(modelMatrix, 0, nameCardLeft + nameCardPaddingLeft + 0.005f, -0.35f, 0f)
        }
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    private fun positionUserHandlerInScene() {
        // 90 degrees to lie flat on the XZ plane.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateZ, 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, nameCardLeft + nameCardPaddingLeft, -0.49f, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    private fun positionMovingNicknameInScene() {
        // 90 degrees to lie flat on the XZ plane.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateZ, 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, movingNickname?.xOffset ?: 0f, movingNicknameHeight / 2f + 0.14f, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    private fun positionAvatarInScene() {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateZ, 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, 0f, 0.14f, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    private fun positionFooterInScene() {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationManager.rotateZ, 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, nameCardLeft + nameCardPaddingLeft + 0.003f, nameCardBottom + nameCardPaddingBottom, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    override fun onWorldCreated() {
        createTitleBitmap(titleTypeface)?.let {
            texturesMap[titleId] = OpenGLUtils.loadTexture(context, it)
            title = Title(titleHeight * it.width / it.height.toFloat(), titleHeight)
        }

        createTextAsBitmap(userNickname, DeviceUtil.sp2pxF(context, 50f), Color.WHITE, nicknameTypeface, nicknameHeight, 1.275f)?.let {
            texturesMap[nicknameId] = OpenGLUtils.loadTexture(context, it)
            nickname = Nickname(nicknameHeight * it.width / it.height.toFloat(), nicknameHeight)
        }

        createTextAsBitmap(userHandlerText, DeviceUtil.sp2pxF(context, 32f), Color.WHITE, userHandlerTypeface, userHandlerHeight, 1.2f)?.let {
            texturesMap[userHandlerId] = OpenGLUtils.loadTexture(context, it)
            userHandler = UserHandler(userHandlerHeight * it.width / it.height.toFloat(), userHandlerHeight)
        }

        createTextAsBitmap(userNickname, DeviceUtil.sp2pxF(context, 64f), Color.WHITE, nicknameTypeface)?.let {
            texturesMap[movingNicknameId] = OpenGLUtils.loadTexture(context, it)
            movingNickname = MovingNickname(movingNicknameHeight * it.width / it.height.toFloat(), movingNicknameHeight)
        }

        avatar = Avatar(avatarRadius)
        createAvatarBitmap()?.let {
            texturesMap[avatarId] = OpenGLUtils.loadTexture(context, it)
        }

        createFooterBitmap()?.let {
            texturesMap[footerId] = OpenGLUtils.loadTexture(context, it)
            footer = Footer(footerHeight * it.width / it.height.toFloat(), footerHeight)
        }

        nameCard = NameCard()

        textureProgram = TextureShaderProgram(context)
        clipTextureProgram = ClipTextureShaderProgram(context)
        nameCardProgram = NameCardShaderProgram(context)
    }

    override fun setViewportSize(width: Int, height: Int) {
        super.setViewportSize(width, height)
        // Set the OpenGL viewport to fill the entire surface.
        GLES20.glViewport(0, 0, width, height)
        MatrixHelper.perspectiveM(projectionMatrix, 45f, width.toFloat() / height.toFloat(), 1f, 10f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 4f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun setTextureID(textureId: Int) {
        texturesMap[nameCardId] = textureId
    }

    override fun release() {
        rotationManager.cancelRotationBack()

        textureProgram?.deleteProgram()
        textureProgram = null
        clipTextureProgram?.deleteProgram()
        clipTextureProgram = null
        nameCardProgram?.deleteProgram()
        nameCardProgram = null

        // 删除纹理
        val texturesArray = textures
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE)
        GLES20.glDeleteTextures(texturesArray.size, texturesArray, 0)
    }

    override fun draw() {
        super.draw()

        GLES20.glEnable(GLES20.GL_BLEND)

        if (isStarting) {
            rotationManager.startEnterRotation()
        }

        // Update the viewProjection matrix, and create an inverted matrix for touch picking.
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0)

        // The color of name card border has alpha channel
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA) // Add this line breaks everything on some phone (like HUAWEI)

        // Draw the name card.
        texturesMap[nameCardId]?.takeIf { it != 0 }?.let { nameCardTextureId ->
            positionNameCardInScene()
            nameCardProgram?.useProgram()
            nameCard?.let {
                nameCardProgram?.setUniforms(
                    modelViewProjectionMatrix,
                    nameCardTextureId,
                    it.cornerRadius,
                    it.borderSize,
                    viewportWidth,
                    (viewportWidth / nameCardAspectRatio).toInt()
                )
                it.bindData(nameCardProgram)
                it.draw()
            }
        }

        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA) // 绘制文字纹理时用这个混合模式可以避免边缘的黑边

        // Draw the name card title
        texturesMap[titleId]?.takeIf { it != 0 }?.let { titleTextureId ->
            positionTitleInScene()
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, titleTextureId, 1f)
            title?.bindData(textureProgram)
            title?.draw()
        }

        // Draw the name card nickname
        texturesMap[nicknameId]?.takeIf { it != 0 }?.let { nicknameTextureId ->
            positionNicknameInScene()
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, nicknameTextureId, 1f)
            nickname?.bindData(textureProgram)
            nickname?.draw()
        }

        // Draw the name card user handler
        texturesMap[userHandlerId]?.takeIf { it != 0 }?.let { userHandlerTextureId ->
            positionUserHandlerInScene()
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, userHandlerTextureId, 1f)
            userHandler?.bindData(textureProgram)
            userHandler?.draw()
        }

        // Draw the moving name card nickname
        texturesMap[movingNicknameId]?.takeIf { it != 0 }?.let { movingNicknameTextureId ->
            positionMovingNicknameInScene()
            movingNickname?.update()
            clipTextureProgram?.useProgram()
            clipTextureProgram?.setUniforms(
                modelViewProjectionMatrix, movingNicknameTextureId, 1f,
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

        // Draw the footer
        texturesMap[footerId]?.takeIf { it != 0 }?.let { footerTextureId ->
            positionFooterInScene()
            textureProgram?.useProgram()
            textureProgram?.setUniforms(modelViewProjectionMatrix, footerTextureId, 1f)
            footer?.bindData(textureProgram)
            footer?.draw()
        }

        GLES20.glDisable(GLES20.GL_BLEND)
    }

    companion object {
        const val TAG = "NameCardDrawer"

        const val titleId = 0
        const val avatarId = 1
        const val nicknameId = 2
        const val movingNicknameId = 3
        const val userHandlerId = 4
        const val footerId = 5
        const val nameCardId = 6
    }
}