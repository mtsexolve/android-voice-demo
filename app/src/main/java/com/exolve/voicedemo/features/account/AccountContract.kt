package com.exolve.voicedemo.features.account

import androidx.compose.runtime.Immutable
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEffect
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import com.exolve.voicedemo.core.uiCommons.interfaces.UiState
import com.exolve.voicesdk.RegistrationState

class AccountContract {

    // UI view state
    @Immutable
    data class State(
        val number: String,
        val password: String,
        val token: String,
        val registrationState: RegistrationState,
    ) : UiState

    // Events that user performs
    @Immutable
    sealed class Event : UiEvent {
        @Immutable data object OnActivateClicked : Event()
        @Immutable data object OnBackToCallActivityClicked : Event()
        @Immutable data class UserTexFieldChanged(override val textState: String) : Event(),
            FillableLoginField
        @Immutable data class PasswordTexFieldChanged(override val textState: String) : Event(),
            FillableLoginField
        @Immutable data object OnCopyButtonClicked : Event()
    }

    //Side effects
    @Immutable
    sealed class Effect : UiEffect

    interface FillableLoginField {
        val textState: String
    }

}
