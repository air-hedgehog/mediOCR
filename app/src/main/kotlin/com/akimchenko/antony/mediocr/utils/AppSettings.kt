package com.akimchenko.antony.mediocr.utils

import android.content.Context
import android.content.SharedPreferences
import com.akimchenko.antony.mediocr.MediocrApp

object AppSettings {

    private const val SHARED_PREFERENCES_NAME = "mediocr_shared_preferences"
    private const val TESSERACT_SELECTED_LANGUAGE = "tesseract_selected_language"
    private const val USE_APPLICATION_CAMERA = "use_application_camera"
    private const val DEFAULT_RESULT_FORMATTING = "default_result_formatting"
    private const val INITIAL_DOWNLOADS_LIST = "initial_downloads_list"
    const val SAVED_FILES_SORT_TYPE = "saved_files_sort_type"

    private const val traineddata = ".traineddata"

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
    var selectedLanguage: String
        get() {
            val lang = sp.getString(TESSERACT_SELECTED_LANGUAGE, "eng") ?: "eng"
            return if (lang.endsWith(traineddata)) lang else "$lang$traineddata"
        }
        set(lang) {
            sp.edit().putString(TESSERACT_SELECTED_LANGUAGE, lang).apply()
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

    @JvmStatic
    var initialDownloadsList: List<String>?
        get() {
            sp.getString(INITIAL_DOWNLOADS_LIST, null)?.let {
                return it.split(",")
            }
            return null
        }
        set(list) {
            list?.let {
                sp.edit().putString(INITIAL_DOWNLOADS_LIST, it.joinToString(",")).apply()
            }
        }
}