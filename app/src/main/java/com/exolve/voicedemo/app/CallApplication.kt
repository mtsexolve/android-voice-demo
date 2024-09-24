package com.exolve.voicedemo.app

import android.app.Application
import android.content.Intent
import android.util.Log
import com.exolve.voicedemo.core.telecom.TelecomManager
import com.exolve.voicedemo.core.uiCommons.ApplicationStateInspector
import com.exolve.voicedemo.MessageService
import com.exolve.voicedemo.app.activities.CallActivity
import com.exolve.voicedemo.core.telecom.TelecomContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val APPLICATION = "CallApplication"

class CallApplication : Application() {

    private var applicationStateInspector = object: ApplicationStateInspector() {
        override fun inForegroundState() {
            CoroutineScope(Dispatchers.IO).launch {
                TelecomManager.getInstance().setForegroundState()
            }
        }
        override fun inBackgroundState() {
            CoroutineScope(Dispatchers.IO).launch {
                TelecomManager.getInstance().setBackgroundState()
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        Log.d(APPLICATION, "onCreate")
        MessageService.enable(context = this)
        TelecomManager.initialize(context = this)
        registerActivityLifecycleCallbacks(applicationStateInspector)
        CoroutineScope(Dispatchers.IO).launch {
            TelecomManager.getInstance().telecomEvents.collect { telecomEvent ->
                if (telecomEvent is TelecomContract.CallEvent.OnNewCall) {
                    val isFirstCall = TelecomManager.getInstance().getCalls().size == 1
                    if (telecomEvent.call.isOutCall ||
                        isFirstCall && applicationStateInspector.isMainActivityActive()
                    ) {
                        Intent(this@CallApplication, CallActivity::class.java).also { intent ->
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(APPLICATION, "onTerminate")
    }
}