package com.binbo.glvideo.core.opengl.renderer

interface RenderImpl {
    fun onSurfaceCreate() {}
    fun onSurfaceChange(width: Int, height: Int) {}
    fun onSurfaceDestroy() {}
    fun onDrawFrame() {}
}