package com.akimchenko.antony.mediocr.cropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.akimchenko.antony.mediocr.R


class DraggableNode(val context: Context, val point: Point) {

    companion object {
        private var count = 0
        fun startNewRectangle() = run { count = 0 }
    }

    private var id: Int = 0
    var bitmap: Bitmap? = null

    init {
        this.id = count++
        show()
    }

    fun show() {
        bitmap = ContextCompat.getDrawable(context, R.drawable.node_cross)!!.toBitmap()
    }

    fun hide() {
        bitmap = ContextCompat.getDrawable(context, R.drawable.empty_placeholder)!!.toBitmap()
    }

    fun getWidthOfNode(): Int = bitmap?.width ?: 0
    fun getHeightOfNode(): Int = bitmap?.height ?: 0
    fun getId() = id
}