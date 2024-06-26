package com.binbo.glvideo.sample_app.ui.capture.fragment

import android.Manifest
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.fragment.app.Fragment
import com.binbo.glvideo.core.camera.CameraController
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentCameraPreviewBinding
import com.binbo.glvideo.sample_app.ui.widget.CommonHintDialog
import com.binbo.glvideo.sample_app.utils.PermissionUtils
import com.binbo.glvideo.sample_app.utils.getColorCompat
import com.tbruyelle.rxpermissions3.RxPermissions

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

    private val commonHintDialog by lazy { CommonHintDialog(requireContext()) }

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
        binding.viewCamera.setSurfaceTextureAvailableListener(cameraController!!)
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

    override fun onResume() {
        super.onResume()
        binding.viewCamera.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.viewCamera.onPause()
    }

    override fun onStop() {
        super.onStop()
        cameraController.scheduleUnbindCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        commonHintDialog.dismiss()
        _binding = null
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
        fun newInstance() = CameraPreviewFragment()
    }
}