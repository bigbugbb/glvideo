package com.binbo.glvideo.core.ext

import android.util.Log
import com.binbo.glvideo.core.BuildConfig
import com.binbo.glvideo.core.GLVideo.Core.context

import android.app.ActivityManager
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.location.LocationManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.view.*
import android.view.View.LAYER_TYPE_SOFTWARE
import android.widget.Checkable
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView

import java.io.*
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt


val now: Long
    get() = System.currentTimeMillis()

val nowString: String
    get() = "$now"

val nowSystemClock: Long
    get() = SystemClock.uptimeMillis()

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

fun View.setPreVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.INVISIBLE
}

fun View.isVisible(): Boolean {
    return visibility == View.VISIBLE
}

fun <T : View> T.singleClick(time: Long = 500, onClickListener: View.OnClickListener) {
    setOnClickListener {
        val currentTimeMillis = System.currentTimeMillis()
        if (abs(currentTimeMillis - lastClickTime) > time || this is Checkable) {
            lastClickTime = currentTimeMillis
            onClickListener.onClick(this)
        }
    }
}

var <T : View> T.lastClickTime: Long
    set(value) = setTag(1766613352, value)
    get() = getTag(1766613352) as? Long ?: 0

inline fun View.doOnNextLayout(crossinline action: (view: View) -> Unit) {
    addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(view: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
            view.removeOnLayoutChangeListener(this)
            action(view)
        }
    })
}

fun dip(dpValue: Int): Int {
    val density = context.resources.displayMetrics.density
    return (dpValue * density).roundToInt()
}

fun dip(dpValue: Float): Float {
    val density = context.resources.displayMetrics.density
    return dpValue * density
}

fun isLocationServiceEnable(): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    // getting GPS uploadStatus
    val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    // getting network uploadStatus
    val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    return isGPSEnabled || isNetworkEnabled
}

fun <T : Serializable> deepCopy(obj: T): T {
    val baos = ByteArrayOutputStream()
    val oos = ObjectOutputStream(baos)
    oos.writeObject(obj)
    oos.close()
    val bais = ByteArrayInputStream(baos.toByteArray())
    val ois = ObjectInputStream(bais)
    @Suppress("unchecked_cast")
    return ois.readObject() as T
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.runOnNonNull(block: (T) -> Unit) {
    block.invoke(this as T)
}

var sCircleOutlineProvider: ViewOutlineProvider? = null

fun <T : View> T.circleOutline(): T {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        clipToOutline = true
        if (sCircleOutlineProvider == null) {
            sCircleOutlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View?, outline: Outline?) {
                    view?.run {
                        outline?.setRoundRect(0, 0, width, height, width.toFloat() / 2)
                    }
                }
            }
        }
        outlineProvider = sCircleOutlineProvider
    }
    return this
}

/**
 * 设置圆角
 */
fun <T : View> T.roundOutline(radius: Float): T {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        clipToOutline = true
        if (sCircleOutlineProvider == null) {
            sCircleOutlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View?, outline: Outline?) {
                    view?.run {
                        outline?.setRoundRect(0, 0, width, height, radius)
                    }
                }
            }
        }
        outlineProvider = sCircleOutlineProvider
    }
    return this
}

fun <T : View> T.generateBackgroundWithShadow(
    @ColorRes backgroundColor: Int,
    @DimenRes cornerRadius: Int,
    @ColorRes shadowColor: Int,
    @DimenRes elevation: Int,
    shadowGravity: Int
): LayerDrawable {
    val cornerRadiusValue = context.resources.getDimension(cornerRadius)
    val elevationValue = context.resources.getDimension(elevation).toInt()
    val shadowColorValue: Int = ContextCompat.getColor(context, shadowColor)
    val backgroundColorValue: Int = ContextCompat.getColor(context, backgroundColor)
    val outerRadius = floatArrayOf(
        cornerRadiusValue, cornerRadiusValue, cornerRadiusValue, cornerRadiusValue,
        cornerRadiusValue, cornerRadiusValue, cornerRadiusValue, cornerRadiusValue
    )
//    val backgroundPaint = Paint().apply {
//        style = Paint.Style.FILL
//        setShadowLayer(cornerRadiusValue, 0f, 0f, 0)
//    }
    val shapeDrawablePadding = Rect().apply {
        left = elevationValue
        right = elevationValue
    }
    val dy: Float
    when (shadowGravity) {
        Gravity.CENTER -> {
            shapeDrawablePadding.top = elevationValue
            shapeDrawablePadding.bottom = elevationValue
            dy = 0f
        }
        Gravity.TOP -> {
            shapeDrawablePadding.top = elevationValue * 2
            shapeDrawablePadding.bottom = elevationValue
            dy = -1 * elevationValue / 3f
        }
        Gravity.BOTTOM -> {
            shapeDrawablePadding.top = elevationValue
            shapeDrawablePadding.bottom = elevationValue * 2
            dy = elevationValue / 3f
        }
        else -> {
            shapeDrawablePadding.top = elevationValue
            shapeDrawablePadding.bottom = elevationValue * 2
            dy = elevationValue / 3f
        }
    }
    val shapeDrawable = ShapeDrawable().apply {
        setPadding(shapeDrawablePadding)
//        paint.color = backgroundColorValue
        paint.setShadowLayer(cornerRadiusValue, 0f, dy, shadowColorValue)
    }
    setLayerType(LAYER_TYPE_SOFTWARE, shapeDrawable.paint)
    shapeDrawable.shape = RoundRectShape(outerRadius, null, null)
    return LayerDrawable(arrayOf<Drawable>(shapeDrawable)).apply {
        setLayerInset(0, elevationValue, elevationValue * 2, elevationValue, elevationValue * 2)
    }
}

fun debugToast(msg: String?) {
    if (BuildConfig.DEBUG && msg != null) {
        Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT).show()
    }
}

fun isDebug(): Boolean {
    return BuildConfig.DEBUG
}

@ColorInt
fun parseColorAlpha(colorStr: String, @FloatRange(from = 0.0, to = 1.0) alpha: Float = 0.0f): Int {
    val colorInt = Color.parseColor(colorStr)
    if (alpha != -1f) {
        val red = Color.red(colorInt)
        val green = Color.green(colorInt)
        val blue = Color.blue(colorInt)
        return Color.argb((alpha * 0xff).toInt(), red, green, blue)
    }
    return colorInt
}

fun isMainThread() = Thread.currentThread() == Looper.getMainLooper().thread

fun runOnMainThread(block: () -> Unit) {
    if (isMainThread()) {
        block()
    } else {
        mainHandler.post { block() }
    }
}

val isMainProcess: Boolean
    get() = TextUtils.equals(
        context.applicationContext.packageName,
        currentProcessName
    )

/**
 * 获取当前进程名称
 *
 * @return
 */
@get:NonNull
val currentProcessName: String
    get() {
        val pid: Int = android.os.Process.myPid()
        val manager: ActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (manager.runningAppProcesses != null) {
            for (process: ActivityManager.RunningAppProcessInfo in Objects.requireNonNull(manager).runningAppProcesses) {
                if (process.pid === pid) {
                    return process.processName
                }
            }
        }
        return ""
    }

fun <T> MutableLiveData<T>.default(value: T): MutableLiveData<T> {
    this.value = value
    return this
}

fun <VH : RecyclerView.ViewHolder> RecyclerView.Adapter<VH>.notifyDataSetChangedInMain() {
    if (isMainThread()) {
        this.notifyDataSetChanged()
    } else {
        Handler(Looper.getMainLooper()).post {
            this.notifyDataSetChanged()
        }
    }
}

val mainHandler = Handler(Looper.getMainLooper())

fun threadName() = Thread.currentThread().name

fun isHarmony(context: Context): Boolean {
    return try {
        val id: Int = Resources.getSystem().getIdentifier("config_os_brand", "string", "android")
        val osBrand = context.getString(id)
        osBrand == "harmony"
    } catch (e: Exception) {
        false
    }
}

/**
 * @param currentCount:当前重复次数，等于-1时先放大再抖动，默认等于-1
 * @param repeatCount:抖动次数，抖动完成后把view.scaleX恢复成原来的倍数，默认抖动3次
 * @param animationEnd:动画结束回调
 * @param rotateEnd:抖动动画结束回调
 */
fun View.shakeAnimation(currentCount: Int = -1, repeatCount: Int = 3, rotateEnd: () -> Unit = {}, animationEnd: () -> Unit = {}) {
    var count = currentCount
    if (count == -1) {
        this.animate().scaleX(1.15f).scaleY(1.15f).withEndAction {
            shakeAnimation(0, repeatCount = repeatCount, rotateEnd = rotateEnd, animationEnd = animationEnd)
        }.start()
        return
    }
    if (count < repeatCount) {
        count++
        this.animate().rotation(20f).setDuration(50).withEndAction {
            this.animate().rotation(0f).setDuration(50).withEndAction {
                this.animate().rotation(-20f).setDuration(50).withEndAction {
                    this.animate().rotation(0f).setDuration(50).withStartAction {
                        rotateEnd.invoke()
                        shakeAnimation(currentCount = count, repeatCount = repeatCount, rotateEnd = {
                            if (count == repeatCount - 1) {
                                this.animate().scaleY(1.0f).scaleX(1.0f)
                                    .withEndAction {
                                        animationEnd.invoke()
                                    }
                                    .start()
                            }
                        }, animationEnd = animationEnd)
                    }.start()
                }.start()
            }.start()
        }.start()
    }
}

/**
 * 手指戳一戳动画
 */
fun View.pokeAnimation(animationStart: () -> Unit = {}, animationEnd: () -> Unit = {}, pokeRange: Float = 1f) {
    val translationValue = 10 * pokeRange
    this.animate().translationX(-translationValue).setDuration(100).withStartAction { animationStart.invoke() }.withEndAction {
        this.animate().translationX(0f).setDuration(100).withEndAction {
            this.animate().translationX(-translationValue).setDuration(100).withEndAction {
                this.animate().translationX(0f).setDuration(100).withEndAction {
                    animationEnd.invoke()
                }.start()
            }.start()
        }.start()
    }.start()
}

/**
 * 跟随手指戳一戳旋转动画
 */
fun View.pokeRotateAnimation(animationStart: () -> Unit = {}, animationEnd: () -> Unit = {}) {
    this.animate().rotation(30f).setDuration(80).withStartAction { animationStart.invoke() }.withEndAction {
        this.animate().rotation(0f).setDuration(80).withEndAction {
            this.animate().rotation(30f).setDuration(80).withEndAction {
                this.animate().rotation(0f).setDuration(80).withEndAction {
                    animationEnd.invoke()
                }.start()
            }.start()
        }.start()
    }.start()
}

fun View.shutterAnimation(currentCount: Int = -1, repeatCount: Int = 5, rotateEnd: () -> Unit = {}, animationEnd: () -> Unit = {}) {
    var count = currentCount

    if (currentCount == -1) {
        // 第一次的效果
        this.animate().alpha(1f).setDuration(100)
            .withEndAction {
                this.animate().alpha(0f).setDuration(300)
                    .withEndAction {
                        shutterAnimation(0, repeatCount = repeatCount, rotateEnd = rotateEnd, animationEnd = animationEnd)
                    }.start()
            }
            .start()
        return
    }

    if (count < repeatCount) {
        count++
        this.animate().alpha(1f).setDuration(50)
            .withEndAction {
                this.animate().alpha(0f).setDuration(50)
                    .withEndAction {
                        rotateEnd.invoke()
                        shutterAnimation(currentCount = count, repeatCount = repeatCount, rotateEnd = {
                            if (count == repeatCount - 1) {
                                this.animate().scaleY(1.0f).scaleX(1.0f)
                                    .withEndAction {
                                        animationEnd.invoke()
                                    }
                                    .start()
                            }
                        }, animationEnd = animationEnd)
                    }.start()
            }
            .start()
    }
}

fun String.getTextBounds(paint: Paint): Rect {
    val bounds = Rect()
    paint.getTextBounds(this, 0, length, bounds)
    return bounds
}

/**
 * 根布局 拓展方法，在根布局中检查有没目标子控件，如果没有，就从隐藏控件的 viewStub inflate 里面取到
 * @param rootView 可以是根布局
 * @param targetViewId 隐藏控件中的一个子控件
 */
fun <T : View> View?.checkInflated(viewStub: ViewStub?, @IdRes targetViewId: Int): T? {
    var targetView = this?.findViewById<T>(targetViewId)
    if (targetView == null && viewStub != null) {
        val viewInflated = viewStub.inflate()
        return viewInflated.findViewById<T>(targetViewId)
    }
    return targetView
}

fun tryUntilTimeout(timeout: Long, block: () -> Boolean) {
    var successful: Boolean
    var totalSleepTime = 0
    do {
        successful = runCatching { block() }.getOrElse { false }
        if (!successful) {
            Thread.sleep(16)
            totalSleepTime += 16
        }
        if (totalSleepTime >= timeout) {
            break
        }
    } while (!successful)
}
