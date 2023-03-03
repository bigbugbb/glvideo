package com.binbo.glvideo.sample_app.impl.advanced.namecard.objects;

import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;
import static com.binbo.glvideo.core.ext.CommonExtKt.dip;
import static com.binbo.glvideo.core.utils.Constants.BYTES_PER_FLOAT;
import static com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardBottom;
import static com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardLeft;
import static com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardRight;
import static com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig.missionCardTop;

import com.binbo.glvideo.core.opengl.data.VertexArray;
import com.binbo.glvideo.sample_app.impl.advanced.namecard.program.NameCardShaderProgram;

public class NameCard {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    private static final float borderSize = dip(4f);
    private static final float cornerRadius = dip(24f);

    private static final float[] VERTEX_DATA = {
            // Order of coordinates: X, Y, S, T

            // Triangle Fan
            missionCardLeft, missionCardBottom, 0f, 0f,
            missionCardRight, missionCardBottom, 1f, 0f,
            missionCardLeft, missionCardTop, 0f, 1f,
            missionCardRight, missionCardTop, 1f, 1f};

    private final VertexArray vertexArray;

    public NameCard() {
        vertexArray = new VertexArray(VERTEX_DATA);
    }

    public void bindData(NameCardShaderProgram textureProgram) {
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

    public float getBorderSize() {
        return borderSize;
    }

    public float getCornerRadius() {
        return cornerRadius;
    }
}

