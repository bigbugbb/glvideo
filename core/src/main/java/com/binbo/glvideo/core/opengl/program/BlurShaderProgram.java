package com.binbo.glvideo.core.opengl.program;

import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.binbo.glvideo.core.R;


public class BlurShaderProgram extends ShaderProgram {

    // Uniform locations
    private final int uMatrixLocation;
    private final int uTextureUnitLocation;
    private final int uBlurRadiusLocation;
    private final int uBlurOffsetLocation;
    private final int uSumWeightLocation;

    // Attribute locations
    private final int aPositionLocation;
    private final int aTextureCoordinatesLocation;

    public BlurShaderProgram(Context context) {
        super(context, R.raw.camera_blur_vertex_shader, R.raw.camera_blur_fragment_shader);

        // Retrieve uniform locations for the shader program.
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
        uTextureUnitLocation = glGetUniformLocation(program, U_TEXTURE_UNIT);
        uBlurRadiusLocation = GLES20.glGetUniformLocation(program, "uBlurRadius"); // int
        uBlurOffsetLocation = GLES20.glGetUniformLocation(program, "uBlurOffset"); // vec2
        uSumWeightLocation = GLES20.glGetUniformLocation(program, "uSumWeight"); // float

        // Retrieve attribute locations for the shader program.
        aPositionLocation = glGetAttribLocation(program, A_POSITION);
        aTextureCoordinatesLocation = glGetAttribLocation(program, A_TEXTURE_COORDINATES);
    }

    public void setUniforms(float[] matrix, int textureId, boolean vertical, int blurRadius, float blurOffset, float sumWeight) {
        // Pass the matrix into the shader program.
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);



        // Set the active texture unit to texture unit 0.
        glActiveTexture(GL_TEXTURE0);

        // Bind the texture to this unit.
        glBindTexture(GL_TEXTURE_2D, textureId);

        GLES20.glUniform1i(uBlurRadiusLocation, blurRadius);

        if (vertical) {
            GLES20.glUniform2f(uBlurOffsetLocation, 0.0f, blurOffset);
        } else {
            GLES20.glUniform2f(uBlurOffsetLocation, blurOffset, 0.0f);
        }

//        GLES20.glUniform1f(uSumWeightLocation, 0.99683464f);
        GLES20.glUniform1f(uSumWeightLocation, sumWeight);
        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0.
        glUniform1i(uTextureUnitLocation, 0);

    }

    public int getPositionAttributeLocation() {
        return aPositionLocation;
    }

    public int getTextureCoordinatesAttributeLocation() {
        return aTextureCoordinatesLocation;
    }

    private int scaleRatio;
    private int blurRadius;
    private float blurOffsetW;
    private float blurOffsetH;
    private float sumWeight;

    /**
     * 计算总权重
     */
    private void calculateSumWeight() {
        if (blurRadius < 1) {
            setSumWeight(0);
            return;
        }

        float sumWeight = 0;
        float sigma = blurRadius / 3f;
        for (int i = 0; i < blurRadius; i++) {
            float weight = (float) ((1 / Math.sqrt(2 * Math.PI * sigma * sigma)) * Math.exp(-(i * i) / (2 * sigma * sigma)));
            sumWeight += weight;
            if (i != 0) {
                sumWeight += weight;
            }
        }

        setSumWeight(sumWeight);
    }

    public void setSumWeight(float sumWeight) {
        Log.d("debug", "setSumWeight: " + sumWeight);
        this.sumWeight = sumWeight;
    }
}
