package com.binbo.glvideo.sample_app.ui.capture.fragment

import android.Manifest
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.fragment.app.Fragment
import com.binbo.glvideo.core.camera.CameraController
import com.binbo.glvideo.core.ext.nowSystemClock
import com.binbo.glvideo.core.opengl.drawer.BlurDrawer
import com.binbo.glvideo.core.opengl.drawer.CameraDrawer
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentCameraPreviewEffectsBinding
import com.binbo.glvideo.sample_app.impl.capture.EffectsCameraRenderer
import com.binbo.glvideo.sample_app.ui.widget.CommonHintDialog
import com.binbo.glvideo.sample_app.utils.PermissionUtils
import com.binbo.glvideo.sample_app.utils.getColorCompat
import com.tbruyelle.rxpermissions3.RxPermissions

/**
 * A simple [Fragment] subclass.
 * Use the [CameraPreviewEffectsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CameraPreviewEffectsFragment : Fragment(), SurfaceTexture.OnFrameAvailableListener {

    private var _binding: FragmentCameraPreviewEffectsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var cameraController: CameraController

    private lateinit var cameraRenderer: EffectsCameraRenderer

    private val commonHintDialog by lazy { CommonHintDialog(requireContext()) }

    private val targetResolution: Size
        get() = Size(720, 1280)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCameraPreviewEffectsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraController = CameraController(requireContext(), targetResolution, this)
        cameraController.onFrameAvailableListener = this
        lifecycle.addObserver(cameraController)

        cameraRenderer = EffectsCameraRenderer().apply {
            addDrawer(CameraDrawer().apply {
                setSurfaceTextureAvailableListener(cameraController)
            })
            addDrawer(BlurDrawer())
            addDrawer(FrameDrawer())
            setUseCustomRenderThread(true)
            setSurface(binding.viewGLCamera)
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
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
        cameraController.scheduleUnbindCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        commonHintDialog.dismiss()
        _binding = null
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        cameraRenderer.notifySwap(nowSystemClock * 1000)
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
        fun newInstance() = CameraPreviewEffectsFragment()
    }
}