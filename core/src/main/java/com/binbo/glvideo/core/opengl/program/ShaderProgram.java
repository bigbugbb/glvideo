package com.binbo.glvideo.core.opengl.program;

import android.content.Context;
import android.opengl.GLES20;

import com.binbo.glvideo.core.opengl.utils.ShaderHelper;
import com.binbo.glvideo.core.opengl.utils.TextResourceReader;

public abstract class ShaderProgram {
    // Uniform constants
    protected static final String U_MATRIX = "u_Matrix";
    protected static final String U_COLOR = "u_Color";
    protected static final String U_ALPHA = "u_Alpha";
    protected static final String U_TEXTURE_UNIT = "u_TextureUnit";

    // Attribute constants
    protected static final String A_POSITION = "a_Position";
    protected static final String A_COLOR = "a_Color";
    protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";

    // Shader program
    protected final int program;

    protected ShaderProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId) {
        // Compile the shaders and link the program.
        program = ShaderHelper.buildProgram(
                TextResourceReader.readTextFileFromResource(context, vertexShaderResourceId),
                TextResourceReader.readTextFileFromResource(context, fragmentShaderResourceId));
    }

    public void useProgram() {
        // Set the current OpenGL shader program to this program.
        GLES20.glUseProgram(program);
    }

    public void deleteProgram() {
        GLES20.glDeleteProgram(program);
    }
}

