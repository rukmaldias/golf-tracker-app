package com.rapsodo.golf

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

import com.rapsodo.golf.domain.logger.AppLogger
import com.rapsodo.golf.BuildConfig

@HiltAndroidApp
class GolfApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }
        Napier.i(tag = AppLogger.TAG) { "App started" }
    }
}