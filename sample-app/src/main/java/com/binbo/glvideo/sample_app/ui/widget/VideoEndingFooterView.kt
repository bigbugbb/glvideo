package com.binbo.glvideo.sample_app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.binbo.glvideo.sample_app.R


class VideoEndingFooterView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attributes, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_video_ending_footer, this, true)
        orientation = HORIZONTAL
    }
}