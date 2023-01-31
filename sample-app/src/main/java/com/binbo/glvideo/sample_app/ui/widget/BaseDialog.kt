package com.binbo.glvideo.sample_app.ui.widget

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import com.binbo.glvideo.core.utils.DeviceUtil.getScreenHeight
import com.binbo.glvideo.core.utils.DeviceUtil.getScreenWidth

open class BaseDialog : Dialog {
    private val TAG = "BaseDialog"

    constructor(context: Context) : super(context) {}
    constructor(context: Context, themeResId: Int) : super(context, themeResId) {}

    protected constructor(context: Context, cancelable: Boolean, cancelListener: DialogInterface.OnCancelListener?) : super(
        context,
        cancelable,
        cancelListener
    ) {
    }

    fun showFullScreen() {
        show()
        val window = window
        val lp = window!!.attributes
        //lp.height = Utils.screenH;
        lp.width = Math.min(getScreenHeight(context), getScreenWidth(context))
        window.attributes = lp
    }
}