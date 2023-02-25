package com.binbo.glvideo.sample_app.ui.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withSave

class CircleCoverView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var coverColor = Color.parseColor("#99000000") //遮罩的颜色

    private val bounds = RectF()
    private val clipPath = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        //设置离屏缓冲的范围
        bounds.set(0f, 0f, width.toFloat(), height.toFloat())
        //设置Clip Path的矩形区域
        clipPath.addCircle(w / 2f, h / 2f, Integer.min(w, h) / 2f, Path.Direction.CW)
//        clipPath.addRoundRect(mPadding, mPadding, width - mPadding, height - mPadding, mRoundCorner, mRoundCorner, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        // Canvas的离屏缓冲
        val count = canvas.saveLayer(bounds, paint)
        // KTX的扩展函数相当于对Canvas的 save 和 restore 操作
        canvas.withSave {
            //画遮罩的颜色
            canvas.drawColor(coverColor)
            //按Path来裁切
            canvas.clipPath(clipPath)
            //画镂空的范围
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC)
        }
        // 把离屏缓冲的内容,绘制到View上去
        canvas.restoreToCount(count)
    }
}