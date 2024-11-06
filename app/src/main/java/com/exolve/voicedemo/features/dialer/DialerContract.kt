package com.exolve.voicedemo.features.dialer

import com.exolve.voicedemo.core.uiCommons.interfaces.DialableUi
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEffect
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import com.exolve.voicedemo.core.uiCommons.interfaces.UiState
import javax.annotation.concurrent.Immutable

@Immutable
class DialerContract {

    @Immutable
    data class State(
        override val dialerText: String,
        val hasCurrentCall: Boolean = false,
    ) : UiState, DialableUi

    @Immutable
    sealed class Event : UiEvent {
        @Immutable data object OnCallButtonClicked : Event()
        @Immutable data object OnCallButtonLongPressed : Event()
        @Immutable data class OnDigitButtonClicked(val index: String) : Event()
        @Immutable data class OnRemoveButtonClicked(val longClicked: Boolean) : Event()
        @Immutable data object OnBackToCallActivityClicked : Event()
        @Immutable data object OnContactsButtonClicked : Event()
    }

    @Immutable
    sealed class Effect : UiEffect
}