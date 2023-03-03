package com.binbo.glvideo.sample_app.impl.advanced.namecard

import android.util.Size
import kotlin.math.abs

object NameCardConfig {
    var countOfFriends = 0

    val recordVideoSize = Size(540, 960)  // 录制生成的视频size
    val recordVideoExt = ".mp4"

    val frameRate = 30

    const val missionCardLeft = -0.6f
    const val missionCardRight = 0.6f
    const val missionCardTop = 0.83f
    const val missionCardBottom = -0.83f

    const val missionCardPaddingTop = 0.07f
    const val missionCardPaddingLeft = 0.07f
    const val missionCardPaddingBottom = 0.05f

    const val titleHeight = 0.1f
    const val nicknameHeight = 0.14f
    const val movingNicknameHeight = 0.385f
    const val userHandlerHeight = 0.1f
    const val footerHeight = 0.075f
    const val avatarRadius = 0.325f
    const val sloganHeight = 0.75f
    const val watermarkHeight = 0.12f

    val missionCardAspectRatio: Float
        get() = (abs(missionCardLeft) + abs(missionCardRight)) / (abs(missionCardTop) + abs(missionCardBottom))
}