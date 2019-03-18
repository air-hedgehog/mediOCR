package com.akimchenko.antony.mediocr.utils

import androidx.annotation.IntDef
import java.util.*

object NotificationCenter {

    const val LANG_DOWNLOAD_STATUS_CHANGED = 0
    const val LANG_DELETED = 1

    @IntDef(LANG_DOWNLOAD_STATUS_CHANGED,
            LANG_DELETED)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Id

    interface Observer {
        fun onNotification(@Id id: Int, `object`: Any?)
    }

    @JvmStatic
    private var observers: MutableSet<Observer> = HashSet()

    @JvmStatic
    private var newObservers: MutableSet<Observer>? = null

    @JvmStatic
    private var enumeratingObservers: Boolean = false

    @JvmStatic
    fun addObserver(observer: Observer) {
        if (enumeratingObservers) {
            if (newObservers == null)
                newObservers = HashSet(observers)
            newObservers!!.remove(observer)
        } else {
            observers.add(observer)
        }
    }

    @JvmStatic
    fun removeObserver(observer: Observer) {
        if (enumeratingObservers) {
            if (newObservers == null)
                newObservers = HashSet(observers)
            newObservers!!.remove(observer)
        } else {
            observers.remove(observer)
        }
    }

    @JvmStatic
    fun notify(@Id id: Int, `object`: Any?) {
        enumeratingObservers = true
        for (observer in observers)
            observer.onNotification(id, `object`)
        enumeratingObservers = false
        if (newObservers != null)
            observers = newObservers as MutableSet<Observer>
    }

}