package com.binbo.glvideo.sample_app.ui.capture.fragment

import android.Manifest
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.binbo.glvideo.core.camera.CameraController
import com.binbo.glvideo.core.ext.nowString
import com.binbo.glvideo.core.ext.setVisible
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentVideoRecordingBinding
import com.binbo.glvideo.sample_app.impl.capture.graph.VideoCaptureGraphManager
import com.binbo.glvideo.sample_app.ui.widget.CommonHintDialog
import com.binbo.glvideo.sample_app.utils.PermissionUtils
import com.binbo.glvideo.sample_app.utils.getColorCompat
import com.tbruyelle.rxpermissions3.RxPermissions
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class VideoRecordingFragment : Fragment() {

    private var _binding: FragmentVideoRecordingBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var cameraController: CameraController

    private lateinit var graphManager: VideoCaptureGraphManager

    private val commonHintDialog by lazy { CommonHintDialog(requireContext()) }

    private val targetResolution: Size
        get() = Size(720, 1280)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentVideoRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraController = CameraController(requireContext(), targetResolution, this)
        graphManager = VideoCaptureGraphManager(nowString, binding.viewGLCamera, cameraController)

        cameraController.onFrameAvailableListener = graphManager
        lifecycle.addObserver(cameraController)

        binding.cardStartRecording.singleClick { startRecording() }
        binding.cardStopRecording.singleClick { stopRecording() }

        // 不阻塞调用导致surfaceHolder回调失效
        runBlocking {
            graphManager.createMediaGraph()
            graphManager.prepare()
            graphManager.start()
        }
    }

    override fun onStart() {
        super.onStart()

        RxPermissions(this)
            .request(Manifest.permission.CAMERA)
            .subscribe { granted ->
                if (granted) {
                    // All permissions are granted !
                    cameraController.scheduleBindCamera()
                } else {
                    // At least one denied permission with ask never again
                    // Need to go to the settings
                    onPermissionsNotGranted()
                }
            }

        cameraController.lensFacing = CameraSelector.LENS_FACING_FRONT
    }

    override fun onStop() {
        super.onStop()
        stopRecording()
        cameraController.scheduleUnbindCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        runBlocking {
            graphManager.stop()
            graphManager.release()
            graphManager.destroyMediaGraph()
        }
        commonHintDialog.dismiss()
        _binding = null
    }

    private fun startRecording() {
        if (graphManager.isRecording) return

        lifecycleScope.launch {
            graphManager.recordVideo(true)
        }

        binding.cardStartRecording.setVisible(false)
        binding.cardStopRecording.setVisible(true)
    }

    private fun stopRecording() {
        if (!graphManager.isRecording) return

        lifecycleScope.launch {
            graphManager.recordVideo(false)
            graphManager.awaitDone()
            activity?.finish()
        }
    }

    private fun onPermissionsNotGranted() {
        if (!commonHintDialog.isShowing) {
            commonHintDialog.showDialog(
                titleStr = App.context.getString(R.string.no_permission_toast),
                noStr = App.context.getString(R.string.common_cancel),
                yesStr = App.context.getString(R.string.settings_title),
                noColor = App.context.getColorCompat(android.R.color.white),
                yesColor = App.context.getColorCompat(R.color.colorPrimary)
            ).apply {
                onPositiveClick = {
                    kotlin.runCatching { PermissionUtils.toPermissionSetting(context) }
                }
                onNegativeClick = { activity?.finish() }
                setOnCancelListener { activity?.finish() }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = VideoRecordingFragment()
    }
}