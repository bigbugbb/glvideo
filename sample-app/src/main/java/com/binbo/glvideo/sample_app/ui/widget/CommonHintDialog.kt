package com.binbo.glvideo.sample_app.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import com.binbo.glvideo.core.ext.setVisible
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.DialogCommonHintLayoutBinding


class CommonHintDialog @JvmOverloads constructor(
    context: Context
) : BaseDialog(context, R.style.commonConfirmDialog) {

    private var _binding: DialogCommonHintLayoutBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    var onPositiveClick: (() -> Unit)? = null
    var onNegativeClick: (() -> Unit)? = null
    var onDialogDismissDoNothing: (() -> Unit)? = null

    var clickNNothing = true

    // true 点击对话框透明处，对话框消失
    var cancelOnTouchOutside: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DialogCommonHintLayoutBinding.inflate(layoutInflater, null, false)
        val view = binding.root

        setContentView(view)

        binding.dialogNoTv.singleClick {
            clickNNothing = false
            dismiss()
            onNegativeClick?.invoke()
        }
        binding.dialogYesTv.singleClick {
            clickNNothing = false
            dismiss()
            onPositiveClick?.invoke()
        }
        view.singleClick {
            if (cancelOnTouchOutside) {
                dismiss()
            }
        }
        setCanceledOnTouchOutside(cancelOnTouchOutside)
        initRipple()
    }

    fun showDialog(
        titleStr: String,
        noStr: String,
        yesStr: String,
        titleColor: Int = Color.WHITE,
        noColor: Int = Color.WHITE,
        yesColor: Int = Color.WHITE,
        canceledOnTouchOutside: Boolean = true, // true 点击对话框透明处，对话框消失
        mainTitle: String = "",
        marginHorizontal: Int = 0//dip(64)
    ): CommonHintDialog {
        showFullScreen()
        if (marginHorizontal > 0) {
            val lpViewDialogContent = binding.viewDialogContent.layoutParams as FrameLayout.LayoutParams
            lpViewDialogContent.leftMargin = marginHorizontal
            lpViewDialogContent.rightMargin = marginHorizontal
            binding.viewDialogContent.layoutParams = lpViewDialogContent
        }
        binding.textTitle.setVisible(mainTitle.isNotBlank())
        binding.textTitle.text = mainTitle
        binding.dialogYesOrNoTitleTv.text = titleStr
        binding.dialogNoTv.text = noStr
        binding.dialogYesTv.text = yesStr

        binding.dialogYesOrNoTitleTv.setTextColor(titleColor)
        binding.dialogNoTv.setTextColor(noColor)
        binding.dialogYesTv.setTextColor(yesColor)

        cancelOnTouchOutside = canceledOnTouchOutside
        return this
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun initRipple() {
        binding.dialogYesTv.foreground = context.getDrawable(R.drawable.common_hint_ripple)
        binding.dialogNoTv.foreground = context.getDrawable(R.drawable.common_hint_ripple)
    }

    override fun dismiss() {
        super.dismiss()
        if (clickNNothing) {
            onDialogDismissDoNothing?.invoke()
        }
    }

}