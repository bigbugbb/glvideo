package com.binbo.glvideo.sample_app.impl.advanced.namecard.drawer

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
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.avatarRadius
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.footerHeight
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardAspectRatio
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardBottom
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardLeft
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardPaddingBottom
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardPaddingLeft
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardRight
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardTop
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.movingNicknameHeight
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.nicknameHeight
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.userHandlerHeight
import com.binbo.glvideo.sample_app.impl.advanced.namecard.ext.createAvatarBitmap
import com.binbo.glvideo.sample_app.impl.advanced.namecard.ext.createFooterBitmap
import com.binbo.glvideo.sample_app.impl.advanced.namecard.ext.createTextAsBitmap
import com.binbo.glvideo.sample_app.impl.advanced.namecard.objects.*
import com.binbo.glvideo.sample_app.impl.advanced.namecard.program.NameCardShaderProgram
import com.binbo.glvideo.sample_app.impl.advanced.namecard.utils.NameCardRotationManager
import kotlin.math.abs


open class NameCardDrawer : Drawer() {

    protected var avatar: Avatar? = null
    protected var nickname: Nickname? = null
    protected var movingNickname: MovingNickname? = null
    protected var userHandler: UserHandler? = null
    protected var footer: Footer? = null
    protected var nameCard: NameCard? = null

    protected var textureProgram: TextureShaderProgram? = null
    protected var missionCardProgram: NameCardShaderProgram? = null
    protected var clipTextureProgram: ClipTextureShaderProgram? = null

    protected var texturesMap = ArrayMap<Int, Int>()

    protected val textures: IntArray
        get() = texturesMap.values.toIntArray()

    protected val userHandlerText: String
        get() = "@BinBo"

    protected val userNickname: String
        get() = "bigbug"

    protected open val offsetY = 0.125f

    private val movingNicknameTypeface: Typeface
        get() = createTypeface(Typeface.DEFAULT_BOLD, 900, true)

    private val nicknameTypeface: Typeface
        get() = createTypeface(Typeface.DEFAULT_BOLD, 1000, false)

    private val userHandlerTypeface: Typeface
        get() = createTypeface(Typeface.DEFAULT, 400, false)

    private var missionCardPressed = false
    private var previousTouchedPoint: Geometry.Point? = null
    private val rotationManager = NameCardRotationManager()

    private var startingTime: Long = SystemClock.uptimeMillis()
    private val isStarting: Boolean
        get() = SystemClock.uptimeMillis() - startingTime < 3000 // 录制3s视频，期间不希望用户与卡片交互

    open val elementRotationX: Float
        get() = rotationManager.rotateX

    open val elementRotationY: Float
        get() = rotationManager.rotateY

    open val elementRotationZ: Float
        get() = rotationManager.rotateZ

    override fun handleTouchPress(normalizedX: Float, normalizedY: Float) {
        if (isStarting) return
        val ray = convertNormalized2DPointToRay(normalizedX, normalizedY)
        // Define a plane representing our air hockey table.
        val plane = Geometry.Plane(Geometry.Point(0f, 0f, 0f), Geometry.Vector(0f, 0f, 1f))
        // Find out where the touched point intersects the plane representing our name card.
        val touchedPoint = Geometry.intersectionPoint(ray, plane)

        previousTouchedPoint = touchedPoint

        missionCardPressed = touchedPoint.x > missionCardLeft && touchedPoint.x < missionCardRight &&
                touchedPoint.y > missionCardBottom && touchedPoint.y < missionCardTop
        Log.d(TAG, "touched: $touchedPoint pressed: $missionCardPressed")

        if (missionCardPressed) {
            rotationManager.cancelRotationBack()
        }
    }

    override fun handleTouchDragged(normalizedX: Float, normalizedY: Float) {
        if (missionCardPressed) {
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
        missionCardPressed = false
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

    protected fun positionMissionCardInScene() {
        // 90 degrees to lie flat on the XZ plane.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, offsetY, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationZ, 0f, 0f, 1f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    protected fun positionNicknameInScene() {
        // 90 degrees to lie flat on the XZ plane.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, elementRotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationZ, 0f, 0f, 1f)
        if (userHandlerText.isNullOrBlank()) {
            Matrix.translateM(modelMatrix, 0, missionCardLeft + missionCardPaddingLeft + 0.005f, offsetY - 0.49f, 0f)
        } else {
            Matrix.translateM(modelMatrix, 0, missionCardLeft + missionCardPaddingLeft + 0.005f, offsetY - 0.35f, 0f)
        }
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    protected fun positionUserHandlerInScene() {
        // 90 degrees to lie flat on the XZ plane.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, elementRotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationZ, 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, missionCardLeft + missionCardPaddingLeft, -0.49f + offsetY, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    protected fun positionMovingNicknameInScene() {
        // 90 degrees to lie flat on the XZ plane.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, elementRotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationZ, 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, movingNickname?.xOffset ?: 0f, movingNicknameHeight / 2f + 0.155f + offsetY, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    protected open fun positionAvatarInScene() {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, elementRotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationZ, 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, 0f, 0.14f + offsetY, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    protected fun positionFooterInScene() {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, elementRotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, elementRotationZ, 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, missionCardLeft + missionCardPaddingLeft + 0.003f, missionCardBottom + missionCardPaddingBottom + offsetY, 0f)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    override fun onWorldCreated() {
        createTextAsBitmap(userNickname, DeviceUtil.sp2pxF(context, 50f), Color.WHITE, nicknameTypeface, nicknameHeight, 1.275f)?.let {
            texturesMap[nicknameId] = OpenGLUtils.loadTexture(context, it)
            nickname = Nickname(nicknameHeight * it.width / it.height.toFloat(), nicknameHeight)
        }

        createTextAsBitmap(userHandlerText, DeviceUtil.sp2pxF(context, 32f), Color.WHITE, userHandlerTypeface, userHandlerHeight, 1.2f)?.let {
            texturesMap[userHandlerId] = OpenGLUtils.loadTexture(context, it)
            userHandler = UserHandler(userHandlerHeight * it.width / it.height.toFloat(), userHandlerHeight)
        }

        createTextAsBitmap(userNickname, DeviceUtil.sp2pxF(context, 64f), Color.WHITE, movingNicknameTypeface)?.let {
            texturesMap[movingNicknameId] = OpenGLUtils.loadTexture(context, it)
            movingNickname = MovingNickname(movingNicknameHeight * it.width / it.height.toFloat(), movingNicknameHeight)
        }

        createAvatarBitmap()?.let {
            texturesMap[avatarId] = OpenGLUtils.loadTexture(context, it)
            avatar = Avatar(avatarRadius)
        }

        createFooterBitmap()?.let {
            texturesMap[footerId] = OpenGLUtils.loadTexture(context, it)
            footer = Footer(footerHeight * it.width / it.height.toFloat(), footerHeight)
        }

        nameCard = NameCard()

        textureProgram = TextureShaderProgram(context)
        clipTextureProgram = ClipTextureShaderProgram(context)
        missionCardProgram = NameCardShaderProgram(context)
    }

    override fun setViewportSize(width: Int, height: Int) {
        super.setViewportSize(width, height)
        // Set the OpenGL viewport to fill the entire surface.
        GLES20.glViewport(0, 0, width, height)
        MatrixHelper.perspectiveM(projectionMatrix, 45f, width.toFloat() / height.toFloat(), 1f, 10f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 4f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun release() {
        rotationManager.cancelRotationBack()

        textureProgram?.deleteProgram()
        textureProgram = null
        clipTextureProgram?.deleteProgram()
        clipTextureProgram = null
        missionCardProgram?.deleteProgram()
        missionCardProgram = null

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
        positionMissionCardInScene()
        missionCardProgram?.useProgram()
        nameCard?.let {
            missionCardProgram?.setUniforms(
                modelViewProjectionMatrix,
                it.cornerRadius,
                it.borderSize,
                viewportWidth,
                (viewportWidth / missionCardAspectRatio).toInt()
            )
            it.bindData(missionCardProgram)
            it.draw()
        }

        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA) // 绘制文字纹理时用这个混合模式可以避免边缘的黑边

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
        const val TAG = "MissionCardDrawer"

        const val titleId = 0
        const val avatarId = 1
        const val nicknameId = 2
        const val movingNicknameId = 3
        const val userHandlerId = 4
        const val footerId = 5
        const val missionCardId = 6
    }
}