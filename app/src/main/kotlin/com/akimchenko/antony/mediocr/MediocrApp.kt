package com.akimchenko.antony.mediocr

import android.app.Application
import com.akimchenko.antony.mediocr.utils.AppSettings

class MediocrApp: Application() {

    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
    }
}