package com.akimchenko.antony.mediocr.cropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap


class DraggableNode(context: Context, resourceId: Int, val point: Point) {

    companion object {
        private var count = 0
    }

    private var id: Int = 0
    var bitmap: Bitmap

    init {
        this.id = count++
        bitmap = ContextCompat.getDrawable(context, resourceId)!!.toBitmap()
    }

    fun getWidthOfNode(): Int = bitmap.width
    fun getHeightOfNode(): Int = bitmap.height
    fun getId() = id
}