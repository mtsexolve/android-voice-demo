package com.exolve.voicedemo.features.settings

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.exolve.voicedemo.core.telecom.TelecomEvent
import com.exolve.voicedemo.core.uiCommons.BaseViewModel
import com.exolve.voicedemo.core.utils.SharingProvider
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


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
            voipBackgroundRunning = telecomManager.isBackgroundRunningEnabled(),
            detectCallLocation = telecomManager.isDetectLocationEnabled(),
            telecomManagerMode = telecomManager.telecomManagerIntegrationMode(),
            sipTraces = telecomManager.isSipTracesEnabled(),
            logLevel = telecomManager.logLevel(),
            useEncryption = telecomManager.isEncryptionEnabled(),
            environment = telecomManager.currentEnvironment(),
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

            is SettingsContract.Event.OnBackgroundRunningChanged -> {
                telecomManager.setBackgroundRunningEnabled(event.enabled)
                setState { copy(voipBackgroundRunning = telecomManager.isBackgroundRunningEnabled()) }
            }

            is SettingsContract.Event.OnCallLocationDetectChanged -> {
                telecomManager.setDetectLocationEnabled(event.enabled)
                setState { copy(detectCallLocation = telecomManager.isDetectLocationEnabled()) }
            }

            is SettingsContract.Event.OnTelecomManagerModeChanged -> {
                telecomManager.setTelecomIntegrationMode(event.mode)
                setState { copy(telecomManagerMode = event.mode) }
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

            is SettingsContract.Event.OnEnvironmentChanged -> {
                telecomManager.setEnvironment(event.environment)
                setState { copy(
                    environment = event.environment,
                    needRestart = true
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

    override fun onCleared() {
        super.onCleared()
        viewModelScope.coroutineContext.cancel()
    }
}

