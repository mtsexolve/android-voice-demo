package com.exolve.voicedemo.features.call

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.exolve.voicedemo.core.uiCommons.interfaces.*
import com.exolve.voicedemo.core.utils.OnDropData
import com.exolve.voicesdk.AudioRouteData
import com.exolve.voicesdk.CallState
import com.exolve.voicesdk.AudioRoute

@Immutable
class CallContract {
    @Immutable
    data class State(
        val currentCallId: String,
        val calls: List<CallItemState>,
        val callsHash: Int = 0,
        val isHoldPressed: Boolean = false,
        val isSpeakerPressed: Boolean = false,
        val isTransferPressed: Boolean = false,
        val isDtmfPressed: Boolean = false,
        val isAddNewCallPressed: Boolean = false,
        val hasConference: Boolean = false,
        override val dialerText: String = "",
        val audioRoutes: List<AudioRouteData>,
        val selectedAudioRoute: AudioRoute,
        val onDropData: OnDropData?
    ) : UiState, DialableUi {

        @Immutable
        data class CallItemState(
            val isCallOutgoing: Boolean,
            val number: String,
            val formattedNumber: String,
            val callsId: String,
            val status: CallState,
            val indexForUiTest: Int,
            val isInConference: Boolean = false,
            val isMuted: Boolean = false,
            var duration: UInt,
            var qualityRating: Float = 5.0f
        ) {
            fun isActive(): Boolean {
                return status == CallState.CONNECTED ||
                        status == CallState.LOST_CONNECTION ||
                        status == CallState.ON_HOLD
            }
        }
    }

    @Immutable
    sealed class Event : UiEvent, DialableEvent {
        @Immutable data class OnAcceptCallButtonClicked(val callsId: String) : Event()
        @Immutable data class OnResumeButtonClicked(val callsId: String) : Event()
        @Immutable data class OnTerminateCurrentButtonClicked(val callsId: String) : Event()
        @Immutable object OnTerminateAllButtonClicked : Event()
        @Immutable data class OnHoldButtonClicked(val callsId: String) : Event()
        @Immutable data class OnAudioRouteSelect(val route: AudioRoute) : Event()
        @Immutable object OnMuteButtonClicked : Event()
        @Immutable object OnSpeakerButtonClicked : Event()
        @Immutable data class OnTransferButtonClicked(val currentCallId: String) : Event()
        @Immutable data class OnTransferNumberSelected(val selectedCall: String) : Event()
        @Immutable object OnNewCallButtonClicked : Event()
        @Immutable object OnDtmfButtonClicked : Event()
        @Immutable object OnBackToControlScreenClicked : Event()
        @Immutable data class OnItemsSwapped(val ind1: Int, val ind2: Int) : Event()
        @Immutable data class OnItemDroppedOnItem(val firstIndex: Int?, val secondIndex: Int?) : Event()
        @Immutable data class OnRemoveCallFromConference(val callsId: String) : Event()
        @Immutable data class OnAddCallToConference(val callsId: String) : Event()
        @Immutable object OnReleaseDropData : Event()
    }

    @Stable
    sealed class Effect : UiEffect
}