package com.akimchenko.antony.mediocr.utils

import com.akimchenko.antony.mediocr.components.AppSettingsComponent
import com.akimchenko.antony.mediocr.components.LangDownloadComponent
import org.koin.dsl.module.module

val sharedPreferencesModule = module {
    single { AppSettingsComponent(get()) }
}

val onCompleteDownloadModule = module {
    single { LangDownloadComponent(get()) }
}