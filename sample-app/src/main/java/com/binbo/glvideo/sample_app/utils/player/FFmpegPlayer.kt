package com.binbo.glvideo.sample_app.utils.player

class FFmpegPlayer {
    companion object {
        init {
            System.loadLibrary("ffmpeg_player")
        }
    }
}