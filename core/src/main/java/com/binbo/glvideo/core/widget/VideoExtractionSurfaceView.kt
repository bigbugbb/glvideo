package com.binbo.glvideo.core.widget

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceView
import android.view.animation.DecelerateInterpolator
import com.binbo.glvideo.core.opengl.renderer.DefaultGLRenderer
import com.binbo.glvideo.core.utils.dip
import kotlin.math.abs

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/15
 * @time 18:48
 */
class VideoExtractionSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs) {

    private val TAG = "VideoExtractionSurfaceView"

    private val horizontalMinDistance = dip(8)

    private var midPntX = 0f
    private var midPntY = 0f

    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    private var renderer: DefaultGLRenderer? = null

    private var flingXAnimator: ValueAnimator? = null

    init {
        setupGestureListeners()
    }

    fun setRenderer(renderer: DefaultGLRenderer) {
        this.renderer = renderer
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        flingXAnimator?.cancel()
        flingXAnimator = null
    }

    /**
     * If it's ACTION_DOWN event - user touches the screen and all current animation must be canceled.
     * If it's ACTION_UP event - user removed all fingers from the screen and current image position must be corrected.
     * If there are more than 2 fingers - update focal point coordinates.
     * Pass the event to the gesture detectors if those are enabled.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_DOWN) {
            cancelAllAnimations()
        }
        if (event.pointerCount > 1) {
            midPntX = (event.getX(0) + event.getX(1)) / 2
            midPntY = (event.getY(0) + event.getY(1)) / 2
        }
        gestureDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)

        if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_UP) {
        }
        return true
    }

    private fun cancelAllAnimations() {
        flingXAnimator?.cancel()
    }

    private fun setupGestureListeners() {
        gestureDetector = GestureDetector(context, GestureListener(), null, true)
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            renderer?.queueEvent {
                renderer?.onScale(scaleFactor)
            }
            return true
        }
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        private val minVelocity: Int
            get() = width / 3

        private val maxVelocity: Int
            get() = width * 5

        private var lastFlingX: Int = 0

        override fun onDown(e: MotionEvent): Boolean {
            val normalizedX = e.x / width.toFloat() * 2 - 1
            val normalizedY = -(e.y / height.toFloat() * 2 - 1)
            renderer?.queueEvent {
                renderer?.onTouchPress(normalizedX, normalizedY)
            }
            return false
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            renderer?.queueEvent {
                val normalizedX = distanceX / width.toFloat()
                renderer?.onScroll(-normalizedX, 0f)
            }
            return false
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val totalDistance = abs(velocityX.toInt()).coerceAtMost(maxVelocity)
            if (e1.x - e2.x > horizontalMinDistance && abs(velocityX) > minVelocity) {
//                LogUtil.d(TAG, "fling left")
                flingXAnimator = ValueAnimator.ofInt(0, totalDistance).apply {
                    lastFlingX = 0
                    duration = 2500
                    interpolator = DecelerateInterpolator()
                    addUpdateListener {
                        val diff = (it.animatedValue as Int) - lastFlingX
                        lastFlingX += diff

                        renderer?.queueEvent {
                            val normalizedX = diff / width.toFloat()
                            renderer?.onFling(-normalizedX, 0f)
                        }
                    }
                    start()
                }
            } else if (e2.x - e1.x > horizontalMinDistance && abs(velocityX) > minVelocity) {
//                LogUtil.d(TAG, "fling right")
                flingXAnimator = ValueAnimator.ofInt(0, totalDistance).apply {
                    lastFlingX = 0
                    duration = 2500
                    interpolator = DecelerateInterpolator()
                    addUpdateListener {
                        val diff = (it.animatedValue as Int) - lastFlingX
                        lastFlingX += diff

                        renderer?.queueEvent {
                            val normalizedX = diff / width.toFloat()
                            renderer?.onFling(normalizedX, 0f)
                        }
                    }
                    start()
                }
            }
            return true
        }
    }
}