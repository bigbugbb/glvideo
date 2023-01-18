package com.binbo.glvideo.core.opengl.program;

import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;

import android.content.Context;

import com.binbo.glvideo.core.R;


public class ClipTextureShaderProgram extends ShaderProgram {

    private static final String U_LEFT_OUTSIDE_PORTION = "u_LeftOutsidePortion";
    private static final String U_RIGHT_INSIDE_PORTION = "u_RightInsidePortion";
    private static final String U_TOP_OUTSIDE_PORTION = "u_TopOutsidePortion";
    private static final String U_BOTTOM_OUTSIDE_PORTION = "u_BottomOutsidePortion";

    // Uniform locations
    private final int uMatrixLocation;
    private final int uTextureUnitLocation;
    private final int uAlphaLocation;
    private final int uLeftOutsidePortionLocation;
    private final int uRightInsidePortionLocation;
    private final int uTopOutsidePortionLocation;
    private final int uBottomOutsidePortionLocation;

    // Attribute locations
    private final int aPositionLocation;
    private final int aTextureCoordinatesLocation;

    public ClipTextureShaderProgram(Context context) {
        super(context, R.raw.clip_texture_vertex_shader, R.raw.clip_texture_fragment_shader);

        // Retrieve uniform locations for the shader program.
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
        uTextureUnitLocation = glGetUniformLocation(program, U_TEXTURE_UNIT);
        uAlphaLocation = glGetUniformLocation(program, U_ALPHA);

        uLeftOutsidePortionLocation = glGetUniformLocation(program, U_LEFT_OUTSIDE_PORTION);
        uRightInsidePortionLocation = glGetUniformLocation(program, U_RIGHT_INSIDE_PORTION);
        uTopOutsidePortionLocation = glGetUniformLocation(program, U_TOP_OUTSIDE_PORTION);
        uBottomOutsidePortionLocation = glGetUniformLocation(program, U_BOTTOM_OUTSIDE_PORTION);

        // Retrieve attribute locations for the shader program.
        aPositionLocation = glGetAttribLocation(program, A_POSITION);
        aTextureCoordinatesLocation = glGetAttribLocation(program, A_TEXTURE_COORDINATES);
    }

    public void setUniforms(float[] matrix, int textureId, float alpha,
                            float leftOutPortion, float rightInsidePortion,
                            float topOutPortion, float bottomOutPortion) {
        // Pass the matrix into the shader program.
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);

        // Pass the vector of out portion into the shader program.
        glUniform1f(uLeftOutsidePortionLocation, leftOutPortion);
        glUniform1f(uRightInsidePortionLocation, rightInsidePortion);
        glUniform1f(uTopOutsidePortionLocation, topOutPortion);
        glUniform1f(uBottomOutsidePortionLocation, bottomOutPortion);

        // Set the active texture unit to texture unit 0.
        glActiveTexture(GL_TEXTURE0);

        // Bind the texture to this unit.
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0.
        glUniform1i(uTextureUnitLocation, 0);

        glUniform1f(uAlphaLocation, alpha);
    }

    public int getPositionAttributeLocation() {
        return aPositionLocation;
    }

    public int getTextureCoordinatesAttributeLocation() {
        return aTextureCoordinatesLocation;
    }
}

