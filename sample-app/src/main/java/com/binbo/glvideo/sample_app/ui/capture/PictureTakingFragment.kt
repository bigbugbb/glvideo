package com.binbo.glvideo.sample_app.ui.capture

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
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.databinding.FragmentPictureTakingBinding
import com.binbo.glvideo.sample_app.ext.getColorCompat
import com.binbo.glvideo.sample_app.impl.capture.graph.picture_taking.PictureCaptureGraphManager
import com.binbo.glvideo.sample_app.ui.widget.CommonHintDialog
import com.binbo.glvideo.sample_app.utils.PermissionUtils
import com.binbo.glvideo.sample_app.utils.permission.RxPermissions
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PictureTakingFragment : Fragment() {

    private var _binding: FragmentPictureTakingBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var cameraController: CameraController

    private lateinit var graphManager: PictureCaptureGraphManager

    private val commonHintDialog by lazy { CommonHintDialog(requireContext()) }

    private val targetResolution: Size
        get() = Size(720, 1280)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPictureTakingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraController = CameraController(requireContext(), targetResolution, this)
        graphManager = PictureCaptureGraphManager(binding.viewGLCamera, cameraController)

        cameraController.onFrameAvailableListener = graphManager
        lifecycle.addObserver(cameraController)

        binding.btnTakePicture.singleClick {
            lifecycleScope.launch {
                graphManager.takePicture()
            }
        }

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
        fun newInstance() = PictureTakingFragment()
    }
}