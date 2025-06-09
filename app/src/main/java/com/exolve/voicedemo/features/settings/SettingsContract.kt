package com.exolve.voicedemo.features.settings

import androidx.compose.runtime.Immutable
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEffect
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import com.exolve.voicedemo.core.uiCommons.interfaces.UiState
import com.exolve.voicesdk.LogLevel
import com.exolve.voicesdk.TelecomIntegrationMode

class SettingsContract {

    // UI view state
    @Immutable
    data class State(
        val versionDescription: String,
        val voipBackgroundRunning: Boolean,
        val detectCallLocation: Boolean,
        val telecomManagerMode: TelecomIntegrationMode,
        val callContext: String,
        val sipTraces: Boolean, // need restart
        val logLevel: LogLevel, // need restart
        val useEncryption: Boolean, // need restart
        val environment: String, // need restart

        val needRestart: Boolean
    ) : UiState

    // Events that user performs
    @Immutable
    sealed class Event : UiEvent {
        @Immutable data object OnBackToCallActivityClicked : Event()
        @Immutable data object OnSendLogsClicked : Event()
        @Immutable data class OnBackgroundRunningChanged(val enabled: Boolean) : Event()
        @Immutable data class OnCallLocationDetectChanged(val enabled: Boolean) : Event()
        @Immutable data class OnTelecomManagerModeChanged(val mode: TelecomIntegrationMode) : Event()
        @Immutable data class OnSipTracesChanged(val enabled: Boolean) : Event()
        @Immutable data class OnLogLevelChanged(val level: LogLevel) : Event()
        @Immutable data class OnUseEncryptionChanged(val enabled: Boolean) : Event()
        @Immutable data class OnEnvironmentChanged(val environment: String) : Event()
        @Immutable data class OnCallContextChanged(val callContext: String) : Event()
        @Immutable data object Restart : Event()
    }

    //Side effects
    @Immutable
    sealed class Effect : UiEffect

}
