package com.binbo.glvideo.sample_app.utils

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.binbo.glvideo.core.ext.runOnMainThread
import com.binbo.glvideo.sample_app.App

fun Context.toast(message: String, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(applicationContext, message, duration).show()
}

fun Context.getColorCompat(@ColorRes resId: Int): Int {
    return ContextCompat.getColor(this, resId)
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