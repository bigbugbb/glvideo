package com.binbo.glvideo.sample_app.utils

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.annotation.AnyThread
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.binbo.glvideo.core.ext.isMainThread
import com.binbo.glvideo.sample_app.App
import io.reactivex.rxjava3.disposables.Disposable

fun Context.toast(message: String, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(applicationContext, message, duration).show()
}

fun Context.getColorCompat(@ColorRes resId: Int): Int {
    return ContextCompat.getColor(this, resId)
}

fun Context.getStringSafe(@StringRes resId: Int, vararg formatArgs: Any?, returnStringWhenError: String = ""): String {
    return kotlin.runCatching {
        getString(resId, *formatArgs)
    }.getOrElse {
        returnStringWhenError
    }
}

fun <T> FragmentActivity.replaceViewWithFragment(viewId: Int, clazz: Class<T>, args: Bundle = bundleOf(), tag: String = clazz.name) where T : Fragment {
    var fragment = supportFragmentManager.findFragmentByTag(tag) as? T
    if (fragment != null) {
        supportFragmentManager.beginTransaction().show(fragment).commit()
    } else {
        fragment = clazz.newInstance().apply {
            arguments = args
        }
        supportFragmentManager.beginTransaction()
            .replace(viewId, fragment, tag)
            .commit()
    }
}

@AnyThread
inline fun <reified T> MutableLiveData<T>.set(value: T) {
    if (isMainThread()) {
        this.value = value
    } else {
        this.postValue(value)
    }
}

@AnyThread
inline fun <reified T> MutableLiveData<T>.setNext(map: (T) -> T) {
    set(map(verifyLiveDataNotEmpty()))
}

@AnyThread
inline fun <reified T> LiveData<T>.verifyLiveDataNotEmpty(): T {
    return value ?: throw NullPointerException("MutableLiveData<${T::class.java}> not contain value.")
}

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T?) {
            observer.onChanged(t)
            removeObserver(this)
        }
    })
}

fun <T> LifecycleOwner.observe(liveData: LiveData<T>, observer: (t: T) -> Unit) {
    liveData.observe(this, Observer { it?.let { t -> observer(t) } })
}

fun <T> LifecycleOwner.observeOnce(liveData: LiveData<T>, observer: (t: T) -> Unit) {
    liveData.observeOnce(this, Observer { it?.let { t -> observer(t) } })
}

fun Disposable.bindToLifecycleOwner(owner: LifecycleOwner) {
    object : BaseSimpleLifecycleObserver(owner) {
        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            Log.e("bindToRxBusLifecycle", "onDestroy")
            dispose()
        }
    }
}

fun doClickVibrator(millis: Long = 120L, effectId: Int = VibrationEffect.EFFECT_HEAVY_CLICK, amplitude: Int = 50) {
    runCatching {
        val vibrator = App.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                vibrator.vibrate(VibrationEffect.createPredefined(effectId))
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                vibrator.vibrate(VibrationEffect.createOneShot(millis, amplitude))
            }
            else -> {
                vibrator.vibrate(millis)
            }
        }
    }
}