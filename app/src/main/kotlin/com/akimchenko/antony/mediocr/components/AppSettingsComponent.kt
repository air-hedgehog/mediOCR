package com.akimchenko.antony.mediocr.components

import android.content.Context
import android.content.SharedPreferences

class AppSettingsComponent(context: Context) {

    private val SHARED_PREFERENCES_NAME = "mediocr_shared_preferences"
    private val TESSERACT_SELECTED_LANGUAGE = "tesseract_selected_language"

    private val sp: SharedPreferences

    init {
        sp = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    fun getSelectedLanguage(): String = sp.getString(TESSERACT_SELECTED_LANGUAGE, "eng")!!

    fun setSelectedLanguage(lang: String?) = sp.edit().putString(TESSERACT_SELECTED_LANGUAGE, lang).apply()

}