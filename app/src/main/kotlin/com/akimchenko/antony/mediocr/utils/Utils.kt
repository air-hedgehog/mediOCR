package com.akimchenko.antony.mediocr.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.StateListDrawable
import androidx.core.content.ContextCompat
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import java.io.*
import java.text.DateFormat
import java.util.*

object Utils {
    @JvmStatic
    val customLanguageTags: HashMap<String, String> = hashMapOf(
            Pair("aze_cyrl", "Azerbaijani - Cyrillic"),
            Pair("chi_sim", "Chinese - Simplified"),
            Pair("chi_sim_vert", "Chinese - Simplified Vertical"),
            Pair("chi_tra", "Chinese - Traditional"),
            Pair("chi_tra_vert", "Chinese - Traditional Vertical"),
            Pair("dan_frak", "Danish - Fraktur"),
            Pair("equ", "Math / equation detection"),
            Pair("frk", "Frankish"),
            Pair("jpn_vert", "Japanese - Vertical"),
            Pair("kat_old", "Georgian - Old"),
            Pair("kmr", "Kurdish"),
            Pair("kor_vert", "Korean - Vertical"),
            Pair("osd", "Orientation and script detection"),
            Pair("slk_frak", "Slovak - Fraktur"),
            Pair("spa_old", "Spanish Old"),
            Pair("srp_latn", "Serbian - Latin"),
            Pair("uzb_cyrl", "Uzbek - Cyrillic"))

    @JvmStatic
    fun getLocalizedLangName(item: String): String = Utils.customLanguageTags[item] ?: Locale(item).displayLanguage

    @JvmStatic
    fun makeSelector(context: Context, bitmap: Bitmap): StateListDrawable {
        val resources = context.resources
        return StateListDrawable().also {stateList ->
            stateList.setExitFadeDuration(60)
            val pressedState = BitmapDrawable(resources, bitmap)
            pressedState.setColorFilter(ContextCompat.getColor(context, R.color.selected_tint), PorterDuff.Mode.SRC_ATOP)
            stateList.addState(intArrayOf(android.R.attr.state_pressed), pressedState)
            stateList.addState(intArrayOf(), BitmapDrawable(resources, bitmap))
        }
    }

    @JvmStatic
    fun formatDate(date: Long): String = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(date)

    @JvmStatic
    fun writeBitmapToFile(bitmap: Bitmap, file: File) {
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
    public fun isLanguageDownloaded(activity: MainActivity, item: String): Boolean {
        return activity.getTesseractDataFolder().listFiles()?.find { it.name == "$item.traineddata" } != null
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