precision mediump float;

uniform float u_CornerRadius;
uniform float u_BorderSize;
uniform vec2 u_Resolution;

varying vec2 v_TextureCoords;

vec4 borderColor = vec4(0.4, 0.4, 0.4, 0.25);

float dot(vec2 v) {
    return v.x * v.x + v.y * v.y;
}

void setupBorder(vec2 xy) {
    if (xy.x <= u_BorderSize || xy.x >= u_Resolution.x - u_BorderSize) {
        gl_FragColor = borderColor;
    } else if (xy.y <= u_BorderSize || xy.y >= u_Resolution.y - u_BorderSize) {
        gl_FragColor = borderColor;
    }
}

void main()
{
    vec2 xy = vec2(v_TextureCoords.x * u_Resolution.x, v_TextureCoords.y * u_Resolution.y);
    float radiusSquare = u_CornerRadius * u_CornerRadius;
    float radiusSquareInner = (u_CornerRadius - u_BorderSize) * (u_CornerRadius - u_BorderSize);

    gl_FragColor = vec4(1, 0, 0, 0);

    if (xy.x <= u_CornerRadius && xy.y <= u_CornerRadius) { // bottom-left
                                                            vec2 center = vec2(u_CornerRadius, u_CornerRadius);
                                                            float distanceToCenter = dot(xy - center);
                                                            if (distanceToCenter > radiusSquare) {
                                                                discard;
                                                            } else if (distanceToCenter >= radiusSquareInner) {
                                                                gl_FragColor = borderColor;
                                                                return;
                                                            }
    } else if (xy.x >= u_Resolution.x - u_CornerRadius && xy.y <= u_CornerRadius) { // bottom-right
                                                                                    vec2 center = vec2(u_Resolution.x - u_CornerRadius, u_CornerRadius);
                                                                                    float distanceToCenter = dot(xy - center);
                                                                                    if (distanceToCenter > radiusSquare) {
                                                                                        discard;
                                                                                    } else if (distanceToCenter >= radiusSquareInner) {
                                                                                        gl_FragColor = borderColor;
                                                                                        return;
                                                                                    }
    } else if (xy.x <= u_CornerRadius && xy.y >= u_Resolution.y - u_CornerRadius) { // top-left
                                                                                    vec2 center = vec2(u_CornerRadius, u_Resolution.y - u_CornerRadius);
                                                                                    float distanceToCenter = dot(xy - center);
                                                                                    if (distanceToCenter > radiusSquare) {
                                                                                        discard;
                                                                                    } else if (distanceToCenter > radiusSquareInner) {
                                                                                        gl_FragColor = borderColor;
                                                                                        return;
                                                                                    }
    } else if (xy.x >= u_Resolution.x - u_CornerRadius && xy.y >= u_Resolution.y - u_CornerRadius) { // top-right
                                                                                                     vec2 center = vec2(u_Resolution.x - u_CornerRadius, u_Resolution.y - u_CornerRadius);
                                                                                                     float distanceToCenter = dot(xy - center);
                                                                                                     if (dot(xy - center) >= radiusSquare) {
                                                                                                         discard;
                                                                                                     } else if (distanceToCenter > radiusSquareInner) {
                                                                                                         gl_FragColor = borderColor;
                                                                                                         return;
                                                                                                     }
    }

    if (v_TextureCoords.y < 0.1) {
        gl_FragColor = vec4(0, 0, 0, 1);
        setupBorder(xy);
    } else if (v_TextureCoords.y <= 0.1015) {
        gl_FragColor = vec4(0.25, 0.25, 0.25, 1);
        setupBorder(xy);
    } else {
        gl_FragColor = vec4(0, 0, 0, 1);
        setupBorder(xy);
    }
}