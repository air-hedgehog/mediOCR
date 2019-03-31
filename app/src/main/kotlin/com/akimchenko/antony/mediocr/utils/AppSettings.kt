package com.akimchenko.antony.mediocr.utils

import android.content.Context
import android.content.SharedPreferences
import com.akimchenko.antony.mediocr.MediocrApp

object AppSettings {

    private const val SHARED_PREFERENCES_NAME = "mediocr_shared_preferences"
    private const val TESSERACT_SELECTED_LANGUAGE = "tesseract_selected_language"
    private const val USE_APPLICATION_CAMERA = "use_application_camera"

    @JvmStatic
    private lateinit var sp: SharedPreferences

    @JvmStatic
    fun init(appContext: MediocrApp) {
        sp = appContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun getSelectedLanguage(): String = sp.getString(TESSERACT_SELECTED_LANGUAGE, "eng")!!

    @JvmStatic
    fun setSelectedLanguage(lang: String?) = sp.edit().putString(TESSERACT_SELECTED_LANGUAGE, lang).apply()

    @JvmStatic
    var useApplicationCamera: Boolean
        get() = sp.getBoolean(USE_APPLICATION_CAMERA, true)
        set(isUse) = sp.edit().putBoolean(USE_APPLICATION_CAMERA, isUse).apply()
}