package com.akimchenko.antony.mediocr.cropper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.akimchenko.antony.mediocr.R

class CropperView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val point1: Point = Point(50, 20)
    private val point2: Point = Point(150, 20)
    private val point3: Point = Point(150, 120)
    private val point4: Point = Point(50, 102)

    private val canvas = Canvas()
    private val paint = Paint()
    private var groupId: Int = -1

    private val nodesList = arrayListOf(
        DraggableNode(context, R.drawable.node_cross, point1),
        DraggableNode(context, R.drawable.node_cross, point2),
        DraggableNode(context, R.drawable.node_cross, point3),
        DraggableNode(context, R.drawable.node_cross, point4)
    )

    init {
        isFocusable = true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return

        paint.isAntiAlias = true
        paint.isDither = true
        paint.color = ContextCompat.getColor(context, R.color.cropper_fill)
        paint.style = Paint.Style.FILL
        paint.strokeJoin = Paint.Join.ROUND
        //paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 5.0f
        canvas.drawPaint(paint)
        if (groupId == 1) {
            canvas.drawRect(point1.x + nodesList[0].getWidthOfNode() / 2.0f,
                    point3.y + nodesList[2].getWidthOfNode() / 2.0f,
                    point3.x + nodesList[2].getWidthOfNode() / 2.0f,
                    point1.y + nodesList[0].getWidthOfNode() /2.0f, paint)
        } else {
            canvas.drawRect(point2.x + nodesList[1].getWidthOfNode() / 2.0f,
                    point4.y + nodesList[3].getWidthOfNode() / 2.0f,
                    point4.x + nodesList[3].getWidthOfNode() / 2.0f,
                    point2.y + nodesList[1].getWidthOfNode() /2.0f, paint)
        }
        //val bitmapDrawable = BitmapDrawable()
        nodesList.forEach { canvas.drawBitmap(it.bitmap, it.point.x, it.point.y, Paint()) }
    }
}