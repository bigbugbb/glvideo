package com.binbo.glvideo.core.utils

import android.R
import android.animation.Animator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.math.sqrt


fun View.setMargins(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val p: ViewGroup.MarginLayoutParams = layoutParams as ViewGroup.MarginLayoutParams
        p.setMargins(left, top, right, bottom)
        requestLayout()
    }
}

/**
 * 以当前view为圆心的全屏揭露动画
 */
fun View.createCircularRevealAnimator(targetView: View?, startRadius: Float): Animator {
    val point = getLocationOnScreen() // or getLocationInWindow(point)
    val centerX = point.x + width / 2
    val centerY = point.y + height / 2

    // 获取扩散的半径
    val screenWidth = DeviceUtil.getScreenWidth(context)
    val screenHeight = DeviceUtil.getScreenHeight(context)
    val finalRadius = listOf(Point(0, 0), Point(screenWidth, 0), Point(0, screenHeight), Point(screenWidth, screenHeight))
        .map { sqrt((it.y - centerY).toDouble() * (it.y - centerY) + (it.x - centerX) * (it.x - centerX)) }
        .maxOrNull()!!
    // 定义揭露动画
    return ViewAnimationUtils.createCircularReveal(targetView, centerX, centerY, startRadius, finalRadius.toFloat())
}

fun View.getLocationOnScreen(): Point {
    val point = IntArray(2)
    getLocationOnScreen(point) // or getLocationInWindow(point)
    val (x, y) = point
    return Point(x, y)
}

fun View.getLocationInWindow(): Point {
    val point = IntArray(2)
    getLocationInWindow(point)
    val (x, y) = point
    return Point(x, y)
}

fun View.addRipple() = with(TypedValue()) {
    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
    foreground = context.getDrawable(resourceId)
}

fun View.addCircleRipple() = with(TypedValue()) {
    context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
    foreground = context.getDrawable(resourceId)
}

/**
 * View设置点击涟漪效果
 * 单位  dp
 */
fun View.ripple(radius: Float = 0f, topLeft: Float = 0f, topRight: Float = 0f, bottomLeft: Float = 0f, bottomRight: Float = 0f, color: Int = Color.BLACK) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        isClickable = true
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setColor(color)
        if (radius != 0f) {
            drawable.cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radius, resources.displayMetrics)
        } else {
            val cornerRadii =
                floatArrayOf(dip(topLeft), dip(topLeft), dip(topRight), dip(topRight), dip(bottomLeft), dip(bottomLeft), dip(bottomRight), dip(bottomRight))
            drawable.cornerRadii = cornerRadii
        }
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorControlHighlight, typedValue, true)
        val rippleDrawable = RippleDrawable(ColorStateList.valueOf(typedValue.data), null, GradientDrawable())
        rippleDrawable.setDrawableByLayerId(R.id.mask, drawable.mutate())
        foreground = rippleDrawable
    }
}

fun View.measureAndLayout() {
    val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
    val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
    measure(widthMeasureSpec, heightMeasureSpec)
    layout(0, 0, measuredWidth, measuredHeight)
}

fun View.getViewBitmap(): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
    draw(Canvas(this))
}

inline fun View.center() = Point(x.toInt() + width / 2, y.toInt() + height / 2)

fun View.hideSoftKeyboard() = runOnMainThread {
    val im = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    im.hideSoftInputFromWindow(this.windowToken, 0)
}

fun View.showSoftKeyboard() = runOnMainThread {
    val im = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    requestFocus()
    im.showSoftInput(this, 0)
}

fun View.showWithAlpha(duration: Long = 350, onStartAction: () -> Unit = {}, onEndAction: () -> Unit = {}) {
    if (!isVisible()) {
        animate().alpha(1f)
            .setDuration(duration)
            .withStartAction { onStartAction(); setVisible(true) }
            .withEndAction { onEndAction() }
            .start()
    }
}

fun View.hideWithAlpha(duration: Long = 350, onStartAction: () -> Unit = {}, onEndAction: () -> Unit = {}) {
    animate().alpha(0f)
        .setDuration(duration)
        .withStartAction { onStartAction() }
        .withEndAction { onEndAction(); setVisible(false) }
        .start()
}

fun View.translateToPosition(x: Float, y: Float, duration: Long = 350) {
    animate().translationX(x).translationY(y).setDuration(duration).start()
}

interface ViewDisplayActionWithDelay {
    val targetView: View

    fun cancel()
    fun start(delayMillis: Long = 1600)
}

class ShowAndAutoHideViewDelegate(
    override val targetView: View,
    val duration: Long = 350,
    private val withAlpha: Boolean = true,
    private val onShowViewStart: () -> Unit = {},
    private val onShowViewEnd: () -> Unit = {},
    private val onHideViewStart: () -> Unit = {},
    private val onHideViewEnd: () -> Unit = {},
) : ViewDisplayActionWithDelay, DefaultLifecycleObserver {

    private val handler = Handler(Looper.getMainLooper())

    private var runnableHideView = Runnable {
        if (withAlpha) {
            targetView.hideWithAlpha(duration, onHideViewStart, onHideViewEnd)
        } else {
            onHideViewStart()
            targetView.setVisible(false)
            onHideViewEnd()
        }
    }

    /**
     * 某些情况下手动清理更好用，故添加了该方法
     */
    override fun cancel() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun start(delayMillis: Long) {
        if (withAlpha) {
            targetView.showWithAlpha(duration, onShowViewStart, onShowViewEnd)
        } else {
            onShowViewStart()
            targetView.setVisible(true)
            onShowViewEnd()
        }
        handler.removeCallbacks(runnableHideView)
        handler.postDelayed(runnableHideView, delayMillis)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cancel()
    }
}

class MoveInAndAutoMoveOutViewDelegate(
    override val targetView: View,
    val fromX: Float = 0f,
    val fromY: Float = 0f,
    val toX: Float = 0f,
    val toY: Float = 0f,
    val duration: Long = 350,
) : ViewDisplayActionWithDelay, DefaultLifecycleObserver {

    private val handler = Handler(Looper.getMainLooper())

    private var runnableMoveOutView = Runnable {
        targetView.translateToPosition(fromX, fromY, duration)
    }

    /**
     * 某些情况下手动清理更好用，故添加了该方法
     */
    override fun cancel() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun start(delayMillis: Long) {
        targetView.translateToPosition(toX, toY, duration)
        handler.removeCallbacks(runnableMoveOutView)
        handler.postDelayed(runnableMoveOutView, delayMillis)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cancel()
    }
}

