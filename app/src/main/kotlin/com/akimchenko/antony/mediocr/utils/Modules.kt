package com.akimchenko.antony.mediocr.utils

import org.koin.dsl.module.module

val sharedPreferencesModule = module {
    single { AppSettingsComponent(get()) }
}