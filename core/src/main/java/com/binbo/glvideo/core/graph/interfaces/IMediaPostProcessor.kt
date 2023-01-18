package com.binbo.glvideo.core.graph.interfaces

import android.os.Bundle

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/8
 * @time 22:24
 */
interface IMediaPostProcessor<T> {
    val arguments: Bundle

    suspend fun process(args: Bundle): MResults<T>

    companion object {
        const val ARG_SRC_FILE_PATH = "arg_src_file_path"
        const val ARG_DST_FILE_PATH = "arg_dst_file_path"
        const val ARG_VIDEO_WIDTH = "arg_video_width"
        const val ARG_VIDEO_HEIGHT = "arg_video_height"
        const val ARG_VIDEO_FRAME_RATE = "arg_video_frame_rate"
    }
}

sealed class MResults<out T> {

    companion object {
        fun <T> success(result: T): MResults<T> = Success(result)
        fun <T> failure(error: Throwable): MResults<T> = Failure(error)
    }

    data class Failure(val error: Throwable) : MResults<Nothing>()
    data class Success<out T>(val data: T) : MResults<T>()
}