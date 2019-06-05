package com.akimchenko.antony.mediocr.cropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point


class DraggableNode(context: Context, resourceId: Int, val point: Point) {

    companion object {
        private var count = 0
    }

    private var id: Int = 0
    var bitmap: Bitmap

    init {
        this.id = count++
        bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
    }

    fun getWidthOfNode(): Int = bitmap.width
    fun getHeightOfNode(): Int = bitmap.height
    fun getId() = id
}