package com.binbo.glvideo.core.camera.analyzer

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.binbo.glvideo.core.camera.face.FaceTrackController


class FaceTrackAnalyzer(private val controller: FaceTrackController) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            require(image.format == ImageFormat.YUV_420_888) { "Invalid image format" }
            val yPlane = image.planes[0]
            val yBuffer = yPlane.buffer
            val nv21 = ByteArray(image.width * image.height * 3 / 2)
            var index = 0
            val yRowStride = yPlane.rowStride
            val yPixelStride = yPlane.pixelStride
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    nv21[index++] = yBuffer[y * yRowStride + x * yPixelStride]
                }
            }
            controller.detect(nv21)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            image.close()
        }
    }
}