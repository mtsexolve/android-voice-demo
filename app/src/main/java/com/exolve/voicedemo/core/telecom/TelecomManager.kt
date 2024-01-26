package com.exolve.voicedemo.core.telecom

import android.app.Application
import android.util.Log
import com.exolve.voicedemo.core.models.Account
import com.exolve.voicedemo.core.repositories.AccountRepository
import com.exolve.voicesdk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.exolve.voicedemo.app.activities.*;

private const val TELECOM_MANAGER = "TelecomManager"

class TelecomManager(private var context: Application) {

    private val _telecomManagerState = MutableStateFlow(TelecomContract.State())
    val telecomManagerState = _telecomManagerState.asStateFlow()
    private val _telecomEvents = MutableSharedFlow<TelecomEvent>(replay = 0, extraBufferCapacity = 10)
    val telecomEvents: SharedFlow<TelecomEvent> = _telecomEvents.asSharedFlow()

    private val configuration: Configuration = Configuration
        .builder(context)
        .logConfiguration(LogConfiguration.builder().logLevel(LogLevel.DEBUG).build())
        .enableSipTrace(true)
        .enableNotifications(true)
        .notificationConfiguration(
            NotificationConfiguration().apply {
                callActivityClass = CallActivity::class.java.canonicalName
                appActivityClass = MainActivity::class.java.canonicalName
            }
        )
        .build()

    private val callClient: CallClient = Communicator
        .initialize(context, configuration, ApplicationState.BACKGROUND)
        .callClient

    init {
        Log.d(TELECOM_MANAGER, "init: callClient = $callClient")
        callClient.run {
            setCallsListener(
                CallsListener(
                    this@TelecomManager,
                    telecomManagerEvent = _telecomEvents,
                    telecomManagerState = telecomManagerState,
                    context = context
                    ),
                context.mainLooper
            )
            setRegistrationListener(RegistrationListener(this@TelecomManager,context), context.mainLooper)
        }
    }

    suspend fun emitTelecomEvent(event: TelecomEvent) {
        _telecomEvents.emit(event)
    }

     fun activateAccount(accountModel: Account?) {
        Log.d(TELECOM_MANAGER, "activateAccount: ${accountModel?.number}")
        if (Communicator.getInstance().callClient.registrationState != RegistrationState.REGISTERED) {
            CoroutineScope(Dispatchers.IO).launch {
                accountModel?.let {  AccountRepository(context).saveAccountDetails(accountModel)}
                Communicator.getInstance().run {
                    callClient.register(accountModel?.number ?: " ", accountModel?.password ?: " ")
                }
            }
        } else {
            Log.w(TELECOM_MANAGER, "activateAccount: already activated, deactivate before new activation")
        }
     }

     fun unregisterAccount() {
        Log.d(TELECOM_MANAGER, "deactivateAccount")
        CoroutineScope(Dispatchers.IO).launch {
            Communicator.getInstance().callClient.unregister()
        }
     }

     fun getCalls(): List<Call> {
        return telecomManagerState.value.calls
     }

     fun call(number: String) {
        telecomManagerState.value.calls.takeIf { it.isNotEmpty() }?.forEach { it.hold() }
        Log.d(TELECOM_MANAGER, "call: number = ${number}")
        callClient.placeCall(number)
     }

     fun acceptCall(callId: String) {
        Log.d(TELECOM_MANAGER, "acceptCall: id = $callId")
        telecomManagerState.value.calls.takeIf { it.isNotEmpty() }?.find { it.id == callId }?.accept()
     }

     fun terminateCall(callId: String) {
        Log.d(TELECOM_MANAGER, "terminateCall: id = $callId")
        telecomManagerState.value.calls.find { it.id == callId }?.terminate()
     }

     fun holdCall(callId: String) {
        Log.d(TELECOM_MANAGER, "holdCall: id = $callId")
        telecomManagerState.value.calls.find { it.id == callId }?.hold()
     }

     fun resumeCall(callId: String) {
        Log.d(TELECOM_MANAGER, "resumeCall: id = $callId")
        telecomManagerState.value.calls.find { it.id == callId }?.resume()
     }

     fun transferCall(callId: String, targetNumber: String) {
        getCalls().find { it.id == callId }?.transfer(targetNumber)
        Log.d(TELECOM_MANAGER, "transferCall: id = $callId, target number = $targetNumber")
     }

     fun sendDtmf(callId: String, digits: String) {
        Log.d(TELECOM_MANAGER, "sendDtmf: id = $callId, digits = $digits")
        telecomManagerState.value.calls.find { it.id == callId }?.sendDtmf(digits)
     }

     fun startConference(firstCallId: String, secondCallId: String) {
        val firstCall = telecomManagerState.value.calls.find { it.id == firstCallId }
        val secondCall = telecomManagerState.value.calls.find { it.id == secondCallId }
        Log.d(TELECOM_MANAGER, "startConference: firstCall = ${firstCall?.number}, secondCall = ${secondCall?.number}" +
                "\n firstCallId = $firstCallId, secondCallId = $secondCallId")
        secondCall?.let { call: Call -> firstCall?.createConference(call.id) }
     }

     fun stopConference() {
        getCalls().forEach { if (it.inConference()) it.removeFromConference() }
     }

     fun removeCallFromConference(callId: String) {
        telecomManagerState.value.calls.find { it.id == callId }?.removeFromConference()
     }

     fun addCallToConference(callId: String) {
        telecomManagerState.value.calls.find { it.id == callId }?.addToConference()
     }

     fun muteCall(callId: String, mute: Boolean) {
        Log.d(TELECOM_MANAGER, "muteCall: id = $callId, mute = $mute")
        telecomManagerState.value.calls.find { it.id == callId }?.mute(mute)
     }

     fun activateSpeaker(activateSpeaker: Boolean) {
        Log.d(TELECOM_MANAGER, "activateSpeaker: $activateSpeaker")
        callClient.isSpeakerOn = activateSpeaker
        CoroutineScope(Dispatchers.IO).launch {
            _telecomEvents.emit(TelecomContract.HardwareEvent.OnSpeakerActivated(activateSpeaker))
        }
     }

     fun getRegistrationState(): RegistrationState {
        return callClient.registrationState
     }

    fun setBackgroundState() {
        Log.d(TELECOM_MANAGER, "setBackgroundState")
        Communicator.getInstance().setApplicationState(ApplicationState.BACKGROUND);
    }

    fun setForegroundState() {
        Log.d(TELECOM_MANAGER, "setForegroundState")
        Communicator.getInstance().setApplicationState(ApplicationState.FOREGROUND);
    }

    fun setToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch { setState { copy(token = token) } }
    }

    suspend fun setState(reduce: TelecomContract.State.() -> TelecomContract.State) {
        withContext(Dispatchers.Main){
            _telecomManagerState.emit(telecomManagerState.value.reduce())
        }
    }

    companion object {
        private var INSTANCE: TelecomManager? = null

        fun initialize(context: Application) {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = TelecomManager(context)
                    Log.d(TELECOM_MANAGER, "initialize: $INSTANCE")
                }
            }
        }
        fun getInstance(): TelecomManager {
            return INSTANCE!!
        }
    }

}
