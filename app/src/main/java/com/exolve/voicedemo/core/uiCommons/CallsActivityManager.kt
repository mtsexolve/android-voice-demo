package com.exolve.voicedemo.core.uiCommons

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.exolve.voicedemo.app.activities.CallActivity
import com.exolve.voicedemo.core.telecom.TelecomManager
import com.exolve.voicedemo.core.telecom.TelecomContract.CallEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val CALLS_ACTIVITY_MANAGER = "CallsActivityManager"

class CallsActivityManager(
    private val context: Application,
    private val telecomManager: TelecomManager,
) : Application.ActivityLifecycleCallbacks{
    private val _activitiesState: MutableStateFlow<ActivitiesInstantiatorContract.State> = MutableStateFlow(initState())
    val activitiesState = _activitiesState.asStateFlow()
    private val _activitiesEvent: MutableSharedFlow<ActivitiesInstantiatorContract.Event> = MutableSharedFlow()
    val activitiesEvent = _activitiesEvent.asSharedFlow()

    private fun initState() = ActivitiesInstantiatorContract.State()

    init {
        Log.d(CALLS_ACTIVITY_MANAGER, "init: ${telecomManager.getCalls().size} ongoing call(s)")
        CoroutineScope(Dispatchers.IO).launch {
            telecomManager.telecomEvents.collect { telecomEvent ->
                (telecomEvent as? CallEvent)?.let { event -> handleTelecomEvent(event) }
            }
        }
    }

    private fun handleTelecomEvent(event: CallEvent) {
        when (event) {
            is CallEvent.OnNewCall -> handleNewCall(event)
            is CallEvent.OnCallTerminated -> handleTerminatedCall()
            else -> {}
        }
    }

    private fun handleTerminatedCall() {
        Log.d(CALLS_ACTIVITY_MANAGER, "handleTerminatedCall: ${telecomManager.getCalls().size} ongoing call(s)")
        if (telecomManager.getCalls().isEmpty() != false) {
            Log.d(CALLS_ACTIVITY_MANAGER, "handleTerminatedCall: no ongoing calls")
            CoroutineScope(Dispatchers.IO).launch {
                _activitiesEvent.emit(ActivitiesInstantiatorContract.Event.CallActivityMustFinished)
            }
        }
    }

    private fun handleNewCall(event: CallEvent.OnNewCall) {
        val isFirstCall = telecomManager.getCalls().size == 1
        Log.d(CALLS_ACTIVITY_MANAGER, "handleNewCall: " +
                "calls.size = ${telecomManager.getCalls().size}, application in fg = ${activitiesState.value.hasActivityInForeground}")
        if (event.call.isOutCall || (isFirstCall && activitiesState.value.hasActivityInForeground)) {
            Intent(context, CallActivity::class.java).also { intent ->
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        }
    }

    private fun setState(reduce: ActivitiesInstantiatorContract.State.()-> ActivitiesInstantiatorContract.State) {
        val newState = _activitiesState.value.reduce()
        _activitiesState.value = newState
    }

    fun activityEvents(): SharedFlow<ActivitiesInstantiatorContract.Event> {
        return activitiesEvent
    }

    private var visibleActivitiesCounter: Int = 0
    override fun onActivityPaused(p0: Activity) {
        Log.d(CALLS_ACTIVITY_MANAGER, "onActivityPaused: ${p0.localClassName}, visibleActivityCounter = $visibleActivitiesCounter")
    }

    override fun onActivityStarted(p0: Activity) {
        visibleActivitiesCounter++
        CoroutineScope(Dispatchers.IO).launch {
            _activitiesEvent.emit(ActivitiesInstantiatorContract.Event.SomeActivityInForeground)
            telecomManager.setForegroundState()
            setState { _activitiesState.value.copy(hasActivityInForeground = true) }
        }
        Log.d(CALLS_ACTIVITY_MANAGER, "onActivityStarted: ${p0.localClassName}, visibleActivityCounter = $visibleActivitiesCounter")
    }

    override fun onActivityDestroyed(p0: Activity) {
        Log.d(CALLS_ACTIVITY_MANAGER, "onActivityDestroyed: ${p0.localClassName}, visibleActivityCounter = $visibleActivitiesCounter")
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        Log.d(CALLS_ACTIVITY_MANAGER, "onActivitySaveInstanceState: ${p0.localClassName}, visibleActivityCounter = $visibleActivitiesCounter")
    }

    override fun onActivityStopped(p0: Activity) {
        visibleActivitiesCounter--
        if (visibleActivitiesCounter == 0) CoroutineScope(Dispatchers.IO).launch {
            Log.d(CALLS_ACTIVITY_MANAGER, "app in bg, all activities in bg")
            _activitiesEvent.emit(ActivitiesInstantiatorContract.Event.AllActivitiesInBackground)
            telecomManager.setBackgroundState()
            setState { _activitiesState.value.copy(hasActivityInForeground = false) }
        }
        Log.d(CALLS_ACTIVITY_MANAGER, "onActivityStopped: ${p0.localClassName}, visibleActivityCounter = $visibleActivitiesCounter")
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        Log.d(CALLS_ACTIVITY_MANAGER, "onActivityCreated: ${p0.localClassName}, visibleActivityCounter = $visibleActivitiesCounter")
    }

    override fun onActivityResumed(p0: Activity) {
        Log.d(CALLS_ACTIVITY_MANAGER, "onActivityResumed: ${p0.localClassName}, visibleActivityCounter = $visibleActivitiesCounter")
    }

    companion object {
        private var INSTANCE: CallsActivityManager? = null
        fun getInstance(context: Application, telecomManager: TelecomManager): CallsActivityManager {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = CallsActivityManager(context = context, telecomManager = telecomManager)
                    Log.d(CALLS_ACTIVITY_MANAGER, "getInstance: $INSTANCE")
                }
            }
            return INSTANCE!!
        }
    }

}