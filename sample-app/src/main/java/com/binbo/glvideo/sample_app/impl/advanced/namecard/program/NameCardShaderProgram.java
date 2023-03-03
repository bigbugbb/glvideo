package com.binbo.glvideo.sample_app.impl.advanced.namecard.program;

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform2f;
import static android.opengl.GLES20.glUniformMatrix4fv;

import android.content.Context;

import com.binbo.glvideo.core.opengl.program.ShaderProgram;
import com.binbo.glvideo.sample_app.R;

public class NameCardShaderProgram extends ShaderProgram {

    protected static final String U_RESOLUTION = "u_Resolution";
    protected static final String U_CORNER_RADIUS = "u_CornerRadius";
    protected static final String U_BORDER_SIZE = "u_BorderSize";

    // Uniform locations
    private final int uMatrixLocation;
    private final int uResolutionLocation;
    private final int uBorderSize;
    private final int uCornerRadiusLocation;

    // Attribute locations
    private final int aPositionLocation;
    private final int aTextureCoordinatesLocation;

    public NameCardShaderProgram(Context context) {
        super(context, R.raw.mission_card_vertex_shader, R.raw.mission_card_fragment_shader);

        // Retrieve uniform locations for the shader program.
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
        uResolutionLocation = glGetUniformLocation(program, U_RESOLUTION);
        uBorderSize = glGetUniformLocation(program, U_BORDER_SIZE);
        uCornerRadiusLocation = glGetUniformLocation(program, U_CORNER_RADIUS);

        // Retrieve attribute locations for the shader program.
        aPositionLocation = glGetAttribLocation(program, A_POSITION);
        aTextureCoordinatesLocation = glGetAttribLocation(program, A_TEXTURE_COORDINATES);
    }

    public void setUniforms(float[] matrix, float cornerRadius, float borderSize, int resolutionWidth, int resolutionHeight) {
        // Pass the matrix into the shader program.
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);

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