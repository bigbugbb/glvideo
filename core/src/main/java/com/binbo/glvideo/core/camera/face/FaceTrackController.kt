package com.binbo.glvideo.core.camera.face

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


interface FaceTrackObserver {
    var face: Face?
    fun onFaceDetected(face: Face)
}

class FaceTrackController(
    private val lifecycleOwner: LifecycleOwner,
    private val imageSize: Size
) : DefaultLifecycleObserver {

    @Volatile
    private var faceTracker: FaceTracker? = null

    val face: Face?
        get() = faceTracker?.mFace

    var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
        set(value) {
            field = value
        }

    private val lifecycleScope: LifecycleCoroutineScope
        get() = lifecycleOwner.lifecycleScope

    fun detect(data: ByteArray) {
        faceTracker?.detector(data, lensFacing)
    }

    fun setFaceTrackObserver(observer: FaceTrackObserver) {
        faceTracker?.setFaceTrackObserver(observer)
    }

    private fun releaseTracker() {
        kotlin.runCatching {
            faceTracker?.stopTrack()
            faceTracker = null
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        lifecycleScope.launch {
            kotlin.runCatching {
//                val modelFile = FileToolUtils.getFile(FACE_TRACKER_MODEL, "lbpcascade_frontalface.xml")
//                val seetaFile = FileToolUtils.getFile(FACE_TRACKER_SEETA, "seeta_fa_v1.1.bin")
//                faceTracker = FaceTracker(modelFile.absolutePath, seetaFile.absolutePath, imageSize.width, imageSize.height)
//                faceTracker?.startTrack()
            }.getOrElse {
                releaseTracker()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        releaseTracker()
    }
}