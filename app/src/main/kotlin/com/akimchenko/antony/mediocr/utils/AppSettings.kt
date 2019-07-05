package com.akimchenko.antony.mediocr.utils

import android.content.Context
import android.content.SharedPreferences
import com.akimchenko.antony.mediocr.MediocrApp

object AppSettings {

    private const val SHARED_PREFERENCES_NAME = "mediocr_shared_preferences"
    private const val TESSERACT_SELECTED_LANGUAGEES = "tesseract_selected_language"
    private const val USE_APPLICATION_CAMERA = "use_application_camera"
    private const val DEFAULT_RESULT_FORMATTING = "default_result_formatting"
    const val SAVED_FILES_SORT_TYPE = "saved_files_sort_type"

    @JvmStatic
    private lateinit var sp: SharedPreferences

    @JvmStatic
    fun init(appContext: MediocrApp) {
        sp = appContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = sp.registerOnSharedPreferenceChangeListener(listener)

    @JvmStatic
    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = sp.unregisterOnSharedPreferenceChangeListener(listener)

    @JvmStatic
    fun getSelectedLanguageList(): ArrayList<String> {
        val rv = sp.getString(TESSERACT_SELECTED_LANGUAGEES, null)?.let {
            ArrayList(it.split(","))
        }
        // "split" method returns one empty string as a legit first element if none of the langs weren't found
        rv?.forEach { if (it.isEmpty()) rv.remove(it) }
        return rv ?: ArrayList()
    }

    @JvmStatic
    fun replaceSelectedLanguage(lang: String, index: Int) {
        val list = getSelectedLanguageList()
        try {
            list[index] = lang
        } catch (e: IndexOutOfBoundsException) {
            list.add(lang)
        }
        sp.edit().putString(TESSERACT_SELECTED_LANGUAGEES, list.joinToString(",")).apply()
    }

    @JvmStatic
    fun addSelectedLanguage(lang: String) {
        if (lang.length < 3) return

        val list = getSelectedLanguageList()
        val resultList: List<String>
        if (list.size < 3) {
            list.add(lang)
            resultList = list
        } else {
            resultList = list.subList(0, 3)
            resultList.add(2, lang)
        }

        sp.edit().putString(TESSERACT_SELECTED_LANGUAGEES, resultList.joinToString(",")).apply()
    }

    @JvmStatic
    fun removeSelectedLanguage(lang: String) {
        val list = getSelectedLanguageList()

        if (list.remove(lang))
            sp.edit().putString(TESSERACT_SELECTED_LANGUAGEES, list.joinToString(",")).apply()
    }

    @JvmStatic
    var useApplicationCamera: Boolean
        get() = sp.getBoolean(USE_APPLICATION_CAMERA, true)
        set(isUse) = sp.edit().putBoolean(USE_APPLICATION_CAMERA, isUse).apply()

    @JvmStatic
    var defaultResultFormatting: Boolean
        get() = sp.getBoolean(DEFAULT_RESULT_FORMATTING, true)
        set(isDefaultFormatting) = sp.edit().putBoolean(DEFAULT_RESULT_FORMATTING, isDefaultFormatting).apply()

    @JvmStatic
    var savedFilesSortedAlphabetically: Boolean
        get() = sp.getBoolean(SAVED_FILES_SORT_TYPE, true)
        set(isSortedAlphabetically) = sp.edit().putBoolean(SAVED_FILES_SORT_TYPE, isSortedAlphabetically).apply()
}