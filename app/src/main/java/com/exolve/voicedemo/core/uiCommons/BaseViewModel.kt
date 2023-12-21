package com.exolve.voicedemo.core.uiCommons

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exolve.voicedemo.core.telecom.TelecomEvent
import com.exolve.voicedemo.core.telecom.TelecomManager
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEffect
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import com.exolve.voicedemo.core.uiCommons.interfaces.UiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class BaseViewModel<Event : UiEvent, State : UiState, Effect : UiEffect>(application: Application) : AndroidViewModel(
    application
) {

    protected val telecomManager: TelecomManager = TelecomManager.getInstance(application)
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

    protected fun setEffect(builder: () -> Effect) {
        val effectValue = builder()
        viewModelScope.launch { _effect.send(effectValue) }
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

}