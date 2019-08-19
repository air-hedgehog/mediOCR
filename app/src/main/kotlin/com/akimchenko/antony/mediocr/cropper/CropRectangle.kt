package com.akimchenko.antony.mediocr.cropper

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.min

class CropRectangle(val context: Context, colorResId: Int) {

    val point1: Point = Point(0, 0)
    val point2: Point = Point(400, 0)
    val point3: Point = Point(400, 400)
    val point4: Point = Point(0, 400)
    private var color = ContextCompat.getColor(context, colorResId)

    fun getColor() = color

    private var isNodesEnabled: Boolean = true

    val nodesList = arrayListOf(
        DraggableNode(context, point1),
        DraggableNode(context, point2),
        DraggableNode(context, point3),
        DraggableNode(context, point4)
    )

    fun setNodesEnabled(isEnabled: Boolean) {
        this.isNodesEnabled = isEnabled
        nodesList.forEach { if (isEnabled) it.show() else it.hide() }
    }

    fun isNodesEnabled(): Boolean = isNodesEnabled

    fun getRectangle(): Rect {
        val nodeWidth = getNodeWidth()
        val left = point1.x + nodeWidth / 2
        val top = point3.y + nodeWidth / 2
        val right = point3.x + nodeWidth / 2
        val bottom = point1.y + nodeWidth / 2

        return Rect(
            min(left, right), min(top, bottom),
            max(left, right), max(top, bottom)
        )
    }

    private fun getNodeWidth() = nodesList[0].getWidthOfNode()

    fun getX() = min(min(point1.x, point2.x), min(point3.x, point4.x)) + getNodeWidth() / 2
    fun getY() = min(min(point1.y, point2.y), min(point3.y, point4.y)) + getNodeWidth() / 2
}