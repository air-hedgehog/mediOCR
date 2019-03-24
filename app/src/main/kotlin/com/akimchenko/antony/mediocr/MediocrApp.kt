package com.akimchenko.antony.mediocr

import androidx.multidex.MultiDexApplication
import com.akimchenko.antony.mediocr.utils.onCompleteDownloadModule
import com.akimchenko.antony.mediocr.utils.sharedPreferencesModule
import org.koin.android.ext.android.startKoin

class MediocrApp: MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        startKoin(this, listOf(sharedPreferencesModule, onCompleteDownloadModule))
    }
}