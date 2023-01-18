package com.binbo.glvideo.core.opengl.objects;

import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;

import com.binbo.glvideo.core.opengl.data.VertexArray;
import com.binbo.glvideo.core.opengl.program.BlurShaderProgram;

public class Blur {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * 4;

    private static final float[] VERTEX_DATA = {
            // Order of coordinates: X, Y, S, T
            -1.0f, -1.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f};

    private VertexArray vertexArray;

    public Blur() {
        vertexArray = new VertexArray(VERTEX_DATA);
    }

    public void bindData(BlurShaderProgram shaderProgram) {
        vertexArray.setVertexAttribPointer(
                0,
                shaderProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE);

        vertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                shaderProgram.getTextureCoordinatesAttributeLocation(),
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                STRIDE);
    }

    public void draw() {
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }
}
