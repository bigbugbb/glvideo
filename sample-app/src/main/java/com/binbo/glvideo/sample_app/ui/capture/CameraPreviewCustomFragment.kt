package com.binbo.glvideo.sample_app.ui.capture

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
import com.binbo.glvideo.core.opengl.drawer.CameraDrawer
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.renderer.DefaultCameraRenderer
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentCameraPreviewCustomBinding
import com.binbo.glvideo.sample_app.ext.getColorCompat
import com.binbo.glvideo.sample_app.ui.widget.CommonHintDialog
import com.binbo.glvideo.sample_app.utils.PermissionUtils
import com.binbo.glvideo.sample_app.utils.permission.RxPermissions

/**
 * A simple [Fragment] subclass.
 * Use the [CameraPreviewCustomFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CameraPreviewCustomFragment : Fragment(), SurfaceTexture.OnFrameAvailableListener {

    private var _binding: FragmentCameraPreviewCustomBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var cameraController: CameraController

    internal lateinit var cameraRenderer: DefaultCameraRenderer

    internal val commonHintDialog by lazy { CommonHintDialog(App.context) }

    private val targetResolution: Size
        get() = Size(720, 1280)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCameraPreviewCustomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraController = CameraController(requireContext(), targetResolution, this)
        cameraController.onFrameAvailableListener = this
        lifecycle.addObserver(cameraController)

        cameraRenderer = DefaultCameraRenderer().apply {
            addDrawer(CameraDrawer().apply {
                setSurfaceTextureAvailableListener(cameraController)
            })
            addDrawer(FrameDrawer())
            setUseCustomRenderThread(true)
            setSurface(binding.viewGLCamera)
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
        }
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

    override fun onStop() {
        super.onStop()
        cameraController.scheduleUnbindCamera()
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
                noColor = App.context.getColorCompat(R.color.dialog_color_white),
                yesColor = App.context.getColorCompat(R.color.dialog_color_62FD75)
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
        fun newInstance() = CameraPreviewCustomFragment()
    }
}