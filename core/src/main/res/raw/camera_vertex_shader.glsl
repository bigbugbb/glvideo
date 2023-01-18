//  顶点坐标
attribute vec4 a_Position;
//  纹理坐标
attribute vec4 a_TextureCoordinates;

uniform mat4 u_Matrix;
//  传给片元着色器的像素点
varying vec2 v_Coord;

void main() {
    gl_Position = a_Position;
    v_Coord = (u_Matrix * vec4(a_TextureCoordinates.x, a_TextureCoordinates.y, 1.0, 1.0)).xy;
}
