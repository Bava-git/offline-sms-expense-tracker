package com.example.smsexpense

import android.app.Application
import com.example.smsexpense.data.AppDbHelper
import com.example.smsexpense.util.AppLogger

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDbHelper.init(this)
        AppLogger.log(this, "SmsTracker", "App created")
    }
}