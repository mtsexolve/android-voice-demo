package com.exolve.voicedemo.core.telecom

import androidx.compose.runtime.Immutable
import com.exolve.voicesdk.*

class TelecomContract {
    data class State(
        val calls: MutableList<Call> = mutableListOf(),
        val currentCall: Call? = null,
        val token: String? = null,
        val registrationState: RegistrationState = RegistrationState.NOT_REGISTERED,
    )

    open class RegistrationEvent : TelecomEvent {
        class OnOffline : RegistrationEvent()
        class OnNotRegistered : RegistrationEvent()
        class OnRegistered : RegistrationEvent()
        class OnRegistering : RegistrationEvent()
        class OnNoConnection : RegistrationEvent()
        data class OnError(
            val error: RegistrationError,
            val errorDescription: String,
        ) : RegistrationEvent()
    }

    sealed class CallEvent(open val call: Call) : TelecomEvent {
        data class OnCallConnected(
            override val call: Call,
        ) : CallEvent(call)

        data class OnCallHold(
            override val call: Call,
        ) : CallEvent(call)

        data class OnCallMuted(
            val isMuted: Boolean,
        )

        data class OnCallDisconnected(
            override val call: Call,
        ) : CallEvent(call)

        data class OnCallError(
            override val call: Call,
            val error: CallError,
            val errorDescription: String,
        ) : CallEvent(call)

        data class OnNewCall(
            override val call: Call,
        ) : CallEvent(call)

        data class OnConferenceStarted(
            override val call: Call,
            val isInConference: Boolean,
        ) : CallEvent(call)

        data class OnCallUserActionRequired(
            override val call: Call,
            val pendingEvent: CallPendingEvent,
        ) : CallEvent(call)

        data class OnCallConnectionLost(
            override val call: Call,
        ) : CallEvent(call)
    }

    sealed class HardwareEvent : TelecomEvent {
        data class OnCallMuted(val call: Call) : HardwareEvent()

        @Immutable
        data object OnAudioRouteChanged : HardwareEvent()

    }
}
