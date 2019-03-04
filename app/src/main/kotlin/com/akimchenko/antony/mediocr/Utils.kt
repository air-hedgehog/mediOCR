package com.akimchenko.antony.mediocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.StateListDrawable
import androidx.core.content.ContextCompat

object Utils {

    @JvmStatic
    fun makeSelector(context: Context, bitmap: Bitmap): StateListDrawable {
        val resources = context.resources
        val stateList = StateListDrawable()
        stateList.setExitFadeDuration(60)
        val pressedState = BitmapDrawable(resources, bitmap)
        pressedState.setColorFilter(ContextCompat.getColor(context, R.color.selected_tint), PorterDuff.Mode.SRC_ATOP)
        stateList.addState(intArrayOf(android.R.attr.state_pressed), pressedState)
        stateList.addState(intArrayOf(), BitmapDrawable(resources, bitmap))
        return stateList
    }

}