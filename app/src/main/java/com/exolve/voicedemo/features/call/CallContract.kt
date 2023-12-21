package com.exolve.voicedemo.features.call

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.exolve.voicedemo.core.uiCommons.interfaces.*
import com.exolve.voicesdk.CallState

@Immutable
class CallContract {

    @Immutable
    data class State(
        val currentCallNumber: String,
        val currentCallId: String,
        val calls: List<CallItemState>,
        val isHoldPressed: Boolean = false,
        val isSpeakerPressed: Boolean = false,
        val isTransferPressed: Boolean = false,
        val isDtmfPressed: Boolean = false,
        val isAddNewCallPressed: Boolean = false,
        val hasConference: Boolean = false,
        override val dialerText: String = "",
    ) : UiState, DialableUi {

        @Immutable
        data class CallItemState(
            val isCallOutgoing: Boolean,
            val number: String,
            val callsId: String,
            val status: CallState,
            val indexForUiTest: Int,
            val isInConference: Boolean = false,
            val isMuted: Boolean = false
        )
    }

    @Immutable
    sealed class Event : UiEvent, DialableEvent {
        @Immutable data class OnAcceptCallButtonClicked(val callsId: String) : Event()
        @Immutable data class OnResumeButtonClicked(val callsId: String) : Event()
        @Immutable data class OnTerminateCurrentButtonClicked(val callsId: String) : Event()
        @Immutable object OnTerminateAllButtonClicked : Event()
        @Immutable data class OnHoldButtonClicked(val callsId: String) : Event()
        @Immutable data class OnSpeakerButtonClicked(val speakerPressed: Boolean) : Event()
        @Immutable object OnMuteButtonClicked : Event()
        @Immutable data class OnTransferButtonClicked(val currentCallId: String) : Event()
        @Immutable data class OnTransferNumberSelected(val selectedCall: String) : Event()
        @Immutable object OnNewCallButtonClicked : Event()
        @Immutable object OnDtmfButtonClicked : Event()
        @Immutable object OnBackToControlScreenClicked : Event()
        @Immutable data class OnItemsSwapped(val ind1: Int, val ind2: Int) : Event()
        @Immutable data class OnItemDroppedOnItem(val firstIndex: Int?, val secondIndex: Int?) : Event()
        @Immutable data class OnRemoveCallFromConference(val callsId: String) : Event()
        @Immutable data class OnAddCallToConference(val callsId: String) : Event()
    }

    @Stable
    sealed class Effect : UiEffect
}