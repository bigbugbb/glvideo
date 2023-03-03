package com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.objects;

import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;
import static com.binbo.glvideo.core.utils.Constants.BYTES_PER_FLOAT;

import com.binbo.glvideo.core.opengl.data.VertexArray;
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram;

/**
 * 用于FBO里绘制，所以顶点是上下颠倒的
 */
public class ClippedVideoFrame {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT
            + TEXTURE_COORDINATES_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    private VertexArray vertexArray;

    public ClippedVideoFrame(float left, float right, float top, float bottom) {
        vertexArray = new VertexArray(new float[]{
                // Order of coordinates: X, Y, S, T
                // Triangle Strip
                -1f, 1f, left, bottom,
                1f, 1f, right, bottom,
                -1f, -1f, left, top,
                1f, -1f, right, top});
    }

    public void updateClipping(float left, float right, float top, float bottom) {
        vertexArray = new VertexArray(new float[]{
                // Order of coordinates: X, Y, S, T
                // Triangle Strip
                -1f, 1f, left, bottom,
                1f, 1f, right, bottom,
                -1f, -1f, left, top,
                1f, -1f, right, top});
    }

    public void bindData(TextureShaderProgram textureProgram) {
        vertexArray.setVertexAttribPointer(
                0,
                textureProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE);

        vertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                textureProgram.getTextureCoordinatesAttributeLocation(),
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                STRIDE);
    }

    public void draw() {
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }
}
