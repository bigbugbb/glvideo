package com.binbo.glvideo.core.opengl.objects;

import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;

import com.binbo.glvideo.core.opengl.data.VertexArray;
import com.binbo.glvideo.core.opengl.program.RCTextureShaderProgram;
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram;


public class Frame {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * 4;

    private float borderSize = 0;
    private float cornerRadius = 0;

    private static final float[] VERTEX_DATA = {
            // Order of coordinates: X, Y, S, T
            -1.0f, -1.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f};

    private VertexArray vertexArray;

    public Frame() {
        vertexArray = new VertexArray(VERTEX_DATA);
    }

    public void updateVertexWithClipping(float clipX, float clipY) {
        if (clipX >= 0.0001) {
            vertexArray = new VertexArray(new float[]{
                    -1.0f, -1.0f, 0.0f + clipX, 0.0f,
                    1.0f, -1.0f, 1.0f - clipX, 0.0f,
                    -1.0f, 1.0f, 0.0f + clipX, 1.0f,
                    1.0f, 1.0f, 1.0f - clipX, 1.0f
            });
        } else if (clipY >= 0.0001) {
            vertexArray = new VertexArray(new float[]{
                    -1.0f, -1.0f, 0.0f, 0.0f + clipY,
                    1.0f, -1.0f, 1.0f, 0.0f + clipY,
                    -1.0f, 1.0f, 0.0f, 1.0f - clipY,
                    1.0f, 1.0f, 1.0f, 1.0f - clipY
            });
        }
    }

    public void bindData(TextureShaderProgram frameProgram) {
        vertexArray.setVertexAttribPointer(
                0,
                frameProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE);

        vertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                frameProgram.getTextureCoordinatesAttributeLocation(),
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                STRIDE);
    }

    public void bindData(RCTextureShaderProgram rcFrameProgram) {
        vertexArray.setVertexAttribPointer(
                0,
                rcFrameProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE);

        vertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                rcFrameProgram.getTextureCoordinatesAttributeLocation(),
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                STRIDE);
    }

    public void draw() {
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }

    public float getBorderSize() {
        return borderSize;
    }

    public void setBorderSize(float borderSize) {
        this.borderSize = borderSize;
    }

    public float getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
    }
}
