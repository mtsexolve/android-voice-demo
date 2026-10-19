package com.exolve.voicedemo.features.settings

import android.app.Application
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.exolve.voicedemo.R
import com.exolve.voicedemo.core.telecom.TelecomEvent
import com.exolve.voicedemo.core.permissions.RequestPermissionsResult
import com.exolve.voicedemo.core.repositories.SettingsRepository
import com.exolve.voicesdk.TelecomIntegrationMode
import com.exolve.voicedemo.core.uiCommons.BaseViewModel
import com.exolve.voicedemo.core.utils.SharingProvider
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull


private const val SETTINGS_VIEWMODEL = "SettingsViewModel"

@Immutable
class SettingsViewModel(application: Application) :
    BaseViewModel<SettingsContract.Event, SettingsContract.State, SettingsContract.Effect>(application) {
    private val sharingProvider = SharingProvider(application)
    private val context = application.applicationContext

    init {
        Log.d(SETTINGS_VIEWMODEL, "init: telecomManager = $telecomManager")

        viewModelScope.launch {
            launch {
                setState { copy(versionDescription = telecomManager.getVersionDescription()) }
            }
        }
    }

    override fun initializeState(): SettingsContract.State {
        Log.d(SETTINGS_VIEWMODEL, "initializeState")
        return SettingsContract.State(
            versionDescription = "",
            registrationMode = telecomManager.registrationMode(),
            detectCallLocation = telecomManager.isDetectLocationEnabled(),
            telecomManagerMode = telecomManager.telecomManagerIntegrationMode(),
            callContext = "",
            sipTraces = telecomManager.isSipTracesEnabled(),
            logLevel = telecomManager.logLevel(),
            useEncryption = telecomManager.isEncryptionEnabled(),
            environment = telecomManager.currentEnvironment(),
            notifyInForeground = telecomManager.isNotifyInForeground(),
            customCallNotification = telecomManager.isCustomCallNotification(),
            needRestart = false
        )
    }

    override suspend fun handleTelecomEvent(event: TelecomEvent) {
        // Do nothing
    }

    override fun handleUiEvent(event: SettingsContract.Event) {
        when (event) {
            is SettingsContract.Event.OnSendLogsClicked -> {
                sharingProvider.share("Share SDK logs")
            }

            is SettingsContract.Event.OnRegistrationModeChanged -> {
                telecomManager.setRegistrationMode(event.mode)
                setState { copy(registrationMode = telecomManager.registrationMode()) }
            }

            is SettingsContract.Event.OnCallLocationDetectChanged -> {
                telecomManager.setDetectLocationEnabled(event.enabled)
                setState { copy(detectCallLocation = telecomManager.isDetectLocationEnabled()) }
            }

            is SettingsContract.Event.OnTelecomManagerModeChanged -> {
                if (telecomManager.hasRequiredPermissionsFor(event.mode)) {
                    applyTelecomIntegrationMode(event.mode)
                } else {
                    val permissions = buildList {
                        if (event.mode != TelecomIntegrationMode.NO_INTEGRATION) {
                            add(Manifest.permission.READ_PHONE_STATE)
                        }
                        if (event.mode == TelecomIntegrationMode.SYSTEM_MANAGED_SERVICE) {
                            add(Manifest.permission.CALL_PHONE)
                        }
                    }
                    requestPermissions(permissions) { result ->
                        if (result == RequestPermissionsResult.GRANTED_ALL && telecomManager.hasRequiredPermissionsFor(event.mode)) {
                            applyTelecomIntegrationMode(event.mode)
                        } else {
                            Toast.makeText(
                                context,
                                R.string.telecom_permissions_required,
                                Toast.LENGTH_LONG
                            ).show()
                            applyTelecomIntegrationMode(TelecomIntegrationMode.NO_INTEGRATION)
                        }
                    }
                }
            }

            is SettingsContract.Event.OnSipTracesChanged -> {
                telecomManager.setSipTracesEnabled(event.enabled)
                setState { copy(
                    sipTraces = event.enabled,
                    needRestart = true
                )}
            }

            is SettingsContract.Event.OnLogLevelChanged -> {
                telecomManager.setLogLevel(event.level)
                setState { copy(
                    logLevel = event.level,
                    needRestart = true
                )}
            }

            is SettingsContract.Event.OnUseEncryptionChanged -> {
                telecomManager.setEncryptionEnabled(event.enabled)
                setState { copy(
                    useEncryption = event.enabled,
                    needRestart = true
                )}
            }

            is SettingsContract.Event.OnNotifyInForegroundChanged -> {
                telecomManager.setNotifyInForeground(event.enabled)
                setState { copy(
                    notifyInForeground = event.enabled,
                    needRestart = true
                )}
            }

            is SettingsContract.Event.OnCustomCallNotificationChanged -> {
                telecomManager.setCustomCallNotification(event.enabled)
                setState { copy(
                    customCallNotification = event.enabled,
                    needRestart = true
                )}
            }

            is SettingsContract.Event.OnEnvironmentChanged -> {
                telecomManager.setEnvironment(event.environment)
                setState { copy(
                    environment = event.environment,
                    needRestart = true
                )}
            }

            is SettingsContract.Event.OnCallContextChanged -> {
                telecomManager.setCallContext(event.callContext)
                setState { copy(
                    callContext = event.callContext
                )}
            }

            is SettingsContract.Event.Restart -> {
                val pm: PackageManager = context.getPackageManager()
                val intent = pm.getLaunchIntentForPackage(context.getPackageName())
                val mainIntent = Intent.makeRestartActivityTask(intent!!.component)
                context.startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }

            else -> {}
        }
    }

    private fun applyTelecomIntegrationMode(mode: TelecomIntegrationMode) {
        viewModelScope.launch {
            val previousMode = telecomManager.telecomManagerIntegrationMode()
            var targetMode = mode
            try {
                telecomManager.setTelecomIntegrationMode(targetMode)
            } catch (e: SecurityException) {
                Toast.makeText(
                    context,
                    R.string.telecom_permissions_required,
                    Toast.LENGTH_LONG
                ).show()
                targetMode = TelecomIntegrationMode.NO_INTEGRATION
                telecomManager.setTelecomIntegrationMode(targetMode)
            }

            val confirmedMode = withTimeoutOrNull(2_000L) {
                waitForTelecomIntegrationMode(previousMode, targetMode)
            }
            val actualMode = confirmedMode ?: telecomManager.telecomManagerIntegrationMode()

            // Only replace the requested setting when the SDK confirms its resulting mode.
            if (confirmedMode != null) {
                SettingsRepository(context).setTelecomManagerMode(actualMode)
            }
            setState { copy(telecomManagerMode = actualMode) }
        }
    }

    private suspend fun waitForTelecomIntegrationMode(
        previousMode: TelecomIntegrationMode,
        requestedMode: TelecomIntegrationMode
    ): TelecomIntegrationMode {
        while (true) {
            val currentMode = telecomManager.telecomManagerIntegrationMode()
            if (currentMode != previousMode || currentMode == requestedMode) {
                return currentMode
            }
            delay(50L)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.coroutineContext.cancel()
    }
}
