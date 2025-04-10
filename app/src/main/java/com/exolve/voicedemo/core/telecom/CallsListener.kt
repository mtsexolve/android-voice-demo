package com.exolve.voicedemo.core.telecom

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.exolve.voicedemo.core.telecom.TelecomContract.CallEvent
import com.exolve.voicesdk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


private const val CALLS_LISTENER = "CallsListener"

class CallsListener(
    private val telecomManager: TelecomManager,
    private val telecomManagerEvent: MutableSharedFlow<TelecomEvent>,
    private val telecomManagerState: StateFlow<TelecomContract.State>,
    private val context: Context
) : ICallsListener {

    override fun callNew(p0: Call?) {
        Log.d(
            CALLS_LISTENER, "(markedLog): callNew(). Call ID: ${p0?.id}, " +
                    "total calls: ${telecomManager.getCalls().size}"
        )
        CoroutineScope(Dispatchers.IO).launch {
            p0?.let {
                telecomManager.setState {
                    copy(
                        calls = telecomManagerState.value.calls.apply { add(it) },
                        currentCall = p0,
                    )
                }
                telecomManagerEvent.emit(CallEvent.OnNewCall(it))
            }
        }
    }

    override fun callConnected(p0: Call?) {
        Log.d(CALLS_LISTENER, "callConnected(). Call ID: ${p0?.id}")
        CoroutineScope(Dispatchers.IO).launch {
            p0?.let {
                telecomManager.setState {
                    copy(currentCall = p0, calls = telecomManagerState.value.calls.apply {
                        this[this.indexOfFirst { otherCall: Call -> otherCall.id == it.id }] = it
                    })
                }
                telecomManagerEvent.emit(CallEvent.OnCallConnected(p0))
            }
        }
    }

    override fun callHold(p0: Call?) {
        Log.d(CALLS_LISTENER, "callHold(). Call ID: ${p0?.id}")
        CoroutineScope(Dispatchers.IO).launch {
            p0?.let {
                telecomManager.setState { copy(currentCall = p0) }
                telecomManagerEvent.emit(CallEvent.OnCallHold(p0))
            }
        }
    }

    override fun callDisconnected(call: Call, details: CallDisconnectDetails) {
        Log.d(CALLS_LISTENER, "callDisconnected(). Call ID: ${call.id}, duration: ${details.duration}, disconnectedByUser: ${details.disconnectedByUser}")
        CoroutineScope(Dispatchers.IO).launch {
            call.let {
                telecomManager.setState {
                    copy(
                        currentCall = if (telecomManagerState.value.currentCall?.id == call.id) {
                            Log.d(CALLS_LISTENER, "callDisconnected() current call is terminated")
                            telecomManagerState.value.calls.apply { remove(it) }.firstOrNull()
                        } else telecomManagerState.value.currentCall,
                        calls = telecomManagerState.value.calls.apply { remove(it) },
                    )
                }
                telecomManagerEvent.emit(CallEvent.OnCallDisconnected(it))
            }
        }
    }

    override fun callConnectionLost(call: Call) {
        Log.d(CALLS_LISTENER, "callConnectionLost(). Call ID: ${call.id}")
        CoroutineScope(Dispatchers.IO).launch {
            telecomManager.setState {
                copy(calls = telecomManagerState.value.calls.apply {
                    this[this.indexOfFirst { otherCall: Call -> otherCall.id == call.id }] = call
                })
            }
            telecomManagerEvent.emit(CallEvent.OnCallConnectionLost(call))
        }
    }

    override fun callError(p0: Call?, p1: CallError?, p2: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(CALLS_LISTENER, "callError(). Call: ${p0?.id}, error: ${p1?.toString()}, errorDescription: $p2")
            if (p0 != null && p1 != null && p2 != null) {
                telecomManagerEvent.emit(CallEvent.OnCallError(p0, p1, p2))
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        context,
                        "Call error: $p2",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                telecomManager.setState {
                    copy(
                        currentCall = if (telecomManagerState.value.currentCall?.id == p0.id) {
                            Log.d(CALLS_LISTENER, "callError() current call is terminated")
                            telecomManagerState.value.calls.apply { remove(p0) }.firstOrNull()
                        } else telecomManagerState.value.currentCall,
                        calls = telecomManagerState.value.calls.apply { remove(p0) },
                    )
                }
                telecomManagerEvent.emit(CallEvent.OnCallDisconnected(p0))
            }
        }
    }

    override fun callUserActionRequired(
        call: Call?,
        pendingEvent: CallPendingEvent?,
        action: CallUserAction?
    ) {
        if (call != null && pendingEvent != null && action == CallUserAction.NEEDS_LOCATION_ACCESS) {
            CoroutineScope(Dispatchers.IO).launch {
                telecomManagerEvent.emit(CallEvent.OnCallUserActionRequired(call, pendingEvent))
            }
        }

        val toastMessage = when (action) {
            CallUserAction.NEEDS_LOCATION_ACCESS -> "No location access for " + if (pendingEvent == CallPendingEvent.ACCEPT_CALL) {
                "accept"
            } else {
                "answering"
            } + " call."

            CallUserAction.ENABLE_LOCATION_PROVIDER -> "Disabled access to geolocation in notification panel"
            null -> "Call location error: action is null"
        }
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(
                context,
                toastMessage,
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }

    override fun callInConference(p0: Call?, p1: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            p0?.let {
                telecomManager.setState {
                    copy(calls = telecomManagerState.value.calls.apply {
                        this[this.indexOfFirst { call: Call -> call.id == p0.id }] = it
                    })
                }
                Log.d(CALLS_LISTENER, "callInConference(). Call ID:  ${p0.id}, inConf = $p1")
                telecomManagerEvent.emit(CallEvent.OnConferenceStarted(it, p1))
            }
        }
    }

    override fun callMuted(p0: Call) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(CALLS_LISTENER, "callMuted(). Call ID: ${p0.id}")
            telecomManagerEvent.emit(TelecomContract.HardwareEvent.OnCallMuted(p0))
        }
    }
}
