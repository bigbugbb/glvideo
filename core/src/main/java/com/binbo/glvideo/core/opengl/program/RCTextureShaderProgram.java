package com.binbo.glvideo.core.opengl.program;

import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform2f;
import static android.opengl.GLES20.glUniformMatrix4fv;

import android.content.Context;

import com.binbo.glvideo.core.R;

/**
 * Round corner texture shader program
 */
public class RCTextureShaderProgram extends ShaderProgram {

    protected static final String U_RESOLUTION = "u_Resolution";
    protected static final String U_CORNER_RADIUS = "u_CornerRadius";
    protected static final String U_BORDER_SIZE = "u_BorderSize";

    // Uniform locations
    private final int uMatrixLocation;
    private final int uTextureUnitLocation;
    private final int uResolutionLocation;
    private final int uBorderSize;
    private final int uCornerRadiusLocation;

    // Attribute locations
    private final int aPositionLocation;
    private final int aTextureCoordinatesLocation;

    public RCTextureShaderProgram(Context context) {
        super(context, R.raw.rc_texture_vertex_shader, R.raw.rc_texture_fragment_shader);

        // Retrieve uniform locations for the shader program.
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
        uTextureUnitLocation = glGetUniformLocation(program, U_TEXTURE_UNIT);
        uResolutionLocation = glGetUniformLocation(program, U_RESOLUTION);
        uBorderSize = glGetUniformLocation(program, U_BORDER_SIZE);
        uCornerRadiusLocation = glGetUniformLocation(program, U_CORNER_RADIUS);

        // Retrieve attribute locations for the shader program.
        aPositionLocation = glGetAttribLocation(program, A_POSITION);
        aTextureCoordinatesLocation = glGetAttribLocation(program, A_TEXTURE_COORDINATES);
    }

    public void setUniforms(float[] matrix, int textureId, float cornerRadius, float borderSize, int resolutionWidth, int resolutionHeight) {
        // Pass the matrix into the shader program.
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);

        // Set the active texture unit to texture unit 0.
        glActiveTexture(GL_TEXTURE0);

        // Bind the texture to this unit.
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0.
        glUniform1i(uTextureUnitLocation, 0);

        glUniform1f(uBorderSize, borderSize);
        glUniform1f(uCornerRadiusLocation, cornerRadius);
        glUniform2f(uResolutionLocation, resolutionWidth, resolutionHeight);
    }

    public int getPositionAttributeLocation() {
        return aPositionLocation;
    }

    public int getTextureCoordinatesAttributeLocation() {
        return aTextureCoordinatesLocation;
    }
}
