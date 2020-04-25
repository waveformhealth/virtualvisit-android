package com.waveformhealth

import android.app.Application
import com.waveformhealth.di.AppComponent
import com.waveformhealth.di.DaggerAppComponent

class WaveformHealthApp : Application() {

    private lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.create()
    }

    fun appComp() = appComponent
}