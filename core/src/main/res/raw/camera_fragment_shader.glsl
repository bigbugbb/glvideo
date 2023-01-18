#extension GL_OES_EGL_image_external : require

precision mediump float;

//采样点的坐标
varying vec2 v_Coord;

//采样器
uniform samplerExternalOES u_TextureUnit;

void main() {
    gl_FragColor = texture2D(u_TextureUnit, v_Coord);
}

