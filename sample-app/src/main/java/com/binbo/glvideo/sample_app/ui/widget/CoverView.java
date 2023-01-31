package com.binbo.glvideo.sample_app.ui.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.binbo.glvideo.sample_app.R;

public class CoverView extends FrameLayout {

    Path leftTopPath = new Path();
    Path rightTopPath = new Path();
    Path leftBottomPath = new Path();
    Path rightBottomPath = new Path();
    private RectF tempRectF = new RectF();
    private Rect tempRect = new Rect();
    private Paint beforePaint;
    private Paint coverPaint;
    private boolean hasSet;
    private int leftTopRadius, leftBottomRadius, rightTopRadius, rightBottomRadius;

    public void setRadius(int leftTopRadius, int leftBottomRadius,int rightBottomRadius, int rightTopRadius) {
        this.leftTopRadius = leftTopRadius;
        this.leftBottomRadius = leftBottomRadius;
        this.rightBottomRadius = rightBottomRadius;
        this.rightTopRadius = rightTopRadius;
    }

    public CoverView(@NonNull Context context) {
        super(context);
        init(context, null);

    }

    public CoverView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);

    }

    public CoverView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CoverView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        if (Build.VERSION.SDK_INT >= 11 || isInEditMode()) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CoverView);
        leftTopRadius = leftBottomRadius = rightTopRadius = rightBottomRadius = a.getDimensionPixelSize(R.styleable
                .CoverView_cover_radius, 0);
        leftTopRadius = a.getDimensionPixelSize(R.styleable.CoverView_cover_leftTopRadius, leftTopRadius);
        rightTopRadius = a.getDimensionPixelSize(R.styleable.CoverView_cover_rightTopRadius, rightTopRadius);
        leftBottomRadius = a.getDimensionPixelSize(R.styleable.CoverView_cover_leftBottomRadius, leftBottomRadius);
        rightBottomRadius = a.getDimensionPixelSize(R.styleable.CoverView_cover_rightBottomRadius, rightBottomRadius);
        a.recycle();
        coverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        coverPaint.setColor(Color.TRANSPARENT);
        coverPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        beforePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        setWillNotDraw(false);
    }

    @Override
    public void setBackground(Drawable background) {
        if (background instanceof ColorDrawable) {
            if (background.getAlpha() == 255) {
                background.setAlpha(254);
                background = new ColorDrawable(((ColorDrawable) background).getColor());
            }
        }
        super.setBackground(background);
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            if (background instanceof ColorDrawable) {
                if (background.getAlpha() == 255) {
                    background.setAlpha(254);
                    background = new ColorDrawable(((ColorDrawable) background).getColor());
                }
            }
        }
        super.setBackgroundDrawable(background);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public void draw(Canvas canvas) {
        final boolean drawRadius = !(leftTopRadius == 0 && leftBottomRadius == 0
                && rightTopRadius == 0 && rightBottomRadius == 0);
        int saveCount = 0;
        if (drawRadius) {
//            final int width = getWidth();
//            final float halfWidth = getWidth() / 2f;
//            final float halfHeight = getHeight() / 2f;
//            final float minSize = halfWidth > halfHeight ? halfWidth : halfHeight;
//            final float leftTop = width > 2 * leftTopRadius ? leftTopRadius : minSize / 2f;
//            final float rightTop = width > 2 * rightTopRadius ? rightTopRadius : minSize / 2f;
//            final float leftBottom = width > 2 * leftBottomRadius ? leftBottomRadius : minSize / 2f;
//            final float rightBottom = width > 2 * rightBottomRadius ? rightBottomRadius : minSize / 2f;
//            Path p = new Path();
//            p.moveTo(0, leftTop / 2f);
//            tempRectF.set(0, 0, leftTop, leftTop);
//            p.arcTo(tempRectF, 180, 90, false);
//
//            p.lineTo(getWidth() - rightTop / 2f, 0);
//            tempRectF.set(getWidth() - rightTop, 0, getWidth(), rightTop);
//            p.arcTo(tempRectF, 270, 90, false);
//
//            p.lineTo(getWidth(), getHeight() - rightBottom / 2f);
//            tempRectF.set(getWidth() - rightBottom, getHeight() - rightBottom, getWidth(), getHeight());
//            p.arcTo(tempRectF, 0, 90, false);
//
//            p.lineTo(leftBottom / 2, getHeight());
//            tempRectF.set(0, getHeight() - leftBottom, leftBottom, getHeight());
//            p.arcTo(tempRectF, 90, 90, false);
//
//            p.lineTo(0, leftTop / 2f);
//
//            saveCount = canvas.save();
//            canvas.clipPath(p);
        }

        super.draw(canvas);

        if (drawRadius) {
//            canvas.restoreToCount(saveCount);
            saveCount = canvas.save();
            final int width = getWidth();
            final float halfWidth = getWidth() / 2f;
            final float halfHeight = getHeight() / 2f;
            final float minSize = halfWidth > halfHeight ? halfWidth : halfHeight;
            final float leftTop = width > 2 * leftTopRadius ? leftTopRadius : minSize;
            final float rightTop = width > 2 * rightTopRadius ? rightTopRadius : minSize;
            final float leftBottom = width > 2 * leftBottomRadius ? leftBottomRadius : minSize;
            final float rightBottom = width > 2 * rightBottomRadius ? rightBottomRadius : minSize;
            leftTopPath.reset();
            leftTopPath.moveTo(0, leftTop/* / 2f*/);
            tempRectF.set(0, 0, leftTop * 2, leftTop * 2);
            leftTopPath.arcTo(tempRectF, 180, 90, false);
            leftTopPath.lineTo(0, 0);
            leftTopPath.lineTo(0, leftTop/* / 2f*/);
            canvas.drawPath(leftTopPath, coverPaint);

            rightTopPath.reset();
            rightTopPath.moveTo(getWidth() - rightTop/* / 2f*/, 0);
            tempRectF.set(getWidth() - rightTop * 2, 0, getWidth(), rightTop * 2);
            rightTopPath.arcTo(tempRectF, 270, 90, false);
            rightTopPath.lineTo(getWidth(), 0);
            rightTopPath.lineTo(getWidth() - rightTop/* / 2f*/, 0);
            canvas.drawPath(rightTopPath, coverPaint);

            rightBottomPath.reset();
            rightBottomPath.moveTo(getWidth(), getHeight() - rightBottom/* / 2f*/);
            tempRectF.set(getWidth() - rightBottom * 2, getHeight() - rightBottom * 2, getWidth(), getHeight());
            rightBottomPath.arcTo(tempRectF, 0, 90, false);
            rightBottomPath.lineTo(getWidth(), getHeight());
            rightBottomPath.lineTo(getWidth(), getHeight() - rightBottom/* / 2f*/);
            canvas.drawPath(rightBottomPath, coverPaint);

            leftBottomPath.reset();
            leftBottomPath.moveTo(leftBottom/* / 2f*/, getHeight());
            tempRectF.set(0, getHeight() - leftBottom * 2, leftBottom * 2, getHeight());
            leftBottomPath.arcTo(tempRectF, 90, 90, false);
            leftBottomPath.lineTo(0, getHeight());
            leftBottomPath.lineTo(leftBottom/* / 2f*/, getHeight());
            canvas.drawPath(leftBottomPath, coverPaint);
            canvas.restoreToCount(saveCount);
        }
    }

    @Override
    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }
}