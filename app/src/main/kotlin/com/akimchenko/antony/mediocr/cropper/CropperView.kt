package com.akimchenko.antony.mediocr.cropper

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.akimchenko.antony.mediocr.R


class CropperView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private val point1: Point = Point(50, 20)
    private val point2: Point = Point(150, 20)
    private val point3: Point = Point(150, 120)
    private val point4: Point = Point(50, 120)

    private val canvas = Canvas()
    private val paint = Paint()
    private var groupId: Int = -1
    private var touchedNodeId: Int = -1

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
        paint.color = ContextCompat.getColor(context, R.color.cropper_fill1)
        paint.style = Paint.Style.FILL
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 1.0f
        canvas.drawPaint(paint)
        if (groupId == 1) {
            canvas.drawRect(
                point1.x + nodesList[0].getWidthOfNode() / 2.0f,
                point3.y + nodesList[2].getWidthOfNode() / 2.0f,
                point3.x + nodesList[2].getWidthOfNode() / 2.0f,
                point1.y + nodesList[0].getWidthOfNode() / 2.0f, paint
            )
        } else {
            canvas.drawRect(
                point2.x + nodesList[1].getWidthOfNode() / 2.0f,
                point4.y + nodesList[3].getWidthOfNode() / 2.0f,
                point4.x + nodesList[3].getWidthOfNode() / 2.0f,
                point2.y + nodesList[1].getWidthOfNode() / 2.0f, paint
            )
        }
        //val bitmapDrawable = BitmapDrawable()
        nodesList.forEach { canvas.drawBitmap(it.bitmap, it.point.x.toFloat(), it.point.y.toFloat(), Paint()) }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //return super.onTouchEvent(event)
        event ?: return false
        val eventAction = event.action
        val x = event.x.coerceIn(left.toFloat(), right.toFloat())
        val y = event.y.coerceIn(top.toFloat(), bottom.toFloat())

        val randomNode = nodesList.first()
        val halfHeight = randomNode.getHeightOfNode() / 2
        val halfWidth = randomNode.getWidthOfNode() / 2

        when (eventAction) {
            MotionEvent.ACTION_DOWN -> {
                touchedNodeId = -1
                groupId = -1
                for (node in nodesList) {
                    val centerX = node.point.x + node.getWidthOfNode() / 2
                    val centerY = node.point.y + node.getHeightOfNode() / 2
                    val radCircle =
                        Math.sqrt(((centerX - x) * (centerX - x) + (centerY - y) * (centerY - y)).toDouble())
                    if (radCircle < node.getWidthOfNode()) {
                        touchedNodeId = node.getId()
                        if (touchedNodeId == 1 || touchedNodeId == 3) {
                            groupId = 2
                            canvas.drawRect(
                                point1.x.toFloat(),
                                point3.y.toFloat(),
                                point3.x.toFloat(),
                                point1.y.toFloat(),
                                paint
                            )
                        } else {
                            groupId = 1
                            canvas.drawRect(
                                point2.x.toFloat(),
                                point4.y.toFloat(),
                                point4.x.toFloat(),
                                point2.y.toFloat(),
                                paint
                            )
                        }
                        invalidate()
                        break
                    }
                    invalidate()
                }
            }

            MotionEvent.ACTION_MOVE -> {

                if (touchedNodeId >= 0) {

                    nodesList[touchedNodeId].point.x = x.toInt() - halfWidth
                    nodesList[touchedNodeId].point.y = y.toInt() - halfHeight

                    if (groupId == 1) {
                        nodesList[1].point.x = nodesList[0].point.x
                        nodesList[1].point.y = nodesList[2].point.y
                        nodesList[3].point.x = nodesList[2].point.x
                        nodesList[3].point.y = nodesList[0].point.y
                        canvas.drawRect(
                            point1.x.toFloat(),
                            point3.y.toFloat(),
                            point3.x.toFloat(),
                            point1.y.toFloat(),
                            paint
                        )
                    } else {
                        nodesList[0].point.x = nodesList[1].point.x
                        nodesList[0].point.y = nodesList[3].point.y
                        nodesList[2].point.x = nodesList[3].point.x
                        nodesList[2].point.y = nodesList[1].point.y
                        canvas.drawRect(
                            point2.x.toFloat(),
                            point4.y.toFloat(),
                            point4.x.toFloat(),
                            point2.y.toFloat(),
                            paint
                        )
                    }
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
            }
        }
        invalidate()
        return true
    }
}