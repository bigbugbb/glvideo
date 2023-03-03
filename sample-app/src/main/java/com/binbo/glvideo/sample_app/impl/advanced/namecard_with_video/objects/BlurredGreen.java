package com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.objects;

import static java.lang.Math.abs;

public class BlurredGreen extends BlurredElement {

    public final static float startX = 0f;
    public final static float startY = -1.5f;
    public final static float startZ = -0.5f;

    public BlurredGreen() {
        x = startX;
        y = startY;
        z = startZ;
        speed = 3f / 1200; // 每秒60帧，20秒
        direction = 1; // 向上
    }

    @Override
    public void update() {
        // Pink: Move by Y axis, -600 to 600, over 20 second, back & forth
        y += speed * direction;
        if (abs(y) > 1.5f) {
            direction *= -1;
        }
    }
}