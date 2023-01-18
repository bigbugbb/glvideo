package com.binbo.glvideo.core.media

enum class DecodeState {
    /**开始状态*/
    START,

    /**解码中*/
    DECODING,

    /**解码暂停*/
    PAUSE,

    /**正在快进*/
    SEEKING,

    FLUSHING,

    /**解码完成*/
    FINISH,

    /**解码器释放*/
    STOP
}
