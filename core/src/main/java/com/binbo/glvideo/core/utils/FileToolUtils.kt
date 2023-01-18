package com.binbo.glvideo.core.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.IntDef
import com.binbo.glvideo.core.GLVideo.Core.context
import com.binbo.glvideo.core.GLVideo.Core.fileMainPath
import com.binbo.glvideo.core.utils.FileUseCase.Companion.FACE_TRACKER_MODEL
import com.binbo.glvideo.core.utils.FileUseCase.Companion.FACE_TRACKER_SEETA
import com.binbo.glvideo.core.utils.FileUseCase.Companion.MEMORY_VIDEO_BGM
import com.binbo.glvideo.core.utils.FileUseCase.Companion.MEMORY_VIDEO_SHARE
import com.binbo.glvideo.core.utils.FileUseCase.Companion.MEMORY_VIDEO_SOURCE
import com.binbo.glvideo.core.utils.FileUseCase.Companion.MISSION_CARD_VIDEO
import com.binbo.glvideo.core.utils.FileUseCase.Companion.NAME_CARD_VIDEO
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PEEK_BGM
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PEEK_CONFIG_FRONT
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PEEK_CONFIG_REAR
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PEEK_FINAL_VIDEO_FRONT
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PEEK_FINAL_VIDEO_REAR
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PEEK_FINAL_VIDEO_SHARE
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PEEK_SOURCE_VIDEO_FRONT
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PEEK_SOURCE_VIDEO_REAR
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PEEK_SOURCE_VIDEO_SHARE
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PEEK_VIDEO_COVER_FRONT
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PEEK_VIDEO_COVER_REAR
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PEEK_VIDEO_COVER_SHARE
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PFP_FIRST_FRAME
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PFP_VIDEO
import com.binbo.glvideo.core.utils.FileUseCase.Companion.PFP_VIDEO_CROP
import com.binbo.glvideo.core.utils.FileUseCase.Companion.REGULAR_INVITE_VIDEO_SHARE
import com.binbo.glvideo.core.utils.FileUseCase.Companion.ROOM_PHOTO_AUDIO
import com.binbo.glvideo.core.utils.FileUseCase.Companion.ROOM_PHOTO_RENDER
import com.binbo.glvideo.core.utils.FileUseCase.Companion.ROOM_PHOTO_SEND
import com.binbo.glvideo.core.utils.FileUseCase.Companion.ROOM_PHOTO_VIDEO_SHARE
import com.binbo.glvideo.core.utils.FileUseCase.Companion.TAKE_PICTURE
import com.binbo.glvideo.core.utils.FileUseCase.Companion.TEST_ONLY
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileToolUtils {

    const val TAG = "FileToolUtils"

    const val ROOM_PHOTO_EXTENSION = ".jpg"
    const val ROOM_VIDEO_EXTENSION = ".mp4"

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
            ROOM_PHOTO_AUDIO -> "room_photo/audio"
            ROOM_PHOTO_RENDER -> "room_photo/render"
            ROOM_PHOTO_VIDEO_SHARE -> "room_photo/share"
            ROOM_PHOTO_SEND -> "room_photo/.send"
            NAME_CARD_VIDEO -> "name_card/video"
            MISSION_CARD_VIDEO -> "mission_card/video"
            PFP_VIDEO -> "pfp/video"
            PFP_VIDEO_CROP -> "pfp/video_crop"
            PFP_FIRST_FRAME -> "pfp/first_frame"
            PEEK_SOURCE_VIDEO_REAR -> "peek/video/r"
            PEEK_SOURCE_VIDEO_FRONT -> "peek/video/f"
            PEEK_SOURCE_VIDEO_SHARE -> "peek/video/s"
            PEEK_CONFIG_REAR -> "peek/config/r"
            PEEK_CONFIG_FRONT -> "peek/config/f"
            PEEK_FINAL_VIDEO_REAR -> "peek/final_video/r"
            PEEK_FINAL_VIDEO_FRONT -> "peek/final_video/f"
            PEEK_FINAL_VIDEO_SHARE -> "peek/final_video/s"
            PEEK_BGM -> "peek/bgm"
            PEEK_VIDEO_COVER_REAR -> "peek/cover/r"
            PEEK_VIDEO_COVER_FRONT -> "peek/cover/f"
            PEEK_VIDEO_COVER_SHARE -> "peek/cover/s"
            MEMORY_VIDEO_BGM -> "memory/video/bgm"
            MEMORY_VIDEO_SOURCE -> "memory/video/source"
            MEMORY_VIDEO_SHARE -> "memory/video/share"
            REGULAR_INVITE_VIDEO_SHARE -> "regular_invite/video"
            FACE_TRACKER_MODEL -> "face_tracker/model"
            FACE_TRACKER_SEETA -> "face_tracker/seeta"
            TAKE_PICTURE -> "take_picture"
            else -> "test"
        }
        return if (useNewSdk) path else "$fileMainPath/$path"
    }

    @JvmStatic
    fun deleteFile(filename: String) {
        kotlin.runCatching {
            Log.i(TAG, "deleteFile()---   fileName = $filename")
            val file = File(filename)
            file.deleteRecursively()
        }.getOrElse {
            Log.i(TAG, "deleteFile()---  Exception = ${it.message}")
        }
    }

    @JvmStatic
    fun saveVideoToGallery(
        filePath: String,
        mimeType: String,
        onSuccess: (String) -> Unit,
        fileExtension: String = ROOM_VIDEO_EXTENSION
    ) {
        val imageTime = System.currentTimeMillis()
        val imageFileName = imageTime.toString() + fileExtension
        val stringId = context.applicationInfo.labelRes
        val appName = if (stringId == 0) context.applicationInfo.nonLocalizedLabel else context.getString(stringId)
        val values = ContentValues()
        values.put(
            MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + File.separator + appName
        ) // Environment.DIRECTORY_SCREENSHOTS:截图,图库中显示的文件夹名。"lobbyDownload"
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName)
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        values.put(MediaStore.MediaColumns.DATE_ADDED, imageTime / 1000)
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, imageTime / 1000)
        values.put(MediaStore.MediaColumns.DATE_EXPIRES, (imageTime + 86400000) / 1000)
        values.put(MediaStore.MediaColumns.IS_PENDING, true)
        if (MediaStore.Video.Media.EXTERNAL_CONTENT_URI != null) {
            val resolver: ContentResolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                kotlin.runCatching {
                    // First, write the actual data for our screenshot
                    resolver.openFileDescriptor(uri, "w").use { descriptor ->
                        descriptor?.let {
                            FileOutputStream(descriptor.fileDescriptor).use { out ->
                                val videoFile = File(filePath)
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
                    onSuccess(imageFileName)
                }.getOrElse {
                    Log.i(TAG, "saveImageToGallery()---  exception = ${it.message}")
                }
            }

        }
    }

    @JvmStatic
    fun saveImageToGallery(
        image: Bitmap,
        compressFormat: Bitmap.CompressFormat,
        mimeType: String,
        onSuccess: (String) -> Unit,
        fileExtension: String = ROOM_PHOTO_EXTENSION
    ) {
        val imageTime = System.currentTimeMillis()
        val imageFileName = imageTime.toString() + fileExtension
        val stringId = context.applicationInfo.labelRes
        val appName = if (stringId == 0) context.applicationInfo.nonLocalizedLabel else context.getString(stringId)
        val values = ContentValues()
        values.put(
            MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + appName
        ) // Environment.DIRECTORY_SCREENSHOTS: 截图, 图库中显示的文件夹名。"lobbyDownload"
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName)
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        values.put(MediaStore.MediaColumns.DATE_ADDED, imageTime / 1000)
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, imageTime / 1000)
        values.put(MediaStore.MediaColumns.DATE_EXPIRES, (imageTime + 86400000) / 1000)
        values.put(MediaStore.MediaColumns.IS_PENDING, true)

        if (MediaStore.Images.Media.EXTERNAL_CONTENT_URI != null) {
            val resolver: ContentResolver = context.contentResolver

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                kotlin.runCatching {
                    // First, write the actual data for our screenshot
                    resolver.openOutputStream(uri).use { out ->
                        if (!image.compress(compressFormat, 100, out)) {
                            Log.i(TAG, "saveImageToGallery()---  failure")
                        }
                    }
                    // Everything went well above, publish it!
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    values.putNull(MediaStore.MediaColumns.DATE_EXPIRES)
                    resolver.update(uri, values, null, null)
                    onSuccess(imageFileName)
                }.getOrElse {
                    Log.i(TAG, "saveImageToGallery()---  exception = ${it.message}")
                }
            }

        }
    }
}

@IntDef(
    ROOM_PHOTO_AUDIO, ROOM_PHOTO_RENDER, ROOM_PHOTO_VIDEO_SHARE, ROOM_PHOTO_SEND,
    PEEK_SOURCE_VIDEO_REAR, PEEK_SOURCE_VIDEO_FRONT, PEEK_SOURCE_VIDEO_SHARE,
    PEEK_CONFIG_REAR, PEEK_CONFIG_FRONT,
    PEEK_VIDEO_COVER_REAR, PEEK_VIDEO_COVER_FRONT, PEEK_VIDEO_COVER_SHARE,
    PEEK_FINAL_VIDEO_REAR, PEEK_FINAL_VIDEO_FRONT, PEEK_FINAL_VIDEO_SHARE,
    REGULAR_INVITE_VIDEO_SHARE, PEEK_BGM,
    MEMORY_VIDEO_BGM, MEMORY_VIDEO_SOURCE, MEMORY_VIDEO_SHARE,
    FACE_TRACKER_MODEL, FACE_TRACKER_SEETA,
    NAME_CARD_VIDEO, MISSION_CARD_VIDEO,
    PFP_VIDEO, PFP_VIDEO_CROP, PFP_FIRST_FRAME,
    TAKE_PICTURE,
    TEST_ONLY
)
@Retention(AnnotationRetention.SOURCE)
annotation class FileUseCase {

    companion object {

        const val ROOM_PHOTO_AUDIO = 1
        const val ROOM_PHOTO_RENDER = 2
        const val ROOM_PHOTO_VIDEO_SHARE = 3
        const val ROOM_PHOTO_SEND = 4

        const val PEEK_SOURCE_VIDEO_REAR = 10
        const val PEEK_SOURCE_VIDEO_FRONT = 11
        const val PEEK_SOURCE_VIDEO_SHARE = 12
        const val PEEK_CONFIG_REAR = 14
        const val PEEK_CONFIG_FRONT = 15
        const val PEEK_BGM = 16
        const val PEEK_VIDEO_COVER_REAR = 17
        const val PEEK_VIDEO_COVER_FRONT = 18
        const val PEEK_VIDEO_COVER_SHARE = 19
        const val PEEK_FINAL_VIDEO_REAR = 20
        const val PEEK_FINAL_VIDEO_FRONT = 21
        const val PEEK_FINAL_VIDEO_SHARE = 22
        const val REGULAR_INVITE_VIDEO_SHARE = 23

        const val MEMORY_VIDEO_BGM = 30
        const val MEMORY_VIDEO_SOURCE = 31
        const val MEMORY_VIDEO_SHARE = 32

        const val FACE_TRACKER_MODEL = 40
        const val FACE_TRACKER_SEETA = 41

        const val NAME_CARD_VIDEO = 50

        const val MISSION_CARD_VIDEO = 60

        const val PFP_VIDEO = 70
        const val PFP_VIDEO_CROP = 71
        const val PFP_FIRST_FRAME = 72

        const val TAKE_PICTURE = 80

        const val TEST_ONLY = 99
    }
}