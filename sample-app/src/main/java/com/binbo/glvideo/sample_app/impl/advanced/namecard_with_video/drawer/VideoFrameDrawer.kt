package com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.drawer

import com.binbo.glvideo.core.opengl.drawer.VideoDrawer
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.cardVideoSize

/**
 * 绘制卡片上的视频到纹理
 */
class VideoFrameDrawer : VideoDrawer(cardVideoSize.width, cardVideoSize.height)