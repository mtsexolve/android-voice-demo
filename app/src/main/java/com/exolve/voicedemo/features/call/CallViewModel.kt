package com.exolve.voicedemo.features.call

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.exolve.voicedemo.core.telecom.TelecomContract
import com.exolve.voicedemo.core.telecom.TelecomContract.CallEvent
import com.exolve.voicedemo.core.telecom.TelecomEvent
import com.exolve.voicedemo.core.telecom.TelecomManager
import com.exolve.voicedemo.core.uiCommons.BaseViewModel
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import com.exolve.voicedemo.features.call.CallContract.*
import com.exolve.voicedemo.features.dialer.DialerContract
import com.exolve.voicesdk.CallState
import com.exolve.voicesdk.Call
import com.exolve.voicesdk.platform.AudioRoute
import kotlinx.coroutines.launch
import javax.annotation.concurrent.Immutable

private const val CALL_VIEW_MODEL = "CallViewModel"

@Immutable
class CallViewModel(application: Application) :
    BaseViewModel<UiEvent, State, Effect>(application) {

    init {
        Log.d(CALL_VIEW_MODEL, "CalLViewModel init")
        val routes = TelecomManager.getInstance().getAudioRoutes()
        if (routes.any { it.route == AudioRoute.BLUETOOTH }) {
            if (routes.none { it.active && it.route == AudioRoute.BLUETOOTH }) {
                TelecomManager.getInstance().setAudioRoute(AudioRoute.BLUETOOTH)
            }
        }

        viewModelScope.launch {
            telecomManager.telecomEvents.collect {
                handleTelecomEvent(it)
            }
        }
    }

    override fun initializeState(): State {
        val calls = mutableListOf<State.CallItemState>()
        telecomManager.getCalls().forEach {
            calls.add(configureItemListStateObject(it, calls))
        }
        Log.d(CALL_VIEW_MODEL, "CalLViewModel: init list size :${calls.size}")
        return State(
            currentCallNumber = calls.getOrNull(calls.lastIndex)?.number ?: "",
            currentCallId = calls.getOrNull(calls.lastIndex)?.callsId ?: "",
            calls,
            audioRoutes = telecomManager.getAudioRoutes(),
            selectedAudioRoute = AudioRoute.UNKNOWN // because user not selected any of
        )
    }

    private fun configureItemListStateObject(
        call: Call,
        list: MutableList<State.CallItemState>
    ) = State.CallItemState(
        isCallOutgoing = call.isOutCall,
        number = call.number,
        callsId = call.id,
        status = call.state,
        isInConference = call.inConference(),
        isMuted = call.isMuted,
        indexForUiTest = ((list.find { callFromList -> call.id == callFromList.callsId })
            ?.let { list.indexOf(it) } ?: list.size)
    )


    override fun handleUiEvent(event: UiEvent) {
        when (event) {
            is Event.OnTerminateCurrentButtonClicked -> { terminateSelectedCall(event.callsId) }
            is Event.OnTerminateAllButtonClicked -> { terminateAllCalls() }
            is Event.OnResumeButtonClicked -> {
                setState { copy(
                    currentCallId = event.callsId,
                    currentCallNumber = uiState.value.calls.find {it.callsId == event.callsId}?.number ?: ""
                ) }
                resumeCall(event.callsId)
            }
            is Event.OnHoldButtonClicked -> {
                setState { copy(
                    currentCallId = "",
                    currentCallNumber = "",
                ) }
                holdCall(event.callsId)
            }
            is Event.OnAudioRouteSelect -> { setAudioRoute(event.route) }
            is Event.OnSpeakerButtonClicked -> {
                if (uiState.value.audioRoutes.size > 2) {
                    requestAudioRoutes()
                } else {
                    handleSpeaker()
                }
            }
            is Event.OnMuteButtonClicked -> { muteCall() }
            is Event.OnDtmfButtonClicked -> { handleDtmf() }
            is Event.OnTransferNumberSelected -> { transferCall(event.selectedCall) }
            is Event.OnBackToControlScreenClicked -> { setState { copy(isAddNewCallPressed = false) } }
            is Event.OnNewCallButtonClicked -> { setState { copy(isAddNewCallPressed = true) } }
            is Event.OnAcceptCallButtonClicked -> {
                setState { copy(
                    currentCallId = event.callsId,
                    currentCallNumber = uiState.value.calls.find {it.callsId == event.callsId}?.number ?: ""
                ) }
                acceptCall(event.callsId)
            }
            is DialerContract.Event.OnDigitButtonClicked -> {
                updateTextFieldState(event.index)
                telecomManager.sendDtmf(uiState.value.currentCallId, event.index)
            }
            is Event.OnItemsSwapped -> {
                uiState.value.calls.toMutableList()
                    .apply {
                        val tmp = this[event.ind1]
                        this[event.ind1] = this[event.ind2]
                        this[event.ind2] = tmp
                    }
                    .also { setState { copy(calls = it) } }

            }
            is Event.OnItemDroppedOnItem -> { startConference(event.firstIndex, event.secondIndex) }
            is Event.OnAddCallToConference -> { telecomManager.addCallToConference(callId = event.callsId) }
            is Event.OnRemoveCallFromConference -> { telecomManager.removeCallFromConference(callId = event.callsId) }
        }
    }

    private fun updateUiListOfCalls(call: Call) {
        val hasCallInUiList = uiState.value.calls.find { it.callsId == call.id } != null
        val newUiListOfCalls = uiState.value.calls.toMutableList()
        when {
            hasCallInUiList && call.state != CallState.DISCONNECTED -> {
                Log.d(CALL_VIEW_MODEL, "CalLViewModel: upd list, hasCallInUiList && !isDisconnected  ")
                newUiListOfCalls.apply {
                    val indexOfCall = this.indexOf(this.find { uiCall -> uiCall.callsId == call.id })
                    this[indexOfCall] = configureItemListStateObject(call = call, list = newUiListOfCalls)
                }
            }
            !hasCallInUiList && call.state != CallState.DISCONNECTED -> {
                Log.d(CALL_VIEW_MODEL, "CalLViewModel: upd list,  !hasCallInUiList && !isDisconnected ")
                newUiListOfCalls.add(configureItemListStateObject(call = call, list = newUiListOfCalls))
            }
            hasCallInUiList && call.state == CallState.DISCONNECTED -> {
                Log.d(CALL_VIEW_MODEL, "CalLViewModel: upd list,  hasCallInUiList && isDisconnected")
                newUiListOfCalls.apply {
                    this.removeAt(this.indexOf(this.find { uiCall -> uiCall.callsId == call.id }))
                }
            }
        }
        Log.d(CALL_VIEW_MODEL, "CalLViewModel: upd list, telecomList size :${telecomManager.getCalls().size}")
        setState { copy(calls = newUiListOfCalls, hasConference = newUiListOfCalls.find { it.isInConference }?.isInConference ?: false) }
    }

    override suspend fun handleTelecomEvent(event: TelecomEvent) {
        if (event is CallEvent) {
            Log.d(CALL_VIEW_MODEL, "CalLViewModel: handleTelecomEvent: $event ${event.call.id}")
            updateUiListOfCalls(event.call)
        } else {
            handleHardwareEvent(event)
        }
    }

    private fun handleHardwareEvent(event: TelecomEvent) {
        (event as? TelecomContract.HardwareEvent)?.let {
            when(event) {
                is TelecomContract.HardwareEvent.OnSpeakerActivated ->
                    setState { copy(isSpeakerPressed = event.isActivated) }
                is TelecomContract.HardwareEvent.OnCallMuted ->
                    setState {
                        val call = uiState.value.calls.find { it.callsId ==  event.call.id }
                        call?.let {
                            copy( calls = uiState.value.calls.toMutableList().apply {
                                val idx = this.indexOf(this.find { uiCall -> uiCall.callsId ==  event.call.id })
                                this[idx] = call.copy(isMuted = event.call.isMuted)
                            })
                        }?: uiState.value
                    }
                is TelecomContract.HardwareEvent.OnAudioRouteChanged -> {
                    val routes = TelecomManager.getInstance().getAudioRoutes()
                    if (uiState.value.selectedAudioRoute != AudioRoute.UNKNOWN
                        && routes.none { it.active && it.route == uiState.value.selectedAudioRoute }) {
                        return;
                    }
                    val noBluetoothInRoutes = uiState.value.audioRoutes.none { it.route == AudioRoute.BLUETOOTH }
                    setState { copy(
                        audioRoutes = routes,
                        selectedAudioRoute = AudioRoute.UNKNOWN
                    ) }
                    val isBluetoothInRoutes = routes.any { it.route == AudioRoute.BLUETOOTH }
                    if (noBluetoothInRoutes && isBluetoothInRoutes) {
                        Log.d(CALL_VIEW_MODEL, "CallViewModel: bluetooth device appears, connect now")
                        TelecomManager.getInstance().setAudioRoute(AudioRoute.BLUETOOTH)
                    }
                }
            }
        }
    }

    private fun acceptCall(callsId: String) {
        telecomManager.acceptCall(callId = callsId)
    }

    private fun updateTextFieldState(value: String) {
        setState { copy(dialerText = dialerText + value) }
    }

    private fun transferCall(selectedCalNumber: String) {
        Log.d(CALL_VIEW_MODEL, "CalLViewModel: transfer call: selectedNumber = $selectedCalNumber")
        telecomManager.transferCall(
            uiState.value.currentCallId, selectedCalNumber.replace("[^0-9]".toRegex(), "")
        )
        setState { copy(isTransferPressed = !isTransferPressed) }
    }

    private fun handleDtmf() {
        setState { copy(isDtmfPressed = !isDtmfPressed) }
    }

    private fun muteCall() {
        Log.d(CALL_VIEW_MODEL, "CalLViewModel: muteCall")
        if (!uiState.value.hasConference) {
            val callId = uiState.value.currentCallId
            val call = uiState.value.calls.find { it.callsId == callId }
            call?.let {
                Log.d(CALL_VIEW_MODEL, "CalLViewModel: muteCall ${it.callsId} ${!it.isMuted}")
                telecomManager.muteCall(callId = callId, mute = !it.isMuted)
            }
            return
        }

        val conferenceCalls = uiState.value.calls.filter { it.isInConference }
        val muted = conferenceCalls.fold(false) { mu, call -> mu || call.isMuted }
        conferenceCalls.forEach {
            telecomManager.muteCall(callId = it.callsId, mute = !muted)
        }
    }

    private fun handleSpeaker() {
        telecomManager.activateSpeaker(activateSpeaker = !uiState.value.isSpeakerPressed)
    }

    private fun setAudioRoute(route: AudioRoute) {
        setState { copy(
            selectedAudioRoute = route
        ) }
        telecomManager.setAudioRoute(route)
    }

    private fun requestAudioRoutes() {
        telecomManager.updateAudioRoutes()
    }

    private fun holdCall(callsId: String) {
        telecomManager.holdCall(callId = callsId)
    }

    private fun resumeCall(callsId: String) {
        telecomManager.resumeCall(callId = callsId)
    }

    private fun terminateSelectedCall(callsId: String) {
        Log.d(CALL_VIEW_MODEL, "CallViewModel: uiEvent terminate: call = $callsId")
        viewModelScope.launch {
            telecomManager.terminateCall(callId = callsId)
        }
    }

    private fun terminateAllCalls() {
        viewModelScope.launch {
            uiState.value.calls.forEach {
                Log.d(CALL_VIEW_MODEL, "CallViewModel: uiEvent terminate all, ${it.callsId}")
                telecomManager.terminateCall(it.callsId)
            }
        }
    }

    private fun startConference(firstIndexInUiList: Int?, secondIndexInUiList: Int?) {
        if (firstIndexInUiList != null && secondIndexInUiList != null) {
            telecomManager.startConference(
                uiState.value.calls.getOrNull(firstIndexInUiList)?.callsId ?: "",
                uiState.value.calls.getOrNull(secondIndexInUiList)?.callsId ?: "",
            )
        }
    }
}