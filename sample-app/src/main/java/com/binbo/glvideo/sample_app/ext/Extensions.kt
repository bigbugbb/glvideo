package com.binbo.glvideo.sample_app.ext

import android.content.Context
import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

fun Context.getColorCompat(@ColorRes resId: Int): Int {
    return ContextCompat.getColor(this, resId)
}

fun <T> FragmentActivity.replaceViewWithFragment(viewId: Int, clazz: Class<T>, args: Bundle, tag: String) where T : Fragment {
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