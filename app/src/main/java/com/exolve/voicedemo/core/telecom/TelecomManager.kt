package com.exolve.voicedemo.core.telecom

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.util.Log
import com.exolve.voicedemo.R
import com.exolve.voicedemo.app.activities.CallActivity
import com.exolve.voicedemo.app.activities.MainActivity
import com.exolve.voicedemo.core.models.Account
import com.exolve.voicedemo.core.repositories.SettingsRepository
import com.exolve.voicesdk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

private const val TELECOM_MANAGER = "TelecomManager"

class TelecomManager(private var context: Application) {

    private val _telecomManagerState = MutableStateFlow(TelecomContract.State())
    val telecomManagerState = _telecomManagerState.asStateFlow()
    private val _telecomEvents = MutableSharedFlow<TelecomEvent>(replay = 0, extraBufferCapacity = 10)
    val telecomEvents: SharedFlow<TelecomEvent> = _telecomEvents.asSharedFlow()
    private val missedCallsChannelId = "missed_calls_channel"
    private var _callContext: String = ""

    private val _contactNameResolver = ContactNameResolverExt { phoneNumber, extraContext, resultHandler ->
            Log.d(TELECOM_MANAGER, "Contact resolver lookup: phone: $phoneNumber, context: $extraContext")
            CoroutineScope(Dispatchers.Main).launch {
                if (!TextUtils.isEmpty(extraContext)) {
                    // to emulate delay for contact lookup via API
                    delay(150)
                    resultHandler!!.accept("Call with context: $extraContext")
                } else if (phoneNumber == "74997090111") {
                    resultHandler!!.accept("Telecom company")
                }
            }
        }

    private val configuration: Configuration = Configuration
        .builder(context)
        .logConfiguration(LogConfiguration.builder().logLevel(logLevel()).build())
        .useSecureConnection(isEncryptionEnabled())
        .enableSipTrace(isSipTracesEnabled())
        .enableNotifications(true)
        .enableRingtone(true)
        .enableBackgroundRunning(SettingsRepository(context).isBackgroundRunningEnabled())
        .enableDetectLocation(SettingsRepository(context).isDetectLocationEnabled())
        .telecomIntegrationMode(SettingsRepository(context).getTelecomManagerMode())
        .notificationConfiguration(
            NotificationConfiguration().apply {
                callActivityClass = CallActivity::class.java.canonicalName
                appActivityClass = MainActivity::class.java.canonicalName
                setContactNameResolver(_contactNameResolver)
                notifyInForeground = true
            }
        )
        .build()

    private val communicator: Communicator

    private val callClient: CallClient

    init {
        val env = SettingsRepository(context).getEnvironment()
        communicator = if (!env.isNullOrEmpty()) {
            Communicator.initialize(context, configuration, ApplicationState.BACKGROUND, env)
        } else {
            Communicator.initialize(context, configuration, ApplicationState.BACKGROUND)
        }

        createNotificationChannel()

        CoroutineScope(Dispatchers.IO).launch {
            telecomEvents.collect { telecomEvent ->
                if (telecomEvent is TelecomContract.CallEvent.OnCallDisconnected) {
                    val isMissed = !telecomEvent.call.isOutCall
                            && telecomEvent.disconnectDetails?.duration == 0
                            && telecomEvent.disconnectDetails.disconnectReason == DisconnectReason.ENDED_BY_PEER
                    val isSystemManaged =
                        SettingsRepository(context).getTelecomManagerMode() == TelecomIntegrationMode.SYSTEM_MANAGED_SERVICE
                    if (isMissed && !isSystemManaged) {
                        // In SYSTEM_MANAGED_SERVICE mode a notification will be shown by the Phone app
                        showMissedCallNotification(telecomEvent.call.formattedNumber)
                    }
                }
            }
        }

        callClient = communicator.callClient
        Log.d(TELECOM_MANAGER, "init: callClient = $callClient")
        callClient.run {
            setCallsListener(
                CallsListener(
                    this@TelecomManager,
                    telecomManagerEvent = _telecomEvents,
                    telecomManagerState = telecomManagerState,
                    context = context
                ), context.mainLooper
            )
            setRegistrationListener(RegistrationListener(this@TelecomManager, context), context.mainLooper)
            setAudioRouteListener(AudioRouteListener(this@TelecomManager), context.mainLooper)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            missedCallsChannelId,
            context.getString(R.string.missed_calls_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.missed_calls_channel_description)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


    private fun showMissedCallNotification(number: String) {
        val notification = Notification.Builder(context, missedCallsChannelId)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentText(context.getString(R.string.missed_call_notification_text, number))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Notification.CATEGORY_MISSED_CALL else Notification.CATEGORY_CALL)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    fun getVersionDescription(): String {
        val versionInfo: VersionInfo = communicator.versionInfo
        return "SDK ver.${versionInfo.buildVersion} env: ${versionInfo.environment.ifEmpty { "default" }}"
    }

    suspend fun emitTelecomEvent(event: TelecomEvent) {
        _telecomEvents.emit(event)
    }

    fun activateAccount(accountModel: Account) {
        Log.d(TELECOM_MANAGER, "activateAccount: ${accountModel.number}")
        if (callClient.registrationState != RegistrationState.REGISTERED) {
            CoroutineScope(Dispatchers.IO).launch {
                accountModel.let { SettingsRepository(context).saveAccountDetails(accountModel) }
                communicator.run {
                    callClient.register(accountModel.number, accountModel.password)
                }
            }
        } else {
            Log.w(TELECOM_MANAGER, "activateAccount: already activated, deactivate before new activation")
        }
    }

    fun unregisterAccount() {
        Log.d(TELECOM_MANAGER, "deactivateAccount")
        CoroutineScope(Dispatchers.IO).launch {
            callClient.unregister()
        }
    }

    fun getCalls(): List<Call> {
        return telecomManagerState.value.calls
    }

    fun call(number: String) {
        telecomManagerState.value.calls.takeIf { it.isNotEmpty() }?.forEach { it.hold() }
        Log.d(TELECOM_MANAGER, "call: number = $number")
        callClient.placeCall(number, _callContext)
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

    fun transferToNumber(callId: String, targetNumber: String) {
        getCalls().find { it.id == callId }?.transferToNumber(targetNumber)
        Log.d(TELECOM_MANAGER, "transfer call $callId to number $targetNumber")
    }

    fun transferToCall(callId: String, targetCall: String) {
        getCalls().find { it.id == callId }?.transferToCall(targetCall)
        Log.d(TELECOM_MANAGER, "transfer call $callId to call $targetCall")
    }

    fun sendDtmf(callId: String, digits: String) {
        Log.d(TELECOM_MANAGER, "sendDtmf: id = $callId, digits = $digits")
        telecomManagerState.value.calls.find { it.id == callId }?.sendDtmf(digits)
    }

    fun qualityRating(callId: String): Float? {
        return telecomManagerState.value.calls.find { it.id == callId }?.statistics?.currentRating
    }

    fun startConference(firstCallId: String, secondCallId: String) {
        val firstCall = telecomManagerState.value.calls.find { it.id == firstCallId }
        val secondCall = telecomManagerState.value.calls.find { it.id == secondCallId }
        Log.d(
            TELECOM_MANAGER, "startConference: firstCall = ${firstCall?.number}, secondCall = ${secondCall?.number}" +
                    "\n firstCallId = $firstCallId, secondCallId = $secondCallId"
        )
        secondCall?.let { call: Call -> firstCall?.createConference(call.id) }
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

    fun setAudioRoute(route: AudioRoute) {
        Log.d(TELECOM_MANAGER, "set audio route: $route")
        callClient.setAudioRoute(route)
    }

    fun getRegistrationState(): RegistrationState {
        return callClient.registrationState
    }

    fun setBackgroundState() {
        Log.d(TELECOM_MANAGER, "setBackgroundState")
        communicator.setApplicationState(ApplicationState.BACKGROUND)
    }

    fun setForegroundState() {
        Log.d(TELECOM_MANAGER, "setForegroundState")
        communicator.setApplicationState(ApplicationState.FOREGROUND)
    }

    fun isBackgroundRunningEnabled(): Boolean {
        return communicator.configurationManager.isBackgroundRunningEnabled
    }

    fun setBackgroundRunningEnabled(enable: Boolean) {
        communicator.configurationManager.isBackgroundRunningEnabled = enable
        SettingsRepository(context).setBackgroundRunningEnabled(enable)
    }

    fun isDetectLocationEnabled(): Boolean {
        return SettingsRepository(context).isDetectLocationEnabled()
    }

    fun setDetectLocationEnabled(enable: Boolean) {
        communicator.configurationManager.setDetectLocationEnabled(enable)
        SettingsRepository(context).setDetectCallLocationEnabled(enable)
    }

    fun setToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch { setState { copy(token = token) } }
    }

    fun telecomManagerIntegrationMode(): TelecomIntegrationMode {
        return communicator.configurationManager.telecomIntegrationMode
    }

    fun setTelecomIntegrationMode(mode: TelecomIntegrationMode) {
        communicator.configurationManager.telecomIntegrationMode = mode
        SettingsRepository(context).setTelecomManagerMode(mode)
    }

    fun isSipTracesEnabled(): Boolean {
        return SettingsRepository(context).isSipTracesEnabled()
    }

    fun setSipTracesEnabled(enabled: Boolean) {
        SettingsRepository(context).setSipTracesEnabled(enabled)
    }

    fun logLevel(): LogLevel {
        return SettingsRepository(context).getLogLevel()
    }

    fun setLogLevel(level: LogLevel) {
        SettingsRepository(context).setLogLevel(level)
    }

    fun isEncryptionEnabled(): Boolean {
        return SettingsRepository(context).isEncryptionEnabled()
    }

    fun setEncryptionEnabled(enabled: Boolean) {
        SettingsRepository(context).setEncryptionEnabled(enabled)
    }

    fun currentEnvironment(): String {
        return communicator.versionInfo.environment
    }

    fun availableEnvironments(): List<String> {
        val arr = buildList {
            add("default")
            addAll(Communicator.getAvailableEnvironments() as List<String>)
        }
        return arr
    }

    fun setEnvironment(environment: String) {
        SettingsRepository(context).setEnvironment(environment)
    }

    suspend fun setState(reduce: TelecomContract.State.() -> TelecomContract.State) {
        withContext(Dispatchers.Main) {
            _telecomManagerState.emit(telecomManagerState.value.reduce())
        }
    }

    fun updateAudioRoutes() {
        CoroutineScope(Dispatchers.IO).launch {
            _telecomEvents.emit(TelecomContract.HardwareEvent.OnAudioRouteChanged)
        }
    }

    fun getAudioRoutes(): List<AudioRouteData> {
        return callClient.audioRoutes
    }

    fun isNotificationInForegroundEnabled(): Boolean {
        return configuration.notifyInForeground
    }

    fun setCallContext(context: String) {
        _callContext = context
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
