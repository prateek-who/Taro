package com.prateek.taro

import android.app.Application
import com.prateek.taro.util.AppPreferences

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPreferences.init(applicationContext)
    }
}