package com.binbo.glvideo.core.camera.analyzer

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.binbo.glvideo.core.media.utils.ImageHelper


class SimpleImageAnalyzer : ImageAnalysis.Analyzer {

    private var takeOneYuv = false

    override fun analyze(image: ImageProxy) {
        // 下面处理数据
        if (takeOneYuv) {
            takeOneYuv = false
//            ImageHelper.useYuvImgSaveFile(image, true) // 存储这一帧为文件
        }
        image.close()
    }

    companion object {
        const val TAG = "SimpleImageAnalyzer"
    }
}