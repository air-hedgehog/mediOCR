package com.akimchenko.antony.mediocr

import android.app.Application
import com.akimchenko.antony.mediocr.viewModels.LanguageViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MediocrApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val viewModelModule = module {
            single {
                AppRepository(this@MediocrApp)
            }
            viewModel { LanguageViewModel(get()) }
        }

        startKoin {
            androidLogger()
            androidContext(this@MediocrApp)
            modules(listOf(viewModelModule))
        }
    }
}