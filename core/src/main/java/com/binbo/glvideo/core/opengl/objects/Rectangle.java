package com.binbo.glvideo.core.opengl.objects;

import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;

import com.binbo.glvideo.core.opengl.data.VertexArray;
import com.binbo.glvideo.core.opengl.program.RCTextureShaderProgram;
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram;

/**
 * @author bigbug
 * @project lobby
 * @date 2022/11/18
 * @time 21:25
 */
public class Rectangle {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * 4;
    
    private float width = 1f;
    private float height = 1f;

    private static final float[] VERTEX_DATA = {
            // Order of coordinates: X, Y, S, T
            -1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, -1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 1.0f, 0.0f};

    private VertexArray vertexArray;

    public Rectangle() {
        vertexArray = new VertexArray(VERTEX_DATA);
    }

    public Rectangle(float width, float height) {
        updateVertices(width, height);
    }

    public void updateVertices(float width, float height) {
        this.width = width;
        this.height = height;
        vertexArray = new VertexArray(new float[]{
                -width, -height, 0.0f, 1.0f,
                width, -height, 1.0f, 1.0f,
                -width, height, 0.0f, 0.0f,
                width, height, 1.0f, 0.0f
        });
    }

    public void updateTextureCoordinates(float x, float y, float w, float h) {
        vertexArray = new VertexArray(new float[]{
                -width, -height, x, y,
                width, -height, x + w, y,
                -width, height, x, y + h,
                width, height, x + w, y + h
        });
    }

    public void updateVertexWithClipping(float clipX, float clipY) {
        if (clipX >= 0.0001) {
            vertexArray = new VertexArray(new float[]{
                    -width, -height, 0.0f + clipX, 1.0f,
                    width, -height, 1.0f - clipX, 1.0f,
                    -width, height, 0.0f + clipX, 0.0f,
                    width, height, 1.0f - clipX, 0.0f
            });
        } else if (clipY >= 0.0001) {
            vertexArray = new VertexArray(new float[]{
                    -width, -height, 0.0f, 1 - clipY,
                    width, -height, 1.0f, 1 - clipY,
                    -width, height, 0.0f, clipY,
                    width, height, 1.0f, clipY
            });
        }
    }

    public void updateVertexWithClippingAndFlipping(float clipX, float clipY, boolean flipX, boolean flipY) {
        vertexArray = new VertexArray(new float[]{
                -width, -height, flipX ? 1 - clipX : clipX, flipY ? clipY : 1.0f - clipY,
                width, -height, flipX ? clipX : 1.0f - clipX, flipY ? clipY : 1.0f - clipY,
                -width, height, flipX ? 1 - clipX : clipX, flipY ? 1f - clipY : clipY,
                width, height, flipX ? clipX : 1.0f - clipX, flipY ? 1f - clipY : clipY
        });
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
}
