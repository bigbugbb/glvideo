package com.binbo.glvideo.core.media.config

data class MediaEncoderConfig(
    val width: Int = 1080,
    val height: Int = 1920,
    val frameRate: Int = 25,
    val videoBitRate: Int = 800000,
    val audioBitRate: Int = 64000,
    val keyFrameInterval: Int = 12,
    val sampleRate: Int = 44100,
    val encoderName: String = ""
)