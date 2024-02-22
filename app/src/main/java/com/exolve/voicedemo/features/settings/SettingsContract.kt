package com.exolve.voicedemo.features.settings

import androidx.compose.runtime.Immutable
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEffect
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import com.exolve.voicedemo.core.uiCommons.interfaces.UiState
import com.exolve.voicesdk.RegistrationState

class SettingsContract {
    // UI view state
    @Immutable
    data class State(
        val number: String,
        val password: String,
        val token: String,
        val versionDescription: String,
        val registrationState: RegistrationState,
        val voipBackgroundRunning: Boolean
    ) : UiState
    // Events that user performs
    @Immutable
    sealed class Event : UiEvent {
        @Immutable object OnActivateClicked : Event()
        @Immutable object OnBackToCallActivityClicked : Event()
        @Immutable data class UserTexFieldChanged(override val textState: String) : Event(), FillableLoginField
        @Immutable data class PasswordTexFieldChanged(override val textState: String) : Event(), FillableLoginField
        @Immutable object OnSendLogsClicked : Event()
        @Immutable object OnCopyButtonClicked : Event()
        @Immutable data class OnBackgroundRunningChanged(val enabled: Boolean) : Event()
    }
    //Side effects
    @Immutable
    sealed class Effect : UiEffect

    interface FillableLoginField {
        val textState: String
    }

}
