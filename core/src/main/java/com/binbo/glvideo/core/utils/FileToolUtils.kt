package com.binbo.glvideo.core.utils

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.IntDef
import com.binbo.glvideo.core.GLVideo.Core.context
import com.binbo.glvideo.core.GLVideo.Core.fileMainPath
import com.binbo.glvideo.core.ext.now
import com.binbo.glvideo.core.utils.FileUseCase.Companion.ADD_WATERMARK
import com.binbo.glvideo.core.utils.FileUseCase.Companion.FACE_TRACKER_MODEL
import com.binbo.glvideo.core.utils.FileUseCase.Companion.FACE_TRACKER_SEETA
import com.binbo.glvideo.core.utils.FileUseCase.Companion.GIF_TO_MP4
import com.binbo.glvideo.core.utils.FileUseCase.Companion.MISSION_CARD_VIDEO
import com.binbo.glvideo.core.utils.FileUseCase.Companion.NAME_CARD_VIDEO
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PICTURE_TAKING
import com.binbo.glvideo.core.utils.FileUseCase.Companion.TEST_ONLY
import com.binbo.glvideo.core.utils.FileUseCase.Companion.VIDEO_BGM
import com.binbo.glvideo.core.utils.FileUseCase.Companion.VIDEO_CUT
import com.binbo.glvideo.core.utils.FileUseCase.Companion.VIDEO_RECORDING
import com.binbo.glvideo.core.utils.FileUseCase.Companion.VIDEO_WITHOUT_BGM
import com.binbo.glvideo.core.utils.FileUseCase.Companion.VIDEO_WITH_BGM
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileToolUtils {

    const val TAG = "FileToolUtils"

    const val DEFAULT_PHOTO_EXTENSION = ".jpg"
    const val DEFAULT_VIDEO_EXTENSION = ".mp4"

    @JvmStatic
    fun generateTempFile(fileName: String): File {
        return if (Build.VERSION.SDK_INT >= 30) {
            Log.i(TAG, "generalTempFile()---   Build.VERSION.SDK_INT 30 以上")
            val filesDir = File(context.filesDir, ".temp")
            if (!filesDir.exists()) {
                filesDir.mkdirs()
            }
            File(filesDir, fileName)
        } else {
            Log.i(TAG, "generalTempFile()---   Build.VERSION.SDK_INT 30 以下")
            val dir = File(fileMainPath, ".temp")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            File(dir, fileName)
        }
    }

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
            FACE_TRACKER_MODEL -> "face_tracker/model"
            FACE_TRACKER_SEETA -> "face_tracker/seeta"
            PICTURE_TAKING -> "picture_taking"
            VIDEO_RECORDING -> "video_recording"
            GIF_TO_MP4 -> "gif_to_mp4"
            VIDEO_BGM -> "video_bgm"
            VIDEO_WITHOUT_BGM -> "video_without_bgm"
            VIDEO_WITH_BGM -> "video_with_bgm"
            ADD_WATERMARK -> "add_watermark"
            VIDEO_CUT -> "video_cut"
            else -> "test"
        }
        return if (useNewSdk) path else "$fileMainPath/$path"
    }

    @JvmStatic
    fun writeVideoToGallery(
        videoFile: File,
        mimeType: String,
        onSuccess: (String) -> Unit = {},
        fileExtension: String = DEFAULT_VIDEO_EXTENSION
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
}

@IntDef(
    FACE_TRACKER_MODEL, FACE_TRACKER_SEETA,
    NAME_CARD_VIDEO, MISSION_CARD_VIDEO,
    PICTURE_TAKING,
    VIDEO_RECORDING,
    GIF_TO_MP4,
    VIDEO_BGM,
    VIDEO_WITHOUT_BGM,
    VIDEO_WITH_BGM,
    ADD_WATERMARK,
    VIDEO_CUT,
    TEST_ONLY
)
@Retention(AnnotationRetention.SOURCE)
annotation class FileUseCase {

    companion object {
        const val FACE_TRACKER_MODEL = 0
        const val FACE_TRACKER_SEETA = 1
        const val NAME_CARD_VIDEO = 10
        const val MISSION_CARD_VIDEO = 20
        const val PICTURE_TAKING = 30
        const val VIDEO_RECORDING = 40
        const val GIF_TO_MP4 = 60
        const val VIDEO_BGM = 70
        const val VIDEO_WITHOUT_BGM = 71
        const val VIDEO_WITH_BGM = 72
        const val ADD_WATERMARK = 80
        const val VIDEO_CUT = 100
        const val TEST_ONLY = 200
    }
}