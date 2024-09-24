package com.exolve.voicedemo.core.uiCommons

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.exolve.voicedemo.app.activities.MainActivity

private const val APPLICATION_STATE_INSPECTOR = "ApplicationStateInspector"

abstract class ApplicationStateInspector : Application.ActivityLifecycleCallbacks {
    private var mainActivityActive = false
    private var startedActivities: MutableSet<String> = mutableSetOf()

    abstract fun inForegroundState()
    abstract fun inBackgroundState()

    fun isMainActivityActive(): Boolean {
        return mainActivityActive
    }

    override fun onActivityPaused(p0: Activity) {
        if (p0 is MainActivity) {
            mainActivityActive = false
        }
        Log.d(APPLICATION_STATE_INSPECTOR, "onActivityPaused: ${p0.localClassName}")
    }

    override fun onActivityStarted(p0: Activity) {
        val prevSize = startedActivities.size
        startedActivities.add(p0.localClassName)
        if (prevSize != startedActivities.size && startedActivities.size == 1) {
            inForegroundState()
        }
        Log.d(APPLICATION_STATE_INSPECTOR, "onActivityStarted: ${p0.localClassName}")
    }

    override fun onActivityDestroyed(p0: Activity) {
        Log.d(APPLICATION_STATE_INSPECTOR, "onActivityDestroyed: ${p0.localClassName}")
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        Log.d(APPLICATION_STATE_INSPECTOR, "onActivitySaveInstanceState: ${p0.localClassName}")
    }

    override fun onActivityStopped(p0: Activity) {
        val prevSize = startedActivities.size
        startedActivities.remove(p0.localClassName)
        if (prevSize != startedActivities.size && startedActivities.size == 0) {
            Log.d(APPLICATION_STATE_INSPECTOR, "app in bg, all activities in bg")
            inBackgroundState()
        }
        Log.d(APPLICATION_STATE_INSPECTOR, "onActivityStopped: ${p0.localClassName}")
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        Log.d(APPLICATION_STATE_INSPECTOR, "onActivityCreated: ${p0.localClassName}")
    }

    override fun onActivityResumed(p0: Activity) {
        if (p0 is MainActivity) {
            mainActivityActive = true
        }
        Log.d(APPLICATION_STATE_INSPECTOR, "onActivityResumed: ${p0.localClassName}")
    }
}