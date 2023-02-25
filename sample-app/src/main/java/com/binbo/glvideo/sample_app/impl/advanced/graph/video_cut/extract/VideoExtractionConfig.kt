package com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.extract

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/14
 * @time 16:50
 */
object VideoExtractionConfig {
    val extractWidth: Int = 72
    val extractHeight: Int = 128
    val maxCachedFrames: Int = 300

    val minExtractDuration = 2000000L // us
    val maxExtractDuration = 6000000L
}