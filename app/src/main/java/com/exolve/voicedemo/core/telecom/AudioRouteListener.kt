package com.exolve.voicedemo.core.telecom

import com.exolve.voicesdk.IAudioRouteListener
import android.util.Log

private const val LOGTAG = "AudioRouteListener"

class AudioRouteListener(
    private val telecomManager: TelecomManager
    ): IAudioRouteListener {
    override fun routeChanged() {
        Log.d(LOGTAG, "route changed")
        telecomManager.updateAudioRoutes();
    }

}