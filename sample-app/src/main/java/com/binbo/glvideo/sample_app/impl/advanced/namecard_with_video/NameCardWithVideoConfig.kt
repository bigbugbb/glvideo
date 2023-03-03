package com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video

import android.content.ContentResolver
import android.net.Uri
import android.util.Size
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.R
import java.io.File
import kotlin.math.abs

object NameCardWithVideoConfig {
    val cardVideoPath =
        ContentResolver.SCHEME_ANDROID_RESOURCE + File.pathSeparator + File.separator + File.separator + context.packageName + File.separator + R.raw.name_card_background
    val cardVideoUri = Uri.parse(cardVideoPath)
    val cardVideoSize = Size(720, 480) // 视频本身的size

    const val nameCardLeft = -0.6f
    const val nameCardRight = 0.6f
    const val nameCardTop = 0.83f
    const val nameCardBottom = -0.83f

    const val nameCardPaddingTop = 0.07f
    const val nameCardPaddingLeft = 0.07f
    const val nameCardPaddingBottom = 0.05f

    const val titleHeight = 0.1f
    const val nicknameHeight = 0.135f
    const val movingNicknameHeight = 0.35f
    const val userHandlerHeight = 0.1f
    const val footerHeight = 0.075f
    const val avatarRadius = 0.325f

    val nameCardAspectRatio: Float
        get() = (abs(nameCardLeft) + abs(nameCardRight)) / (abs(nameCardTop) + abs(nameCardBottom))
}