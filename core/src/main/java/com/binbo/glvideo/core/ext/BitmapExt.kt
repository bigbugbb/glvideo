package com.binbo.glvideo.core.ext

import android.graphics.Bitmap
import android.graphics.Matrix


fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.flip(source: Bitmap, xFlip: Boolean, yFlip: Boolean): Bitmap {
    val matrix = Matrix()
    matrix.postScale(if (xFlip) -1f else 1f, if (yFlip) -1f else 1f, source.width / 2f, source.height / 2f)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}