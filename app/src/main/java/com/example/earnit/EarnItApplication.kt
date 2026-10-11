package com.example.earnit

import android.app.Application
import com.example.earnit.core.di.AppContainer

class EarnItApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}