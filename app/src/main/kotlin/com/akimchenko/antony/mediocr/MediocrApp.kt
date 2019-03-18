package com.akimchenko.antony.mediocr

import androidx.multidex.MultiDexApplication
import com.akimchenko.antony.mediocr.utils.AppSettings

class MediocrApp: MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
    }
}