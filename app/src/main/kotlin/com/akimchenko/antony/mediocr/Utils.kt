package com.akimchenko.antony.mediocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.*
import java.text.DateFormat
import java.util.*

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

    @JvmStatic
    fun formatDate(date: Long) = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(date)

    @JvmStatic
    fun writeExternalToCache(bitmap: Bitmap, file: File) {
        try {
            file.createNewFile()
            val fos = FileOutputStream(file)
            val bos = BufferedOutputStream(fos, 2024 * 8)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            bos.flush()
            bos.close()
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {

        }
    }

    @JvmStatic
    fun getInternalDirs(context: Context): Array<File?> {
        /*return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            context.getExternalFilesDirs(null)
        else
            arrayOf(context.getExternalFilesDir(null))*/
        return arrayOf(context.getExternalFilesDir(null))
    }
}