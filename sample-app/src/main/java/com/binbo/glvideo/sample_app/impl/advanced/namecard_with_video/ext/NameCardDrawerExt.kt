package com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.ext

import android.graphics.*
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.binbo.glvideo.core.ext.dip
import com.binbo.glvideo.core.ext.getViewBitmap
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.utils.thirdparty.GlideApp


fun createAvatarBitmap(): Bitmap? {
    val rootView = LayoutInflater.from(context).inflate(R.layout.layout_name_card_with_video_avatar, null)
    val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(dip(180f).toInt(), View.MeasureSpec.EXACTLY)
    val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(dip(180f).toInt(), View.MeasureSpec.EXACTLY)
    rootView.measure(widthMeasureSpec, heightMeasureSpec)
    val measuredWidth = rootView.measuredWidth
    val measuredHeight = rootView.measuredHeight
    rootView.layout(0, 0, measuredWidth, measuredHeight)
    val avatarBitmap = runCatching {
        GlideApp.with(context).asBitmap()
            .load(R.drawable.my_avatar)
            .error(R.drawable.base_avatar_placeholder_white)
            .placeholder(R.drawable.base_avatar_placeholder_white)
            .submit()
            .get()
    }.getOrNull()
    val imageView = rootView.findViewById<ImageView>(R.id.imageAvatar)
    if (avatarBitmap != null) {
        imageView.setImageBitmap(avatarBitmap)
    } else {
        imageView.setImageResource(R.drawable.base_avatar_placeholder_white)
    }
    return rootView.getViewBitmap()
}

fun createTitleBitmap(typeface: Typeface): Bitmap? {
    val rootView = LayoutInflater.from(context).inflate(R.layout.layout_name_card_with_video_title, null)
    val textView = rootView.findViewById<TextView>(R.id.textNameCardTitle)
    textView.typeface = typeface
    val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
    val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
    rootView.measure(widthMeasureSpec, heightMeasureSpec)
    val measuredWidth = rootView.measuredWidth
    val measuredHeight = rootView.measuredHeight
    rootView.layout(0, 0, measuredWidth, measuredHeight)
    return rootView.getViewBitmap()
}

fun createFooterBitmap(): Bitmap? {
    val rootView = LayoutInflater.from(context).inflate(R.layout.layout_name_card_with_video_footer, null)
    val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
    val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
    rootView.measure(widthMeasureSpec, heightMeasureSpec)
    val measuredWidth = rootView.measuredWidth
    val measuredHeight = rootView.measuredHeight
    rootView.layout(0, 0, measuredWidth, measuredHeight)
    return rootView.getViewBitmap()
}

fun createBlurredElementBitmap(resId: Int): Bitmap? {
    val rootView = LayoutInflater.from(context).inflate(resId, null)
    val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
    val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
    rootView.measure(widthMeasureSpec, heightMeasureSpec)
    val measuredWidth = rootView.measuredWidth
    val measuredHeight = rootView.measuredHeight
    rootView.layout(0, 0, measuredWidth, measuredHeight)
    return rootView.getViewBitmap()
}

fun createTextAsBitmap(
    text: String,
    textSize: Float,
    textColor: Int,
    typeface: Typeface,
    heightInScene: Float = -1f,
    maxWidthAllowedInScene: Float = 100f,
): Bitmap? {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.textSize = textSize
    paint.isDither = true
    paint.isFilterBitmap = true
    paint.typeface = typeface
    paint.color = textColor
    paint.textAlign = Paint.Align.LEFT
    val baseline: Float = -paint.ascent() // ascent() is negative

    return runCatching {
        var updatedText = text
        var hasEllipse = false
        if (heightInScene > 0f) {
            var requireToAdjust = true
            while (requireToAdjust) {
                val bounds = Rect()
                val measuredText = if (hasEllipse) "$updatedText..." else updatedText
                paint.getTextBounds(measuredText, 0, measuredText.length, bounds)
                val widthInScene = bounds.width() / bounds.height() * heightInScene
                if (widthInScene > maxWidthAllowedInScene) {
                    updatedText = updatedText.substring(0, updatedText.lastIndex)
                    hasEllipse = true
                } else {
                    requireToAdjust = false
                }
            }
        }

        if (hasEllipse) {
            updatedText = "$updatedText..."
        }
        val imageWidth = (paint.measureText(updatedText) + dip(5f) + 0.5f).toInt()
        val imageHeight = (baseline + paint.descent() + 0.5f).toInt()
        val image = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawText(updatedText, dip(2f), baseline, paint)
        image
    }.getOrNull()
}