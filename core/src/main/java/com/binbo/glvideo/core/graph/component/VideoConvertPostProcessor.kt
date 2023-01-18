package com.binbo.glvideo.core.graph.component

import android.os.Bundle
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.binbo.glvideo.core.graph.interfaces.IMediaPostProcessor
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resumeWithException

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/8
 * @time 22:47
 */
abstract class VideoConvertPostProcessor(override val arguments: Bundle) : IMediaPostProcessor<String> {

    private val TAG = "VideoConvertPostProcessor"

    suspend fun convertMp4ToWebP(srcFile: File, dstFile: File) = suspendCancellableCoroutine<String> { continuation ->
        kotlin.runCatching {
            val ffmpegCmd = "-i ${srcFile.absolutePath} -vcodec libwebp -filter:v fps=fps=20 -lossless 0 " +
                    "-compression_level 4 -q:v 75 -loop 0 -preset picture -an -vsync 0 -s 200:200 ${dstFile.absolutePath}"
            Log.d(TAG, "ffmpeg cmd: $ffmpegCmd")

            val session = FFmpegKit.execute(ffmpegCmd)
            if (ReturnCode.isSuccess(session.returnCode)) {
                Log.i(TAG, "Command execution completed successfully.")
                continuation.resume(dstFile.absolutePath, null)
            } else if (ReturnCode.isCancel(session.returnCode)) {
                Log.i(TAG, "Command execution cancelled by user.")
                continuation.resumeWithException(Exception("canceled"))
            } else {
                // FAILURE
                val msg = String.format("Command failed with state %s and rc %s.%s", session.state, session.returnCode, session.failStackTrace)
                Log.d(TAG, msg)
                continuation.resumeWithException(Exception(msg))
            }
        }.getOrElse {
            continuation.resumeWithException(it)
        }
    }
}