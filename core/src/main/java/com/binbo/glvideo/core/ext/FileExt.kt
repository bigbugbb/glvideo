package com.binbo.glvideo.core.ext

import android.graphics.Bitmap
import java.io.File

fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
    outputStream().use { out ->
        bitmap.compress(format, quality, out)
        out.flush()
    }
}