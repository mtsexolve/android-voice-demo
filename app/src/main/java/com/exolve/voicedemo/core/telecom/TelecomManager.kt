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
import com.exolve.voicedemo.core.utils.Utils
import com.exolve.voicesdk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant


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
            Utils.getDisplayName(context, phoneNumber).let {
                var name = it
                if (!TextUtils.isEmpty(extraContext)) {
                    name += " with context: $extraContext"
                }
                resultHandler!!.accept(name)
            }
        }
    }

    private val _callNotificationCustomizer =
        CallNotificationCustomizer { callData, notificationBuilder ->
            notificationBuilder.setContentText(callData.contactName)
            if (callData.viewState.equals(CallNotificationContent.ViewState.CALL_INCOMING)) {
                notificationBuilder.setContentTitle("Incoming call")
                notificationBuilder.addAction(0, "Reject", callData.endCallIntent)
                    .addAction(0, "Answer", callData.answerCallIntent)
            } else if (callData.viewState.equals(CallNotificationContent.ViewState.CALL_PAUSED)) {
                notificationBuilder.setContentTitle("Paused call")
                notificationBuilder.addAction(0, "End", callData.endCallIntent)
                    .addAction(0, "Resume", callData.resumeCallIntent)
            } else {
                notificationBuilder.setContentTitle("Ongoing call")
                notificationBuilder.addAction(0, "End", callData.endCallIntent)
            }
        }

    private val configuration: Configuration = Configuration
        .builder(context)
        .logConfiguration(LogConfiguration.builder().logLevel(logLevel()).build())
        .useSecureConnection(isEncryptionEnabled())
        .enableSipTrace(isSipTracesEnabled())
        .enableStun(isStunNeeded())
        .enableNotifications(true)
        .enableRingtone(true)
        .enableDetectLocation(SettingsRepository(context).isDetectLocationEnabled())
        .telecomIntegrationMode(SettingsRepository(context).getTelecomManagerMode())
        .notificationConfiguration(getNotificationConfiguration())
        .build()

    private fun getNotificationConfiguration(): NotificationConfiguration {
        val conf = NotificationConfiguration().apply {
            callActivityClass = CallActivity::class.java.canonicalName
            appActivityClass = MainActivity::class.java.canonicalName
            setContactNameResolver(_contactNameResolver)
            notifyInForeground = SettingsRepository(context).isNotifyInForeground()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                preferredCallStyleNotifications = true
            }
            if (SettingsRepository(context).isCustomCallNotification()) {
                setCallNotificationCustomizer(_callNotificationCustomizer)
            }
        }
        return conf
    }

    private val communicator: Communicator

    private val callClient: CallClient

    init {
        val env = SettingsRepository(context).getEnvironment()
        val savedRegistrationMode = SettingsRepository(context).getRegistrationMode()
        communicator = if (!env.isNullOrEmpty()) {
            Communicator.initialize(context, configuration, savedRegistrationMode, ApplicationState.BACKGROUND, env)
        } else {
            Communicator.initialize(context, configuration, savedRegistrationMode, ApplicationState.BACKGROUND)
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
                        showMissedCallNotification(telecomEvent.call)
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
            setCredentialsProvider(CredentialsProvider(SettingsRepository(context)), context.mainLooper)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            missedCallsChannelId,
            context.getString(R.string.missed_calls_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.missed_calls_channel_description)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun showMissedCallNotification(call: Call) {
        val notification = Notification.Builder(context, missedCallsChannelId)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentText(context.getString(R.string.missed_call_notification_text, Utils.getDisplayName(context, call.number)?:call.formattedNumber))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Notification.CATEGORY_MISSED_CALL else Notification.CATEGORY_CALL)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    .putExtra(Utils.CALL_NUMBER_EXTRA, call.number),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)
            .setShowWhen(true)
            .build()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = (Instant.now().epochSecond and Int.MAX_VALUE.toLong()).toInt()
        notificationManager.notify(id, notification)
    }

    fun getVersionDescription(): String {
        val versionInfo: VersionInfo = communicator.versionInfo
        return "SDK ver.${versionInfo.buildVersion} env: ${versionInfo.environment.ifEmpty { "default" }}"
    }

    suspend fun emitTelecomEvent(event: TelecomEvent) {
        _telecomEvents.emit(event)
    }

    fun activateAccount(account: Account) {
        Log.d(TELECOM_MANAGER, "activateAccount: ${account.number}")
        if (callClient.registrationState != RegistrationState.REGISTERED) {
            CoroutineScope(Dispatchers.IO).launch {
                account.let { SettingsRepository(context).saveAccountDetails(account) }
                communicator.run {
                    callClient.setAccount(account.number, account.password)
                }
            }
        } else {
            Log.w(TELECOM_MANAGER, "activateAccount: already activated, deactivate before new activation")
        }
    }

    fun unregisterAccount() {
        Log.d(TELECOM_MANAGER, "deactivateAccount")
        CoroutineScope(Dispatchers.IO).launch {
            callClient.clearAccount()
        }
    }

    fun getCalls(): List<Call> {
        return telecomManagerState.value.calls
    }

    fun call(number: String) {
        if (registrationMode() == RegistrationMode.PER_CALL_CREDENTIALS) {
            val account = SettingsRepository(context).fetchAccountDetails()
            account?.let {
                Log.d(TELECOM_MANAGER, "call: number = $number as ${account.number}")
                callClient.placeCall(number, _callContext, account.number, account.password)
            }
        } else {
            Log.d(TELECOM_MANAGER, "call: number = $number")
            callClient.placeCall(number, _callContext)
        }
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

    fun setCallActivityVisible(isVisible: Boolean) {
        Log.d(TELECOM_MANAGER, "setCallActivityVisible: $isVisible")
        communicator.setCallActivityVisible(isVisible)
    }

    fun registrationMode(): RegistrationMode {
        return communicator.configurationManager.registrationMode
    }

    fun setRegistrationMode(registrationMode: RegistrationMode) {
        communicator.configurationManager.registrationMode = registrationMode
        SettingsRepository(context).setRegistrationMode(registrationMode)
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
        if (mode == TelecomIntegrationMode.SYSTEM_MANAGED_SERVICE && mode != telecomManagerIntegrationMode()) {
            PendingIntent.getActivity(context, 0,
                Intent(android.telecom.TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS),
                PendingIntent.FLAG_IMMUTABLE).send()
        }
    }

    fun isSipTracesEnabled(): Boolean {
        return SettingsRepository(context).isSipTracesEnabled()
    }

    fun isStunNeeded(): Boolean {
        return SettingsRepository(context).getEnvironment()?.let { env ->
            "webrtc|dtls".toRegex(RegexOption.IGNORE_CASE).find(env) != null
        } ?: false
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

    fun isNotifyInForeground(): Boolean {
        return SettingsRepository(context).isNotifyInForeground()
    }

    fun setNotifyInForeground(enabled: Boolean) {
        SettingsRepository(context).setNotifyInForeground(enabled)
    }

    fun isCustomCallNotification(): Boolean {
        return SettingsRepository(context).isCustomCallNotification()
    }

    fun setCustomCallNotification(enabled: Boolean) {
        SettingsRepository(context).setCustomCallNotification(enabled)
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
