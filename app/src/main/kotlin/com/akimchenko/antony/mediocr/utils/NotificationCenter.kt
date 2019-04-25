package com.akimchenko.antony.mediocr.utils

import androidx.annotation.IntDef

object NotificationCenter {

    const val LANG_DOWNLOADED = 0
    const val LANG_DELETED = 1
    const val RECOGNITION_PROCESS_CANCELLED = 2
    const val SAVE_AS_TXT_ID = 3
    const val SAVE_AS_PDF_ID = 4

    @IntDef(
        LANG_DOWNLOADED,
        LANG_DELETED,
        RECOGNITION_PROCESS_CANCELLED,
        SAVE_AS_PDF_ID,
        SAVE_AS_TXT_ID
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class Id

    interface Observer {
        fun onNotification(@Id id: Int, `object`: Any?)
    }

    @JvmStatic
    private var observers: HashSet<Observer> = HashSet()

    @JvmStatic
    fun addObserver(observer: Observer) = observers.add(observer)

    @JvmStatic
    fun removeObserver(observer: Observer) = observers.remove(observer)

    @JvmStatic
    fun notify(@Id id: Int, `object`: Any?) = observers.forEach { it.onNotification(id, `object`) }
}