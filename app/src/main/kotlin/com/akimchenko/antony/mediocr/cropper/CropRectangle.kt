package com.akimchenko.antony.mediocr.cropper

import android.content.Context
import android.graphics.Point
import androidx.core.content.ContextCompat

class CropRectangle(val context: Context, colorResId: Int) {

    val point1: Point = Point(50, 20)
    val point2: Point = Point(150, 20)
    val point3: Point = Point(150, 120)
    val point4: Point = Point(50, 120)
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
}