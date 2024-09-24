package com.exolve.voicedemo.core.telecom

import com.exolve.voicesdk.IAudioRouteListener
import android.util.Log
import com.exolve.voicesdk.AudioRouteData

private const val LOGTAG = "AudioRouteListener"

class AudioRouteListener(
    private val telecomManager: TelecomManager
    ): IAudioRouteListener {
    override fun routeChanged(routes: List<AudioRouteData>) {
        Log.d(LOGTAG, "routeChanged: active route: ${routes.firstOrNull { it.isActive }?.name}}")
        telecomManager.updateAudioRoutes()
    }

}
