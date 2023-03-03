package com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.utils

import android.animation.ValueAnimator
import com.binbo.glvideo.core.ext.mainHandler


class NameCardRotationManager {

    @Volatile
    var rotateX = 0f

    @Volatile
    var rotateY = 0f

    @Volatile
    var rotateZ = 0f

    @Volatile
    private var rotateXAnimator: ValueAnimator? = null

    @Volatile
    private var rotateYAnimator: ValueAnimator? = null

    @Volatile
    private var rotateZAnimator: ValueAnimator? = null

    private var started = false

    fun startEnterRotation() {
        if (!started) {
            mainHandler.post {
                rotateYAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
                    duration = 1000
                    addUpdateListener {
                        rotateY = it.animatedValue as Float
                        if (rotateY.toInt() == 360) { // 处理edge case, 防止rotateBack自动转回来
                            rotateY = 0f
                        }
                    }
                    start()
                }
            }
            started = true
        }
    }

    fun rotateBack() {
        mainHandler.post {
            rotateXAnimator?.cancel()
            rotateXAnimator = ValueAnimator.ofFloat(rotateX, 0f).apply {
                duration = 1000
                addUpdateListener { rotateX = it.animatedValue as Float }
                start()
            }

            rotateYAnimator?.cancel()
            rotateYAnimator = ValueAnimator.ofFloat(rotateY, 0f).apply {
                duration = 1000
                addUpdateListener { rotateY = it.animatedValue as Float }
                start()
            }

            rotateZAnimator?.cancel()
            rotateZAnimator = ValueAnimator.ofFloat(rotateZ, 0f).apply {
                duration = 1000
                addUpdateListener { rotateZ = it.animatedValue as Float }
                start()
            }
        }
    }

    fun cancelRotationBack() {
        mainHandler.post {
            rotateXAnimator?.cancel()
            rotateXAnimator = null
            rotateYAnimator?.cancel()
            rotateYAnimator = null
            rotateZAnimator?.cancel()
            rotateYAnimator = null
        }
    }
}