package com.binbo.glvideo.core.ext

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.binbo.glvideo.core.GLVideo
import com.binbo.glvideo.core.utils.FileToolUtils
import java.io.File


fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.flip(source: Bitmap, xFlip: Boolean, yFlip: Boolean): Bitmap {
    val matrix = Matrix()
    matrix.postScale(if (xFlip) -1f else 1f, if (yFlip) -1f else 1f, source.width / 2f, source.height / 2f)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

fun Bitmap.writeToGallery(
    compressFormat: Bitmap.CompressFormat,
    mimeType: String,
    fileExtension: String = FileToolUtils.ROOM_PHOTO_EXTENSION,
    onSuccess: (String) -> Unit
) {
    val currentTime = now
    val imageFileName = "$currentTime" + fileExtension
    val stringId = GLVideo.context.applicationInfo.labelRes
    val appName = if (stringId == 0) GLVideo.context.applicationInfo.nonLocalizedLabel else GLVideo.context.getString(stringId)
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + appName) // Environment.DIRECTORY_SCREENSHOTS: 截图, 图库中显示的文件夹名。
        put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.DATE_ADDED, currentTime / 1000)
        put(MediaStore.MediaColumns.DATE_MODIFIED, currentTime / 1000)
        put(MediaStore.MediaColumns.DATE_EXPIRES, (currentTime + 86400000) / 1000)
        put(MediaStore.MediaColumns.IS_PENDING, true)
    }

    MediaStore.Images.Media.EXTERNAL_CONTENT_URI?.let { contentUri ->
        val resolver: ContentResolver = GLVideo.context.contentResolver

        resolver.insert(contentUri, values)?.let {
            kotlin.runCatching {
                // First, write the actual data for our screenshot
                resolver.openOutputStream(it).use { out ->
                    if (!compress(compressFormat, 80, out)) {
                        Log.i(FileToolUtils.TAG, "writeToGallery()---  failure")
                    }
                }
                // Everything went well above, publish it!
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                values.putNull(MediaStore.MediaColumns.DATE_EXPIRES)

                resolver.update(it, values, null, null)
                onSuccess(imageFileName)
            }.getOrElse {
                Log.i(FileToolUtils.TAG, "writeToGallery()---  exception = ${it.message}")
            }
        }
    }
}