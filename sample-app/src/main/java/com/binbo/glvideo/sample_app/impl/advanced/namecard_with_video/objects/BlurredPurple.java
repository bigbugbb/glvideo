package com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.objects;

public class BlurredPurple extends BlurredElement {

    public final static float startX = 0.7f;
    public final static float startY = 0.75f;
    public final static float startZ = -0.5f;

    public BlurredPurple() {
        x = startX;
        y = startY;
        z = startZ;
        speed = 2.5f / 1200; // 每秒60帧，20秒
        direction = 1; // 向上
    }

    @Override
    public void update() {
        // Purple: Move by [-400, 600], over 20 second, back and forth (back means [400, -600] here)
        y += speed * direction;
        if (y > 1.5f || y < -1f) {
            direction *= -1;
        }
    }
}