package com.akimchenko.antony.mediocr.cropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap


class DraggableNode(val context: Context, val resourceId: Int, val point: Point) {

    companion object {
        private var count = 0
    }

    private var id: Int = 0
    var bitmap: Bitmap? = null

    init {
        this.id = count++
        show()
    }

    fun show() {
        bitmap = ContextCompat.getDrawable(context, resourceId)!!.toBitmap()
    }

    fun hide() {
        bitmap = null
    }

    fun getWidthOfNode(): Int = bitmap?.width ?: 0
    fun getHeightOfNode(): Int = bitmap?.height ?: 0
    fun getId() = id
}