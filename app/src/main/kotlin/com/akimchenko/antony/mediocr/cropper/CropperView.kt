package com.akimchenko.antony.mediocr.cropper

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import com.akimchenko.antony.mediocr.R
import kotlin.math.sqrt


class CropperView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private val canvas = Canvas()
    private val paint = Paint()
    private val threeColors = arrayOf(
        R.color.cropper_fill1_alpha,
        R.color.cropper_fill2_alpha,
        R.color.cropper_fill3_alpha
    )
    private var groupId: Int = -1
    private var touchedNodeId: Int = -1
    private var currentRectangle: CropRectangle? = null

    private val inactiveRectangles = arrayOfNulls<CropRectangle?>(3)

    init {
        isFocusable = true
        addRectangle(0)
    }

    fun addRectangle(slotIndex: Int) {
        currentRectangle?.setColorId(threeColors[slotIndex])

        if (currentRectangle != null) {
            currentRectangle!!.setNodesEnabled(false)
            inactiveRectangles[slotIndex] = currentRectangle
        }

        currentRectangle = CropRectangle(context, threeColors[slotIndex])
        currentRectangle?.setNodesEnabled(true)
        invalidate()
    }

    fun getRectanglesList(): Array<CropRectangle?> = inactiveRectangles

    fun removeRectangle(slotIndex: Int) {
        inactiveRectangles[slotIndex] = null
        invalidate()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        this.setOnClickListener {
            currentRectangle?.setNodesEnabled(!(currentRectangle?.isNodesEnabled() ?: false))
        }
    }

    private fun setPaintParametersForRectangle(paint: Paint, color: Int): Paint {
        paint.isAntiAlias = true
        paint.isDither = true
        paint.color = color
        paint.style = Paint.Style.FILL
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 1.0f
        return paint
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        currentRectangle ?: return

        val nodesList = currentRectangle!!.nodesList
        setPaintParametersForRectangle(paint, currentRectangle!!.getColor())

        inactiveRectangles.forEach { rectangle ->
            if (rectangle != null) {
                val oldPaint = setPaintParametersForRectangle(Paint(), rectangle.getColor())
                val oldNodesList = rectangle.nodesList
                canvas.drawRect(rectangle.point1.x + oldNodesList[0].getWidthOfNode() / 2.0f,
                        rectangle.point3.y + oldNodesList[2].getWidthOfNode() / 2.0f,
                        rectangle.point3.x + oldNodesList[2].getWidthOfNode() / 2.0f,
                        rectangle.point1.y + oldNodesList[0].getWidthOfNode() / 2.0f, oldPaint)
            }
        }

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
        }
        invalidate()
        return true
    }
}