package com.akimchenko.antony.mediocr.utils

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import android.view.View

class CameraTextureView : TextureView {

    private var ratioWidth = 0
    private var ratioHeight = 0

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {}

    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0)
            throw IllegalArgumentException("Size cannot be negative.")

        if (ratioWidth == width && ratioHeight == height) return

        ratioWidth = width
        ratioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = View.MeasureSpec.getSize(heightMeasureSpec)
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height)
        } else {
            val measuredWidth: Int
            val measuredHeight: Int
            if (width < height * ratioWidth / ratioHeight) {
                measuredWidth = width
                measuredHeight = width * ratioHeight / ratioWidth
            } else {
                measuredWidth = height * ratioWidth / ratioHeight
                measuredHeight = height
            }
            setMeasuredDimension(measuredWidth, measuredHeight)
        }
    }
}
