package com.exolve.voicedemo.app

import android.app.Application
import android.util.Log
import com.exolve.voicedemo.core.telecom.TelecomManager
import com.exolve.voicedemo.core.uiCommons.CallsActivityManager
import com.exolve.voicedemo.MessageService

private const val APPLICATION = "Application"

class CallApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d(APPLICATION, "onCreate")
        MessageService.enable(this)
        val telecomManager  = TelecomManager.getInstance(context = this)
        val activityManager = CallsActivityManager.getInstance(this, telecomManager)
        registerActivityLifecycleCallbacks(activityManager)
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(APPLICATION, "onTerminate")
    }
}