package com.binbo.glvideo.core.graph.component

import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
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
 * @time 22:46
 */
abstract class VideoBgmPostProcessor(override val arguments: Bundle = bundleOf()) : IMediaPostProcessor<String> {

    private val TAG = "VideoBgmPostProcessor"

    suspend fun addBgmToVideo(srcFile: File, audioFile: File, dstFile: File, applyShortest: Boolean = true) =
        suspendCancellableCoroutine<String> { continuation ->
            kotlin.runCatching {
                val shortest = if (applyShortest) "-shortest" else ""
                val ffmpegCmd = "-i ${srcFile.absolutePath} -i ${audioFile.absolutePath} -map 0:v -map 1:a -c copy $shortest ${dstFile.absolutePath}"
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

    suspend fun addBgmToVideoFromAnother(srcFile: File, videoFileWithBgm: File, dstFile: File) = suspendCancellableCoroutine<String> { continuation ->
        kotlin.runCatching {
            val ffmpegCmd = "-i ${srcFile.absolutePath} -i ${videoFileWithBgm.absolutePath} -map 0:v -map 1:a -c copy ${dstFile.absolutePath}"
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