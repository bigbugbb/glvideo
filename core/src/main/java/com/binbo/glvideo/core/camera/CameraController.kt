package com.binbo.glvideo.core.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.*
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.binbo.glvideo.core.GLVideo
import com.binbo.glvideo.core.GLVideo.Core.tagOfCamera
import com.binbo.glvideo.core.camera.analyzer.SimpleImageAnalyzer
import com.binbo.glvideo.core.camera.utils.CameraImageSaver
import com.binbo.glvideo.core.ext.no
import com.binbo.glvideo.core.ext.nowString
import com.binbo.glvideo.core.opengl.drawer.SurfaceTextureAvailableListener
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.Executors


@SuppressLint("RestrictedApi")
class CameraController(
    private val context: Context,
    private var targetResolution: Size,
    private val lifecycleOwner: LifecycleOwner,
) : DefaultLifecycleObserver, Preview.SurfaceProvider, SurfaceTextureAvailableListener {

    private var camera: Camera? = null
    private var preview: Preview? = null

    private var imageAnalyzer: ImageAnalysis.Analyzer? = null

    val config: CameraConfig
        get() = CameraConfig.build {
            resolution(targetResolution)
            lensFacing(lensFacing)
            flashMode(flashMode)
            enableImageAnalysis(true)
        }

    var flashMode: Int = ImageCapture.FLASH_MODE_OFF
        set(value) {
            field = value
            imageCapture?.flashMode = flashMode
        }

    var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
        set(value) {
            field = value
            if (isAvailableToSwitchBetweenFrontAndBack) {
                // Re-bind use cases to update selected camera
                bindCameraUseCases(surfaceProvider, config)
            }
        }

    val surfaceProvider: Preview.SurfaceProvider
        get() = this

    private val isAvailableToSwitchBetweenFrontAndBack: Boolean
        get() = kotlin.runCatching { hasBackCamera() && hasFrontCamera() }.getOrElse { false }

    var onFrameAvailableListener: SurfaceTexture.OnFrameAvailableListener?
        get() = frameAvailableListenerRef?.get()
        set(value) {
            frameAvailableListenerRef = WeakReference(value)
        }

    var onCameraStateListener: CameraStateListener?
        get() = cameraStateListenerRef?.get()
        set(value) {
            cameraStateListenerRef = WeakReference(value)
        }

    val captureResult: CaptureResult?
        get() = totalCaptureResult ?: partialCaptureResult

    var cameraCaptureCallback: CameraCaptureSession.CaptureCallback?
        get() = cameraCaptureCallbackRef?.get()
        set(value) {
            cameraCaptureCallbackRef = WeakReference(value)
        }

    private var frameAvailableListenerRef: WeakReference<SurfaceTexture.OnFrameAvailableListener>? = null

    private var cameraStateListenerRef: WeakReference<CameraStateListener>? = null

    private var cameraCaptureCallbackRef: WeakReference<CameraCaptureSession.CaptureCallback>? = null

    private var imageAnalysis: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var partialCaptureResult: CaptureResult? = null
    private var totalCaptureResult: TotalCaptureResult? = null

    private val cameraExecutor = CameraXExecutors.mainThreadExecutor() // Blocking camera operations are performed using this executor
    private val ioExecutor = CameraXExecutors.ioExecutor()
    private val sequentialIoExecutor by lazy { CameraXExecutors.newSequentialExecutor(ioExecutor) }
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private var surfaceTexture: SurfaceTexture? = null

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_BIND_CAMERA -> bindCamera(surfaceProvider, config)
                MSG_UNBIND_CAMERA -> unbindAll()
            }
        }
    }

    override fun onSurfaceRequested(request: SurfaceRequest) {
        val resetTexture = surfaceTexture?.apply {
            setOnFrameAvailableListener(onFrameAvailableListener)
            setDefaultBufferSize(request.resolution.width, request.resolution.height)
        } ?: return
        val surface = Surface(resetTexture)
        Log.d(tagOfCamera, "provideSurface $surface")
        request.provideSurface(surface, ContextCompat.getMainExecutor(context)) { result ->
            Log.d(tagOfCamera, "release surface $surface")
            surface.release()
        }
    }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture) {
        surfaceTexture = st
        scheduleReBindCamera()
    }

    fun scheduleBindCamera() {
        handler.removeCallbacksAndMessages(null)
        handler.sendEmptyMessageDelayed(MSG_BIND_CAMERA, 50)
    }

    fun scheduleUnbindCamera(delay: Long = 50) {
        handler.removeCallbacksAndMessages(null)
        handler.sendEmptyMessageDelayed(MSG_UNBIND_CAMERA, delay)
    }

    fun scheduleReBindCamera() {
        handler.removeCallbacksAndMessages(null)
        handler.sendEmptyMessageDelayed(MSG_UNBIND_CAMERA, 0)
        handler.sendEmptyMessageDelayed(MSG_BIND_CAMERA, 500)
    }

//    fun takePicture(onImageSaved: Uri.() -> Unit) {
//        val file = FileToolUtils.getFile(FileUseCase.PICTURE_TAKING, "$nowString.jpg")
//        val metadata = ImageCapture.Metadata()
//        metadata.isReversedHorizontal = config.lensFacing == CameraSelector.LENS_FACING_FRONT
//        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file)
//            .setMetadata(metadata)
//            .build()
//
//        imageCapture?.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
//            override fun onCaptureSuccess(image: ImageProxy) {
//                super.onCaptureSuccess(image)
//                ioExecutor.execute(
//                    CameraImageSaver(file.absolutePath, image, outputFileOptions, config.rotation, 50, cameraExecutor, sequentialIoExecutor,
//                        object : CameraImageSaver.OnImageSavedCallback {
//                            override fun onImageSaved(uri: Uri) {
//                                onImageSaved.invoke(uri)
//                            }
//
//                            override fun onError(saveError: CameraImageSaver.SaveError, message: String, cause: Throwable?) {
//                                Log.e(tagOfCamera, "saveError: $saveError, message: $message, cause: $cause")
//                            }
//                        }
//                    )
//                )
//            }
//
//            override fun onError(exception: ImageCaptureException) {
//                super.onError(exception)
//                if (!exception.localizedMessage.isNullOrBlank()) {
//                    Toast.makeText(GLVideo.context, exception.localizedMessage, Toast.LENGTH_SHORT).show()
//                }
//            }
//        })
//    }

    fun setImageAnalyzer(analyzer: ImageAnalysis.Analyzer?) {
        imageAnalyzer = analyzer
    }

    /** Returns true if the device has an available back camera. False otherwise */
    fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    private fun bindCamera(surfaceProvider: Preview.SurfaceProvider, config: CameraConfig) {
        //Future表示一个异步的任务，ListenableFuture可以监听这个任务，当任务完成的时候执行回调
        Log.d(tagOfCamera, "setupCamera with surfaceProvider: $surfaceProvider")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            Log.d(tagOfCamera, "get cameraProvider: $cameraProvider")
            kotlin.runCatching {
                bindCameraUseCases(surfaceProvider, config)
            }.getOrElse {
                Log.e(tagOfCamera, it.message ?: "")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /** Declare and bind preview, capture and analysis use cases */
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases(surfaceProvider: Preview.SurfaceProvider, config: CameraConfig) {
        // CameraProvider
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(config.lensFacing).build()

        val useCases = mutableListOf<UseCase?>().apply {
            // Preview
            preview = Preview.Builder()
                .setTargetAspectRatio(config.aspectRatio)
                .setTargetRotation(config.rotation)
                .build()
            add(preview)

            // ImageAnalysis
            if (config.enableImageAnalysis) {
                val builder = ImageAnalysis.Builder()
                    .setTargetResolution(Size(config.resolution.width / 2, config.resolution.height / 2))
                    .setOutputImageRotationEnabled(true)
                    .setTargetRotation(config.rotation)
                    .setBackpressureStrategy(config.backpressureStrategy)
                val camera2InterOp = Camera2Interop.Extender(builder)
                camera2InterOp.setSessionCaptureCallback(DefaultCameraCaptureCallback())

                imageAnalysis = builder.build()
                imageAnalysis?.setAnalyzer(analysisExecutor, imageAnalyzer ?: SimpleImageAnalyzer())
                add(imageAnalysis)
            }

            // ImageCapture
            if (config.enableImageCapture) {
                imageCapture = ImageCapture.Builder()
                    .setTargetResolution(config.resolution)
                    .setFlashMode(config.flashMode)
                    // 优化捕获速度，可能降低图片质量
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    // 设置初始的旋转角度
                    .setTargetRotation(config.rotation)
                    .build()
                add(imageCapture)
            }
        }.filterNotNull().toTypedArray()

        cameraProvider.unbindAll()

        kotlin.runCatching {
            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(surfaceProvider)

            Log.d(tagOfCamera, "cameraProvider bindToLifecycle")
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, *useCases)

            observeCameraState(camera?.cameraInfo!!)
        }.getOrElse {
            Log.e(tagOfCamera, it.toString())
        }
    }

    private fun unbindAll() {
        Log.d(tagOfCamera, "unbindAll")
        cameraProvider?.unbindAll()
    }

    private fun observeCameraState(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.removeObservers(lifecycleOwner)
        cameraInfo.cameraState.observe(lifecycleOwner) { cameraState ->
            run {
                when (cameraState.type) {
                    CameraState.Type.PENDING_OPEN -> {
                        // Ask the user to close other camera apps
                        Log.d("observeCameraState", "CameraState: Pending Open")
                        onCameraStateListener?.onCameraPendingOpen(cameraInfo)
                    }
                    CameraState.Type.OPENING -> {
                        // Show the Camera UI
                        Log.d("observeCameraState", "CameraState: Opening")
                        onCameraStateListener?.onCameraOpening(cameraInfo)
                    }
                    CameraState.Type.OPEN -> {
                        // Setup Camera resources and begin processing
                        Log.d("observeCameraState", "CameraState: Open")
                        onCameraStateListener?.onCameraOpen(cameraInfo)
                    }
                    CameraState.Type.CLOSING -> {
                        // Close camera UI
                        Log.d("observeCameraState", "CameraState: Closing")
                        onCameraStateListener?.onCameraClosing(cameraInfo)
                    }
                    CameraState.Type.CLOSED -> {
                        // Free camera resources
                        Log.d("observeCameraState", "CameraState: Closed")
                        onCameraStateListener?.onCameraClosed(cameraInfo)
                    }
                }
            }

            cameraState.error?.let { error ->
                when (error.code) {
                    // Open errors
                    CameraState.ERROR_STREAM_CONFIG -> {
                        // Make sure to setup the use cases properly
                        Toast.makeText(context.applicationContext, "Stream config error", Toast.LENGTH_SHORT).show()
                    }
                    // Opening errors
                    CameraState.ERROR_CAMERA_IN_USE -> {
                        // Close the camera or ask user to close another camera app that's using the camera
                        Toast.makeText(context.applicationContext, "Camera in use", Toast.LENGTH_SHORT).show()
                    }
                    CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                        // Close another open camera in the app, or ask the user to close another
                        // camera app that's using the camera
                        Toast.makeText(context.applicationContext, "Max cameras in use", Toast.LENGTH_SHORT).show()
                    }
                    CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {
                        Toast.makeText(context.applicationContext, "Other recoverable error", Toast.LENGTH_SHORT).show()
                    }
                    // Closing errors
                    CameraState.ERROR_CAMERA_DISABLED -> {
                        // Ask the user to enable the device's cameras
                        Toast.makeText(context.applicationContext, "Camera disabled", Toast.LENGTH_SHORT).show()
                    }
                    CameraState.ERROR_CAMERA_FATAL_ERROR -> {
                        // Ask the user to reboot the device to restore camera function
                        Toast.makeText(context.applicationContext, "Fatal error", Toast.LENGTH_SHORT).show()
                    }
                    // Closed errors
                    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                        // Ask the user to disable the "Do Not Disturb" mode, then reopen the camera
                        Toast.makeText(context.applicationContext, "Do not disturb mode enabled", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
//        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//            val modelFile = FileToolUtils.getFile(FileUseCase.FACE_TRACKER_MODEL, "lbpcascade_frontalface.xml")
//            OpenGLUtils.copyAssets2SdCard(context, "face_tracker/lbpcascade_frontalface_improved.xml", modelFile.absolutePath)
//            val seetaFile = FileToolUtils.getFile(FileUseCase.FACE_TRACKER_SEETA, "seeta_fa_v1.1.bin")
//            OpenGLUtils.copyAssets2SdCard(context, "face_tracker/seeta_fa_v1.1.bin", seetaFile.absolutePath)
//        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        imageAnalyzer = null
        analysisExecutor.shutdown()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            surfaceTexture?.isReleased?.no {
                surfaceTexture?.release()
                surfaceTexture = null
            }
        }
        handler.removeCallbacksAndMessages(null)
    }

    inner class DefaultCameraCaptureCallback : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureStarted(session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long) {
            super.onCaptureStarted(session, request, timestamp, frameNumber)
//            Log.d(tagOfCapture, "onCaptureStarted timestamp: $timestamp frameNumber: $frameNumber")
            cameraCaptureCallback?.onCaptureStarted(session, request, timestamp, frameNumber)
        }

        override fun onCaptureProgressed(session: CameraCaptureSession, request: CaptureRequest, partialResult: CaptureResult) {
            super.onCaptureProgressed(session, request, partialResult)
//            Log.d(tagOfCapture, "onCaptureProgressed")
            partialCaptureResult = partialResult
            cameraCaptureCallback?.onCaptureProgressed(session, request, partialResult)
        }

        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
//            Log.d(tagOfCapture, "onCaptureCompleted")
            totalCaptureResult = result
            cameraCaptureCallback?.onCaptureCompleted(session, request, result)
        }

        override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
            super.onCaptureFailed(session, request, failure)
//            Log.d(tagOfCapture, "onCaptureFailed failure: $failure")
            cameraCaptureCallback?.onCaptureFailed(session, request, failure)
        }

        override fun onCaptureSequenceCompleted(session: CameraCaptureSession, sequenceId: Int, frameNumber: Long) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber)
//            Log.d(tagOfCapture, "onCaptureSequenceCompleted sequenceId: $sequenceId frameNumber: $frameNumber")
            cameraCaptureCallback?.onCaptureSequenceCompleted(session, sequenceId, frameNumber)
        }

        override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
            super.onCaptureSequenceAborted(session, sequenceId)
//            Log.d(tagOfCapture, "onCaptureSequenceAborted sequenceId: $sequenceId")
            cameraCaptureCallback?.onCaptureSequenceAborted(session, sequenceId)
        }

        override fun onCaptureBufferLost(session: CameraCaptureSession, request: CaptureRequest, target: Surface, frameNumber: Long) {
            super.onCaptureBufferLost(session, request, target, frameNumber)
//            Log.d(tagOfCapture, "onCaptureBufferLost target: $target frameNumber: $frameNumber")
            cameraCaptureCallback?.onCaptureBufferLost(session, request, target, frameNumber)
        }
    }

    interface CameraStateListener {
        fun onCameraPendingOpen(cameraInfo: CameraInfo) {}
        fun onCameraOpening(cameraInfo: CameraInfo) {}
        fun onCameraOpen(cameraInfo: CameraInfo) {}
        fun onCameraClosing(cameraInfo: CameraInfo) {}
        fun onCameraClosed(cameraInfo: CameraInfo) {}
    }

    companion object {
        const val MSG_BIND_CAMERA = 1
        const val MSG_UNBIND_CAMERA = 2
    }
}