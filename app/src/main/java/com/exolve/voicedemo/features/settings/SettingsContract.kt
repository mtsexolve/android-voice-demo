package com.exolve.voicedemo.features.settings

import androidx.compose.runtime.Immutable
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEffect
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import com.exolve.voicedemo.core.uiCommons.interfaces.UiState
import com.exolve.voicesdk.TelecomIntegrationMode

class SettingsContract {

    // UI view state
    @Immutable
    data class State(
        val versionDescription: String,
        val voipBackgroundRunning: Boolean,
        val detectCallLocation: Boolean,
        val telecomManagerMode: TelecomIntegrationMode
    ) : UiState

    // Events that user performs
    @Immutable
    sealed class Event : UiEvent {
        @Immutable data object OnBackToCallActivityClicked : Event()
        @Immutable data object OnSendLogsClicked : Event()
        @Immutable data class OnBackgroundRunningChanged(val enabled: Boolean) : Event()
        @Immutable data class OnCallLocationDetectChanged(val enabled: Boolean) : Event()
        @Immutable data class OnTelecomManagerModeChanged(val mode: TelecomIntegrationMode) : Event()
    }

    //Side effects
    @Immutable
    sealed class Effect : UiEffect

}
