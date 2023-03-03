package com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.objects;

import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.glDrawArrays;
import static com.binbo.glvideo.core.utils.Constants.BYTES_PER_FLOAT;
import static com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.nameCardLeft;
import static com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardWithVideoConfig.nameCardRight;

import android.os.SystemClock;

import com.binbo.glvideo.core.opengl.data.VertexArray;
import com.binbo.glvideo.core.opengl.program.ClipTextureShaderProgram;

public class MovingNickname {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    private static float[] VERTEX_DATA;

    private final VertexArray vertexArray;

    private float offsetX = 0f;
    private long lastUpdateTime = 0;

    private float width = 0f;
    private float height = 0f;

    private float leftOutPortion = 0f;  // 表示左边x%的区域在可视范围外，默认0%
    private float rightOutPortion = 1f; // 表示右边x%的区域在可视范围外，默认100%，因为从右边出来，左边消失

    public MovingNickname(float width, float height) {
        this.width = width;
        this.height = height;

        VERTEX_DATA = new float[]{
                // Order of coordinates: X, Y, S, T

                // Triangle Fan
                width / 2f + nameCardRight, -height / 2f, 0.5f, 0.5f,
                nameCardRight, -height, 0f, 1f,
                width + nameCardRight, -height, 1f, 1f,
                width + nameCardRight, 0f, 1f, 0f,
                nameCardRight, 0f, 0f, 0f,
                nameCardRight, -height, 0f, 1f};
        vertexArray = new VertexArray(VERTEX_DATA);
    }

    public void bindData(ClipTextureShaderProgram nicknameProgram) {
        vertexArray.setVertexAttribPointer(
                0,
                nicknameProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE);

        vertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                nicknameProgram.getTextureCoordinatesAttributeLocation(),
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                STRIDE);
    }

    public void draw() {
        glDrawArrays(GL_TRIANGLE_FAN, 0, 6);
    }

    public void update() {
        long now = SystemClock.uptimeMillis();
        if (now - lastUpdateTime > 16) {
            offsetX -= 0.005f;
            lastUpdateTime = now;

            float cardWidth = Math.abs(nameCardLeft) + Math.abs(nameCardRight) - 0.02f;
            if (Math.abs(offsetX) > cardWidth + width) {
                offsetX = 0f;
            }

            leftOutPortion = Math.max(0, (Math.abs(offsetX) - cardWidth - 0.0035f) / width);
            rightOutPortion = Math.max(0, (width - Math.abs(offsetX) + 0.021f) / width);
        }
    }

    public float getXOffset() {
        return offsetX;
    }

    public float getLeftOutPortion() {
        return leftOutPortion;
    }

    public float getRightOutPortion() {
        return rightOutPortion;
    }
}
