package com.binbo.glvideo.core.opengl.renderer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent


open class DefaultGLSurfaceView : GLSurfaceView {

    lateinit var renderer: DefaultGLRenderer

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (event != null) {
            // Convert touch coordinates into normalized device coordinates,
            // keeping in mind that Android's Y coordinates are inverted.
            val normalizedX = event.x / width.toFloat() * 2 - 1
            val normalizedY = -(event.y / height.toFloat() * 2 - 1)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    queueEvent { renderer?.onTouchPress(normalizedX, normalizedY) }
                }
                MotionEvent.ACTION_MOVE -> {
                    queueEvent { renderer?.onTouchDragged(normalizedX, normalizedY) }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    queueEvent { renderer?.onTouchRelease(normalizedX, normalizedY) }
                }
            }
            true
        } else {
            false
        }
    }

    override fun setRenderer(renderer: Renderer) {
        super.setRenderer(renderer)
        if (renderer is DefaultGLRenderer) {
            this.renderer = renderer
        } else {
            error("renderer must be subclass of DefaultGLRenderer")
        }
    }
}