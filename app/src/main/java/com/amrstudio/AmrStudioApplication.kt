package com.amrstudio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AmrStudioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any global configurations here
    }
}