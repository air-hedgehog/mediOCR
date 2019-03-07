package com.akimchenko.antony.mediocr;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class CameraTextureView extends TextureView {

    private int ratioWidth = 0;
    private int ratioHeight = 0;

    public CameraTextureView(Context context) {
        super(context);
    }

    public CameraTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CameraTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0)
            throw new IllegalArgumentException("Size cannot be negative.");

        if (ratioWidth == width && ratioHeight == height) return;

        ratioWidth = width;
        ratioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height);
        } else {
            int measuredWidth;
            int measuredHeight;
            if (width < height * ratioWidth / ratioHeight) {
                measuredWidth = width;
                measuredHeight = width * ratioHeight / ratioWidth;
            } else {
                measuredWidth = height * ratioWidth / ratioHeight;
                measuredHeight = height;
            }
            setMeasuredDimension(measuredWidth, measuredHeight);
        }
    }
}
