package com.binbo.glvideo.core.camera

import android.util.Size
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture


data class CameraConfig private constructor(
    val aspectRatio: Int,
    val resolution: Size,
    val rotation: Int,
    val lensFacing: Int,
    val flashMode: Int,
    val backpressureStrategy: Int,
    val enableImageAnalysis: Boolean,
    val enableImageCapture: Boolean
) {

    data class Builder(
        private var aspectRatio: Int = AspectRatio.RATIO_16_9,
        private var resolution: Size = Size(1080, 1920),
        private var rotation: Int = Surface.ROTATION_0,
        private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
        private var flashMode: Int = ImageCapture.FLASH_MODE_OFF,
        private var backpressureStrategy: Int = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST,
        private var enableImageAnalysis: Boolean = false,
        private var enableImageCapture: Boolean = false
    ) {
        fun aspectRatio(aspectRatio: Int) = apply { this.aspectRatio = aspectRatio }
        fun resolution(resolution: Size) = apply { this.resolution = resolution }
        fun rotation(rotation: Int) = apply { this.rotation = rotation }
        fun lensFacing(lensFacing: Int) = apply { this.lensFacing = lensFacing }
        fun flashMode(flashMode: Int) = apply { this.flashMode = flashMode }
        fun backpressureStrategy(backpressureStrategy: Int) = apply { this.backpressureStrategy = backpressureStrategy }
        fun enableImageAnalysis(enableImageAnalysis: Boolean) = apply { this.enableImageAnalysis = enableImageAnalysis }
        fun enableImageCapture(enableImageCapture: Boolean) = apply { this.enableImageCapture = enableImageCapture }

        fun build() = CameraConfig(
            aspectRatio,
            resolution,
            rotation,
            lensFacing,
            flashMode,
            backpressureStrategy,
            enableImageAnalysis,
            enableImageCapture
        )
    }

    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }
}