package com.binbo.glvideo.core.utils

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.hardware.Camera
import android.media.AudioManager
import android.os.*
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.format.Formatter
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.binbo.glvideo.core.GLVideo.Core.context
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.net.NetworkInterface
import java.net.SocketException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


object DeviceUtil {
    private val TAG = DeviceUtil::class.java.canonicalName
    private const val UNKNOW = "unknow"

    /**
     * 获取androidId
     *
     * @param context
     * @return
     */
    @SuppressLint("HardwareIds")
    fun getAndroidId(context: Context): String {
        var andoird_id = ""
        try {
            andoird_id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return andoird_id
    }

    fun getUniqueId(context: Context): String {
        return try {
            val androidID = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            Log.i("xxxxx", "xxxxxx androidID = $androidID")
            val id = androidID + Build.SERIAL
            toMD5(id)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            "unknown" + System.currentTimeMillis()
        }
    }

    fun byte2hex(b: ByteArray): String {
        var hs = StringBuffer(b.size)
        var stmp = ""
        val len = b.size
        for (n in 0 until len) {
            stmp = Integer.toHexString(b[n].toInt() and 0xFF)
            hs = if (stmp.length == 1) hs.append("0").append(stmp) else {
                hs.append(stmp)
            }
        }
        return hs.toString()
    }

    //获取本地IP
    val localIpAddress: String?
        get() {
            try {
                val en = NetworkInterface
                    .getNetworkInterfaces()
                while (en.hasMoreElements()) {
                    val intf = en.nextElement()
                    val enumIpAddr = intf
                        .inetAddresses
                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress = enumIpAddr.nextElement()
                        if (!inetAddress.isLoopbackAddress) {
                            return inetAddress.hostAddress
                        }
                    }
                }
            } catch (ex: SocketException) {
                Log.i("WifiPreference IpAddress", ex.toString())
            }
            return null
        }

    /**
     * 分配内存大小 单位：以 B 为单位
     *
     * @return
     */
    val memory: String
        get() = try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            val result = info.availMem.toString() + "@" + info.totalMem
            Log.i(TAG, "getMemory()---  result = $result")
            result
        } catch (e: Exception) {
            ""
        }//总共的block数//每格所占的大小，一般是4KB==

    /**
     * 得到内存大小, 单位 b
     *
     * @return
     */
    val totalStorage: Long
        get() {
            return try {
                val statFs = StatFs(Environment.getExternalStorageDirectory().path)
                val size = statFs.blockSizeLong //每格所占的大小，一般是4KB==
                val totalCounts = statFs.blockCountLong //总共的block数
                val tstorage = totalCounts * size
                Log.i(TAG, "getTotalStorage()---  tstorage = $tstorage")
                tstorage
            } catch (e: Exception) {
                0
            }
        }//获取可用的block数//每格所占的大小，一般是4KB==

    /**
     * 得到档期手机可用内存大小, 单位 b
     *
     * @return
     */
    val freeStorage: Long
        get() {
            return try {
                val statFs = StatFs(Environment.getExternalStorageDirectory().path)
                val size = statFs.blockSizeLong //每格所占的大小，一般是4KB==
                val availableCounts = statFs.availableBlocksLong //获取可用的block数
                val fstorage = availableCounts * size
                Log.i(TAG, "getFreeStorage()---  fstorage = $fstorage")
                fstorage
            } catch (e: Exception) {
                0
            }
        }//ToastUtil.showMessage("电量百分比 = "+batterylevel,1);

    /**
     * 电量百分比
     *
     * @return
     */
    @get:RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    val battery: Int
        get() {
            Log.i("DeviceUtils", "getBattery()---  come in")
            try {
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                //ToastUtil.showMessage("电量百分比 = "+batterylevel,1);
                return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            } catch (e: Exception) {
                Log.i("DeviceUtils", "getBattery()---  Exception " + e.message)
            }
            return 0
        }//ToastUtil.showMessage("获取充电状态 Exception",1);//ToastUtil.showMessage("Unplugged未充电",1);//ToastUtil.showMessage("Charging充电中",1);//如果设备正在充电，可以提取当前的充电状态和充电方式（无论是通过 USB 还是交流充电器），如下所示：
    // Are we charging / charged?

    /**
     * 获取音量  范围（0-15）
     *
     * @return
     */
    val systemVolume: Int
        get() {
            return try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            } catch (e: Exception) {
                0
            }
        }
    val isSilentMode: Boolean
        get() {
            return try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val ringerMode = audioManager.ringerMode
                Log.i(TAG, "isSilentMode()---   ringerMode = $ringerMode")
                ringerMode == AudioManager.RINGER_MODE_SILENT
            } catch (e: Exception) {
                Log.i(TAG, "isSilentMode()---   Exception " + e.message)
                true
            }
        }// Failed to get network operator name from network
    //return null;
    /**
     * 参考 com.amplitude.api.DeviceInfo
     * 获取网络运营商：中国电信,中国移动,中国联通
     *
     * @return
     */
    val carrier: String?
        get() {
            var carrier: String = UNKNOW
            try {
                val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                carrier = manager.networkOperatorName
            } catch (e: Exception) {
                // Failed to get network operator name from network
            }
            Log.i("LOGOUT_TAG", "sync()---  getCarrier()---  carrier = $carrier")
            if (carrier == null) {
                carrier = UNKNOW
            }
            return carrier
            //return null;
        }

    /**
     * Android设备物理唯一标识符
     *
     * @return
     */
    val psuedoUniqueID: String
        get() {
            var devIDShort = "35" + Build.BOARD.length % 10 + Build.BRAND.length % 10
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                devIDShort += Build.SUPPORTED_ABIS[0].length % 10
            } else {
                devIDShort += Build.CPU_ABI.length % 10
            }
            devIDShort += Build.DEVICE.length % 10 + Build.MANUFACTURER.length % 10 + Build.MODEL.length % 10 + Build.PRODUCT.length % 10
            var serial: String
            try {
                serial = Build::class.java.getField("SERIAL")[null].toString()
                return UUID(devIDShort.hashCode().toLong(), serial.hashCode().toLong()).toString()
            } catch (e: Exception) {
                serial = "ESYDV000"
            }
            return UUID(devIDShort.hashCode().toLong(), serial.hashCode().toLong()).toString()
        }

    @Throws(NoSuchAlgorithmException::class)
    private fun toMD5(text: String): String {
        //获取摘要器 MessageDigest
        val messageDigest = MessageDigest.getInstance("MD5")
        //通过摘要器对字符串的二进制字节数组进行hash计算
        val digest = messageDigest.digest(text.toByteArray())
        val sb = StringBuilder()
        for (i in digest.indices) {
            //循环每个字符 将计算结果转化为正整数;
            val digestInt = digest[i].toInt() and 0xff
            //将10进制转化为较短的16进制
            val hexString = Integer.toHexString(digestInt)
            //转化结果如果是个位数会省略0,因此判断并补0
            if (hexString.length < 2) {
                sb.append(0)
            }
            //将循环结果添加到缓冲区
            sb.append(hexString)
        }
        //返回整个结果
        return sb.toString()
    }

    /**
     * 得到设备屏幕的宽度
     */
    fun getScreenWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    /**
     * 得到设备屏幕的高度
     */
    fun getScreenHeight(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    /**
     * 获取虚拟功能键高度
     *
     * @param context
     * @return
     */
    fun getVirtualBarHeight(context: Context): Int {
        var vh = 0
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val dm = DisplayMetrics()
        try {
            val c = Class.forName("android.view.Display")
            val method = c.getMethod("getRealMetrics", DisplayMetrics::class.java)
            method.invoke(display, dm)
            vh = dm.heightPixels - windowManager.defaultDisplay.height
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return vh
    }

    val isSmallScreen: Boolean
        get() {
            val dm: DisplayMetrics = context.getResources().getDisplayMetrics()
            val heightInDp = Math.round(dm.heightPixels / dm.density)
            return heightInDp <= 600
        }

    /**
     * 获取状态栏高度
     */
    fun getStatusBarHeight(context: Context): Int {
        try {
            val c = Class.forName("com.android.internal.R\$dimen")
            val obj = c.newInstance()
            val field = c.getField("status_bar_height")
            val x = field[obj].toString().toInt()
            return context.resources.getDimensionPixelSize(x)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    /**
     * 得到设备的密度
     */
    fun getScreenDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    fun getScreenDensityDpi(context: Context): Float {
        return context.resources.displayMetrics.densityDpi.toFloat()
    }

    /**
     * 根据手机分辨率从dp转成px
     *
     * @param context
     * @param dpValue
     * @return
     */
    fun dip2px(context: Context, dpValue: Float): Int {
        val scale = getScreenDensity(context)
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 根据手机分辨率从dp转成px
     *
     * @param dpValue
     * @return
     */
    fun dip2px(dpValue: Float): Int {
        val scale = getScreenDensity(context)
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    fun px2dip(context: Context, pxValue: Float): Int {
        val scale = getScreenDensity(context)
        return (pxValue / scale + 0.5f).toInt() - 15
    }

    /**
     * 根据手机分辨率从dp转成px
     *
     * @param context
     * @param dpValue
     * @return
     */
    fun dip2pxF(context: Context, dpValue: Float): Float {
        val scale = getScreenDensity(context)
        return dpValue * scale + 0.5f
    }

    fun dip2pxF(dpValue: Float): Float {
        val scale = getScreenDensity(context)
        return dpValue * scale + 0.5f
    }

    /**
     * 根据手机分辨率从sp转成px
     *
     * @param context
     * @param spValue
     * @return
     */
    fun sp2pxF(context: Context, spValue: Float): Float {
        val scale = getScreenDensity(context)
        return spValue * scale + 0.5f
    }

    // 计算出该TextView中文字的长度(像素)
    fun getTextViewLength(textView: TextView, text: String?): Float {
        val paint = textView.paint
        paint.textSize = textView.textSize
        return paint.measureText(text)
    }

    @TargetApi(19)
    private fun setTranslucentStatus(activity: Activity, on: Boolean) {
        val win = activity.window
        val winParams = win.attributes
        val bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }

    //获取国家语言
    fun getCountry(mContext: Context): String {
        val locale = mContext.resources.configuration.locale
        val language = locale.language
        val country = Locale.getDefault().country
        return if (language != null && !(language == "")) {
            "$language-$country"
        } else {
            "en_US"
        }
    }

    /**
     * 获取android当前可用运行内存大小
     *
     * @param context
     * @return
     */
    fun getAvailMemory(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        // mi.availMem; 当前系统的可用内存
//        return Formatter.formatFileSize(context, mi.availMem);// 将获取的内存大小规格化
        return mi.availMem / (1024L * 1024 * 1024)
    }

    val isLowRunMemory: Boolean
        get() {
            if (getAvailMemory(context) < 2) {
                Log.i(TAG, "isLowRunMemory()---    true")
                return true
            }
            Log.i(TAG, "isLowRunMemory()---    false")
            return false
        }

    /**
     * 获取android总运行内存大小
     *
     * @param context
     */
    fun getTotalMemory(context: Context?): String {
        val str1 = "/proc/meminfo" // 系统内存信息文件
        val str2: String
        val arrayOfString: Array<String>
        var initial_memory: Long = 0
        try {
            val localFileReader = FileReader(str1)
            val localBufferedReader = BufferedReader(localFileReader, 8192)
            str2 = localBufferedReader.readLine() // 读取meminfo第一行，系统总内存大小
            arrayOfString = str2.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (num in arrayOfString) {
                Log.i(str2, num + "\t")
            }
            // 获得系统总内存，单位是KB
            val i = Integer.valueOf(arrayOfString[1]).toInt()
            //int值乘以1024转换为long类型
            initial_memory = i.toLong() * 1024
            localBufferedReader.close()
        } catch (e: IOException) {
        }
        return Formatter.formatFileSize(context, initial_memory) // Byte转换为KB或者MB，内存大小规格化
    }

    /**
     * 参考 com.amplitude.api.DeviceInfo
     *
     * @return
     */
    val language: String
        get() {
            var language: String
            language = try {
                Resources.getSystem().configuration.locales[0].language
            } catch (e: Exception) {
                Locale.getDefault().language
            }
            Log.i("LOGOUT_TAG", "sync()---  getLanguage()--- language = $language")
            return language
        }

    fun secToTime(time: Int): String {
        var timeStr: String? = null
        var hour = 0
        var minute = 0
        var second = 0
        if (time <= 0) return "00:00:00" else {
            minute = time / 60
            if (minute < 60) {
                second = time % 60
                timeStr = "00:" + unitFormat(minute) + ":" + unitFormat(second)
            } else {
                hour = minute / 60
                if (hour > 99) return "99:59:59"
                minute = minute % 60
                second = time - hour * 3600 - minute * 60
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second)
            }
        }
        return timeStr
    }

    fun unitFormat(i: Int): String {
        var retStr: String? = null
        retStr = if (i >= 0 && i < 10) "0$i" else "" + i
        return retStr
    }

    fun getOptions(scale: Float): BitmapFactory.Options {
        val options = BitmapFactory.Options()
        options.inSampleSize = scale.toInt()
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return options
    }

    fun getVerCode(context: Context): Int {
        var verCode = -1
        try {
            verCode = context.packageManager.getPackageInfo(
                context.packageName, 0
            ).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("getVerCode", e.message!!)
        }
        return verCode
    }

    fun getVerCodeName(context: Context): String {
        var verCode = "1.3.2"
        verCode = try {
            context.packageManager.getPackageInfo(
                context.packageName, 0
            ).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("getVerCode", e.message!!)
            return verCode
        } catch (e: Exception) {
            return verCode
        }
        return verCode
    }

    /**
     * 设置摄像头生成的图片角度
     *
     * @param activity
     * @param cameraId
     * @param camera
     */
    fun setCameraPictureOrientation(activity: Activity, cameraId: Int, camera: Camera) {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        val parameters = camera.parameters
        val rotation = activity.windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        val result: Int = if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            (info.orientation - degrees + 360) % 360
        } else {
            (info.orientation + degrees) % 360
        }
        parameters.setRotation(result)
        camera.parameters = parameters
    }

    /**
     * 手机振动效果
     */
    @JvmOverloads
    fun doVibrator(milliseconds: Long = 300L) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(milliseconds)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 该方法需要在View完全被绘制出来之后调用，否则判断不了
    //在比如 onWindowFocusChanged（）方法中可以得到正确的结果
    @Volatile
    private var mHasCheckAllScreen = false

    @Volatile
    private var mIsAllScreenDevice = false

    fun isAllScreenDevice(context: Context): Boolean {
        if (mHasCheckAllScreen) {
            return mIsAllScreenDevice
        }
        mHasCheckAllScreen = true
        mIsAllScreenDevice = false
        // 低于 API 21的，都不会是全面屏。。。
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false
        }
        val windowManager: WindowManager? = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (windowManager != null) {
            val display = windowManager.defaultDisplay
            val point = Point()
            display.getRealSize(point)
            val width: Float
            val height: Float
            if (point.x < point.y) {
                width = point.x.toFloat()
                height = point.y.toFloat()
            } else {
                width = point.y.toFloat()
                height = point.x.toFloat()
            }
            if (height / width >= 1.97f) {
                mIsAllScreenDevice = true
            }
        }
        return mIsAllScreenDevice
    }

    fun isNavigationBarExist(activity: Activity?, onNavigationStateListener: OnNavigationStateListener?) {
        if (activity == null) {
            return
        }
        val height = getNavigationHeight(activity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            activity.window.decorView.setOnApplyWindowInsetsListener { v, windowInsets ->
                var isShowing = false
                var b = 0
                if (windowInsets != null) {
                    b = windowInsets.systemWindowInsetBottom
                    isShowing = (b == height)
                }
                if (onNavigationStateListener != null && b <= height) {
                    onNavigationStateListener.onNavigationState(isShowing, b)
                }
                windowInsets
            }
        }
    }

    fun getNavigationHeight(activity: Context?): Int {
        if (activity == null) {
            return 0
        }
        val resources = activity.resources
        val resourceId = resources.getIdentifier(
            "navigation_bar_height",
            "dimen", "android"
        )
        var height = 0
        if (resourceId > 0) {
            //获取NavigationBar的高度
            height = resources.getDimensionPixelSize(resourceId)
        }
        return height
    }//Log.i(TAG,"getDeviceName()---   DeviceName = "+capitalize(manufacturer) + " " + model);

    //Log.i(TAG,"getDeviceName()---   DeviceName = "+capitalize(model));
    val deviceName: String
        get() {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.lowercase(Locale.getDefault()).startsWith(manufacturer.lowercase(Locale.getDefault()))) {
                //Log.i(TAG,"getDeviceName()---   DeviceName = "+capitalize(model));
                capitalizes(model)
            } else {
                //Log.i(TAG,"getDeviceName()---   DeviceName = "+capitalize(manufacturer) + " " + model);
                capitalizes(manufacturer) + " " + model
            }
        }

    fun capitalizes(s: String?): String {
        if (s == null || s.length == 0) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            first.uppercaseChar().toString() + s.substring(1)
        }
    }

    fun supportKeyboardAnimation(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // android 11
            true
        } else false
    }

    /**
     * 是否有刘海屏
     *
     * @return
     */
    @SuppressLint("NewApi")
    fun hasNotchInScreen(activity: Activity): Boolean {
        // android  P 以上有标准 API 来判断是否有刘海屏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val displayCutout = activity.window.decorView.rootWindowInsets.displayCutout
                if (displayCutout != null) {
                    // 说明有刘海屏
                    return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
        return false
    }

    /**
     * 获取statusbar高度。大于android9才能获取，否者返回0
     *
     * @return
     */
    fun getStatusBarHeightForAndroid9(activity: Activity): Int {
        return if (hasNotchInScreen(activity)) {
            val frame = Rect()
            activity.window.decorView.getWindowVisibleDisplayFrame(frame)
            frame.top
        } else {
            0
        }
    }

    interface OnNavigationStateListener {
        fun onNavigationState(isShowing: Boolean, height: Int)
    }
}