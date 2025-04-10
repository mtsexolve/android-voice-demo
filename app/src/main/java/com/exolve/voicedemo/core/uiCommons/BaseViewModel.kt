package com.exolve.voicedemo.core.uiCommons

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exolve.voicedemo.core.telecom.TelecomEvent
import com.exolve.voicedemo.core.telecom.TelecomManager
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEffect
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import com.exolve.voicedemo.core.uiCommons.interfaces.UiState
import com.exolve.voicedemo.core.utils.PermissionRequester
import com.exolve.voicedemo.core.utils.CancelPermissionRequestCallback
import com.exolve.voicedemo.core.utils.PermissionState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class BaseViewModel<Event : UiEvent, State : UiState, Effect : UiEffect>(application: Application) : AndroidViewModel(
    application
) {
    enum class PermissionsRequestedResult { GRANTED_ALL, GRANTED_ANY, DENIED_ALL }

    protected val telecomManager: TelecomManager = TelecomManager.getInstance()
    private val initState: State by lazy { initializeState() }

    // StateFLow - state holder observable flow that emits current and new state to its collectors.
    // Backing property to avoid state updates from other classes
    private val _uiState: MutableStateFlow<State> = MutableStateFlow(initState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    // Shared Flow - StateFlow only emits last known value , whereas sharedflow can configure how
    // many previous values to be emitted
    private val _event: MutableSharedFlow<Event> = MutableSharedFlow()
    val event: SharedFlow<Event> = _event.asSharedFlow()

    // Hot flow. Effect that we show only once
    private val _effect: Channel<Effect> = Channel()
    val effect = _effect.receiveAsFlow()

    private val currentState: State
        get() = uiState.value

    init { subscribeOnEvents() }

    abstract fun initializeState() : State

    fun setEvent(event : Event) {
        val newEvent = event
        viewModelScope.launch { _event.emit(newEvent) }
    }

    fun setState(reduce: State.() -> State) {
        val newState = currentState.reduce()
        _uiState.value = newState
        Log.d("BaseViewModel", "setState: $newState")

    }

    private fun subscribeOnEvents() {
        viewModelScope.launch {
            event.collect {
                handleUiEvent(it)
            }
        }
    }

    abstract fun handleUiEvent(event : Event)

    abstract suspend fun handleTelecomEvent(event: TelecomEvent)


    protected fun requestPermissions(vararg permissions: String, onRequestedResult: (state: PermissionsRequestedResult) -> Unit = {}): CancelPermissionRequestCallback {
        if(permissions.all { ContextCompat.checkSelfPermission(getApplication(), it) == PackageManager.PERMISSION_GRANTED } ) {
            onRequestedResult(PermissionsRequestedResult.GRANTED_ALL)
            return {}
        } else if(permissions.any { ContextCompat.checkSelfPermission(getApplication(), it) == PackageManager.PERMISSION_GRANTED } ) {
            onRequestedResult(PermissionsRequestedResult.GRANTED_ANY)
            return {}
        } else {
            return PermissionRequester.requestPermissions(getApplication(), *permissions) { permissionResults ->
                if (permissionResults.all{it.state == PermissionState.GRANTED}) {
                    onRequestedResult(PermissionsRequestedResult.GRANTED_ALL)
                } else if (permissionResults.any{it.state == PermissionState.GRANTED}) {
                    onRequestedResult(PermissionsRequestedResult.GRANTED_ANY)
                }
                else {
                    onRequestedResult(PermissionsRequestedResult.DENIED_ALL)
                }
            }
        }
    }

}