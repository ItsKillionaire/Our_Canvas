package com.ourcanvas

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OurCanvasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("OurCanvasApplication", "onCreate: Application created")
    }
}