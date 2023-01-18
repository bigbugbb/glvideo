precision mediump float;

uniform sampler2D u_TextureUnit;
uniform float u_Alpha;
uniform float u_LeftOutsidePortion;
uniform float u_RightInsidePortion;
uniform float u_TopOutsidePortion;
uniform float u_BottomOutsidePortion;

varying vec2 v_TextureCoordinates;

void main()
{
    if (v_TextureCoordinates.x < u_LeftOutsidePortion) {
        discard;
    }

    if (v_TextureCoordinates.x >= u_RightInsidePortion) {
        discard;
    }

    if (v_TextureCoordinates.y < u_TopOutsidePortion) {
        discard;
    }

    if (v_TextureCoordinates.y > u_BottomOutsidePortion) {
        discard;
    }

    gl_FragColor = texture2D(u_TextureUnit, v_TextureCoordinates) * u_Alpha;
}