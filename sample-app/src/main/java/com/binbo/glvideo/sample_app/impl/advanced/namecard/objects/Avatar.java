package com.binbo.glvideo.sample_app.impl.advanced.namecard.objects;

import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.glDrawArrays;
import static com.binbo.glvideo.core.utils.Constants.BYTES_PER_FLOAT;

import com.binbo.glvideo.core.opengl.data.VertexArray;
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram;

/**
 * @author bigbug
 * @project lobby
 * @date 2022/11/9
 * @time 12:05
 */
public class Avatar {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT
            + TEXTURE_COORDINATES_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    private static float[] VERTEX_DATA;

    private final VertexArray vertexArray;

    public Avatar(float radius) {
        VERTEX_DATA = new float[]{
                // Order of coordinates: X, Y, S, T

                // Triangle Fan
                0f, 0f, 0.5f, 0.5f,
                -radius, -radius, 0f, 1f,
                radius, -radius, 1f, 1f,
                radius, radius, 1f, 0f,
                -radius, radius, 0f, 0f,
                -radius, -radius, 0f, 1f};
        vertexArray = new VertexArray(VERTEX_DATA);
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
        glDrawArrays(GL_TRIANGLE_FAN, 0, 6);
    }
}
