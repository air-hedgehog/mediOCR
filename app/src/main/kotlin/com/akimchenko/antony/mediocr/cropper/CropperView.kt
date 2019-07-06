package com.akimchenko.antony.mediocr.cropper

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.akimchenko.antony.mediocr.R
import kotlin.math.sqrt


class CropperView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private val canvas = Canvas()
    private val paint = Paint()
    private val threeColors = arrayOf(
        R.color.cropper_fill1,
        R.color.cropper_fill2,
        R.color.cropper_fill3
    )
    private var groupId: Int = -1
    private var touchedNodeId: Int = -1
    private var currentRectangle: CropRectangle? = null

    private val rectanglesList = ArrayList<CropRectangle>()

    init {
        isFocusable = true
        addRectangle(0)
    }

    fun addRectangle(colorIndex: Int) {
        rectanglesList.forEach { it.setNodesEnabled(false) }
        val newRectangle = CropRectangle(threeColors[colorIndex])
        currentRectangle?.setNodesEnabled(false)
        currentRectangle = newRectangle
        rectanglesList.add(newRectangle)
        invalidate()
    }

    fun getRectanglesList(): ArrayList<CropRectangle> = rectanglesList

    fun removeRectangle(rectangle: CropRectangle) {
        rectanglesList.remove(rectangle)
        invalidate()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        this.setOnClickListener {
            currentRectangle?.setNodesEnabled(!(currentRectangle?.isNodesEnabled() ?: false))
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        val nodesList = currentRectangle?.nodesList ?: return
        paint.isAntiAlias = true
        paint.isDither = true
        paint.color = ContextCompat.getColor(context, R.color.cropper_fill1)
        paint.style = Paint.Style.FILL
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 1.0f
        //canvas.drawPaint(paint)
        if (groupId == 1) {
            canvas.drawRect(
                currentRectangle!!.point1.x + nodesList[0].getWidthOfNode() / 2.0f,
                currentRectangle!!.point3.y + nodesList[2].getWidthOfNode() / 2.0f,
                currentRectangle!!.point3.x + nodesList[2].getWidthOfNode() / 2.0f,
                currentRectangle!!.point1.y + nodesList[0].getWidthOfNode() / 2.0f, paint
            )
        } else {
            canvas.drawRect(
                currentRectangle!!.point2.x + nodesList[1].getWidthOfNode() / 2.0f,
                currentRectangle!!.point4.y + nodesList[3].getWidthOfNode() / 2.0f,
                currentRectangle!!.point4.x + nodesList[3].getWidthOfNode() / 2.0f,
                currentRectangle!!.point2.y + nodesList[1].getWidthOfNode() / 2.0f, paint
            )
        }
        nodesList.forEach {
            it.bitmap?.let { bitmap ->
                canvas.drawBitmap(bitmap, it.point.x.toFloat(), it.point.y.toFloat(), Paint())
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        val nodesList = currentRectangle?.nodesList ?: return false
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
                        sqrt(((centerX - x) * (centerX - x) + (centerY - y) * (centerY - y)).toDouble())
                    if (radCircle < node.getWidthOfNode()) {
                        touchedNodeId = node.getId()
                        if (touchedNodeId == 1 || touchedNodeId == 3) {
                            groupId = 2
                            canvas.drawRect(
                                currentRectangle!!.point1.x.toFloat(),
                                currentRectangle!!.point3.y.toFloat(),
                                currentRectangle!!.point3.x.toFloat(),
                                currentRectangle!!.point1.y.toFloat(),
                                paint
                            )
                        } else {
                            groupId = 1
                            canvas.drawRect(
                                currentRectangle!!.point2.x.toFloat(),
                                currentRectangle!!.point4.y.toFloat(),
                                currentRectangle!!.point4.x.toFloat(),
                                currentRectangle!!.point2.y.toFloat(),
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

                if (touchedNodeId >= 0 && touchedNodeId < nodesList.size) {

                    nodesList[touchedNodeId].point.x = x.toInt() - halfWidth
                    nodesList[touchedNodeId].point.y = y.toInt() - halfHeight

                    if (groupId == 1) {
                        nodesList[1].point.x = nodesList[0].point.x
                        nodesList[1].point.y = nodesList[2].point.y
                        nodesList[3].point.x = nodesList[2].point.x
                        nodesList[3].point.y = nodesList[0].point.y
                        canvas.drawRect(
                            currentRectangle!!.point1.x.toFloat(),
                            currentRectangle!!.point3.y.toFloat(),
                            currentRectangle!!.point3.x.toFloat(),
                            currentRectangle!!.point1.y.toFloat(),
                            paint
                        )
                    } else {
                        nodesList[0].point.x = nodesList[1].point.x
                        nodesList[0].point.y = nodesList[3].point.y
                        nodesList[2].point.x = nodesList[3].point.x
                        nodesList[2].point.y = nodesList[1].point.y
                        canvas.drawRect(
                            currentRectangle!!.point2.x.toFloat(),
                            currentRectangle!!.point4.y.toFloat(),
                            currentRectangle!!.point4.x.toFloat(),
                            currentRectangle!!.point2.y.toFloat(),
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

    inner class CropRectangle(colorResId: Int) {

        val point1: Point = Point(50, 20)
        val point2: Point = Point(150, 20)
        val point3: Point = Point(150, 120)
        val point4: Point = Point(50, 120)
        private val color = ContextCompat.getColor(context, colorResId)

        fun getColor() = color

        private var isNodesEnabled: Boolean = true

        val nodesList = arrayListOf(
            DraggableNode(context, R.drawable.node_cross, point1),
            DraggableNode(context, R.drawable.node_cross, point2),
            DraggableNode(context, R.drawable.node_cross, point3),
            DraggableNode(context, R.drawable.node_cross, point4)
        )

        fun setNodesEnabled(isEnabled: Boolean) {
            this.isNodesEnabled = isEnabled
            nodesList.forEach { if (isEnabled) it.show() else it.hide() }
        }

        fun isNodesEnabled(): Boolean = isNodesEnabled
    }

}