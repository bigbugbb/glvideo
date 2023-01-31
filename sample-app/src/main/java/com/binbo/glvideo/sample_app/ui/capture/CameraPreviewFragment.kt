package com.binbo.glvideo.sample_app.ui.capture

import android.Manifest
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import com.binbo.glvideo.core.camera.CameraController
import com.binbo.glvideo.core.utils.DeviceUtil
import com.binbo.glvideo.sample_app.ContextUtil
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentCameraPreviewBinding
import com.binbo.glvideo.sample_app.databinding.FragmentCaptureBinding
import com.binbo.glvideo.sample_app.ext.getColorCompat
import com.binbo.glvideo.sample_app.ui.widget.CommonHintDialog
import com.binbo.glvideo.sample_app.utils.PermissionUtils
import com.binbo.glvideo.sample_app.utils.permission.RxPermissions

/**
 * A simple [Fragment] subclass.
 * Use the [CameraPreviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CameraPreviewFragment : Fragment() {

    private var _binding: FragmentCameraPreviewBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var cameraController: CameraController

    internal val commonHintDialog by lazy { CommonHintDialog(ContextUtil.context) }

    private val targetResolution: Size
        get() = Size(720, 1280)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCameraPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraController = CameraController(requireContext(), targetResolution, this)
        lifecycle.addObserver(cameraController!!)
        binding.cameraView.setSurfaceTextureAvailableListener(cameraController!!)
    }

    override fun onStart() {
        super.onStart()
        RxPermissions(this)
            .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .subscribe { granted ->
                if (granted) {
                    cameraController.scheduleBindCamera()
                } else {
                    onPermissionsNotGranted()
                }
            }

        cameraController.lensFacing = CameraSelector.LENS_FACING_FRONT
    }

    override fun onResume() {
        super.onResume()
        binding.cameraView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.cameraView.onPause()
    }

    override fun onStop() {
        super.onStop()
        cameraController.scheduleUnbindCamera()
    }

    private fun onPermissionsNotGranted() {
        if (!commonHintDialog.isShowing) {
            commonHintDialog.showDialog(
                titleStr = ContextUtil.context.getString(R.string.no_permission_toast),
                noStr = ContextUtil.context.getString(R.string.common_cancel),
                yesStr = ContextUtil.context.getString(R.string.settings_title),
                noColor = ContextUtil.context.getColorCompat(R.color.dialog_color_white),
                yesColor = ContextUtil.context.getColorCompat(R.color.dialog_color_62FD75)
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
        fun newInstance() = CameraPreviewFragment()
    }
}