package com.binbo.glvideo.sample_app.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.annotation.IntDef
import com.binbo.glvideo.core.ext.now
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.App.Const.fileMainPath
import com.binbo.glvideo.sample_app.utils.FileUseCase.Companion.ADD_ENDING
import com.binbo.glvideo.sample_app.utils.FileUseCase.Companion.ADD_WATERMARK
import com.binbo.glvideo.sample_app.utils.FileUseCase.Companion.GIF_TO_MP4
import com.binbo.glvideo.sample_app.utils.FileUseCase.Companion.MISSION_CARD_VIDEO
import com.binbo.glvideo.sample_app.utils.FileUseCase.Companion.NAME_CARD_VIDEO
import com.binbo.glvideo.sample_app.utils.FileUseCase.Companion.PICTURE_TAKING
import com.binbo.glvideo.sample_app.utils.FileUseCase.Companion.VIDEO_BGM
import com.binbo.glvideo.sample_app.utils.FileUseCase.Companion.VIDEO_CUT
import com.binbo.glvideo.sample_app.utils.FileUseCase.Companion.VIDEO_RECORDING
import com.binbo.glvideo.sample_app.utils.FileUseCase.Companion.VIDEO_WITHOUT_BGM
import com.binbo.glvideo.sample_app.utils.FileUseCase.Companion.VIDEO_WITH_BGM
import java.io.*

object FileToolUtils {

    const val TAG = "FileToolUtils"

    @JvmStatic
    fun getFile(useCase: Int, filename: String = ""): File {
        val path = useCaseToFilePath(useCase)
        return if (Build.VERSION.SDK_INT >= 30) {
            val filesDir = File(context.filesDir, path)
            if (!filesDir.exists()) {
                filesDir.mkdirs()
            }
            File(filesDir, filename)
        } else {
            val storageDir = File(path)
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            File(storageDir, filename)
        }
    }

    private fun useCaseToFilePath(useCase: Int): String {
        val useNewSdk = Build.VERSION.SDK_INT >= 30
        val path = when (useCase) {
            NAME_CARD_VIDEO -> "name_card/video"
            MISSION_CARD_VIDEO -> "mission_card/video"
            PICTURE_TAKING -> "picture_taking"
            VIDEO_RECORDING -> "video_recording"
            GIF_TO_MP4 -> "gif_to_mp4"
            VIDEO_BGM -> "video_bgm"
            VIDEO_WITHOUT_BGM -> "video_without_bgm"
            VIDEO_WITH_BGM -> "video_with_bgm"
            ADD_WATERMARK -> "add_watermark"
            ADD_ENDING -> "add_ending"
            VIDEO_CUT -> "video_cut"
            else -> "test"
        }
        return if (useNewSdk) path else "$fileMainPath/$path"
    }

    @JvmStatic
    fun writeVideoToGallery(
        videoFile: File,
        mimeType: String,
        fileExtension: String = ".mp4",
        onSuccess: (String) -> Unit = {}
    ) {
        val videoTime = now
        val videoFileName = videoTime.toString() + fileExtension
        val stringId = context.applicationInfo.labelRes
        val appName = if (stringId == 0) context.applicationInfo.nonLocalizedLabel else context.getString(stringId)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + File.separator + appName)
            put(MediaStore.MediaColumns.DISPLAY_NAME, videoFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.DATE_ADDED, videoTime / 1000)
            put(MediaStore.MediaColumns.DATE_MODIFIED, videoTime / 1000)
            put(MediaStore.MediaColumns.DATE_EXPIRES, (videoTime + 86400000) / 1000)
            put(MediaStore.MediaColumns.IS_PENDING, true)
        }
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI?.let { contentUri ->
            val resolver = context.contentResolver
            resolver.insert(contentUri, values)?.let { uri ->
                kotlin.runCatching {
                    // First, write the actual data for our screenshot
                    resolver.openFileDescriptor(uri, "w").use { descriptor ->
                        descriptor?.let {
                            FileOutputStream(descriptor.fileDescriptor).use { out ->
                                FileInputStream(videoFile).use { inputStream ->
                                    val buf = ByteArray(8192)
                                    while (true) {
                                        val sz = inputStream.read(buf)
                                        if (sz <= 0) break
                                        out.write(buf, 0, sz)
                                    }
                                }
                            }
                        }
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    values.putNull(MediaStore.MediaColumns.DATE_EXPIRES)
                    resolver.update(uri, values, null, null)
                    onSuccess(videoFileName)
                }.getOrElse {
                    Log.i(TAG, "writeVideoToGallery()---  exception = ${it.message}")
                }
            }
        }
    }

    fun copyAssets(
        context: Context,
        filename: String,
        assetDir: String,
        destPath: String?,
        onSuccess: (File) -> Unit = {},
        onFailed: (Throwable) -> Unit = {}
    ) {
        val workingPath = File(destPath)
        // if this directory does not exists, make one.
        workingPath.mkdirs()
        var input: InputStream? = null
        var output: OutputStream? = null
        try {
            val outFile = File(workingPath, filename)
            if (outFile.exists()) outFile.delete()
            input = if (TextUtils.isEmpty(assetDir)) context.assets.open(filename) else context.assets.open(assetDir + File.separator + filename)
            output = FileOutputStream(outFile)

            // Transfer bytes from in to out
            val buf = ByteArray(8192)
            var len: Int
            while (input.read(buf).also { len = it } > 0) {
                output.write(buf, 0, len)
            }
            onSuccess(outFile)
        } catch (e: FileNotFoundException) {
            Log.i(TAG, "copyAssets() FileNotFoundException")
            onFailed(e)
        } catch (e: IOException) {
            Log.i(TAG, "copyAssets() IOException")
            onFailed(e)
        } catch (e: Exception) {
            onFailed(e)
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (output != null) {
                try {
                    output.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}

@IntDef(
    NAME_CARD_VIDEO,
    MISSION_CARD_VIDEO,
    PICTURE_TAKING,
    VIDEO_RECORDING,
    GIF_TO_MP4,
    VIDEO_BGM,
    VIDEO_WITHOUT_BGM,
    VIDEO_WITH_BGM,
    ADD_WATERMARK,
    ADD_ENDING,
    VIDEO_CUT
)
@Retention(AnnotationRetention.SOURCE)
annotation class FileUseCase {

    companion object {
        const val NAME_CARD_VIDEO = 10
        const val MISSION_CARD_VIDEO = 20
        const val PICTURE_TAKING = 30
        const val VIDEO_RECORDING = 40
        const val GIF_TO_MP4 = 60
        const val VIDEO_BGM = 70
        const val VIDEO_WITHOUT_BGM = 71
        const val VIDEO_WITH_BGM = 72
        const val ADD_WATERMARK = 80
        const val ADD_ENDING = 90
        const val VIDEO_CUT = 100
    }
}