package com.exolve.voicedemo.features.settings

import android.app.Application
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
            detectCallLocation = telecomManager.isDetectCallLocationEnabled(),
            telecomManagerMode = telecomManager.telecomManagerIntegrationMode()
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
                telecomManager.setDetectCallLocationEnabled(event.enabled)
                setState { copy(detectCallLocation = telecomManager.isDetectCallLocationEnabled()) }
            }

            is SettingsContract.Event.OnTelecomManagerModeChanged -> {
                telecomManager.setTelecomIntegrationMode(event.mode)
                setState { copy(telecomManagerMode = event.mode) }
            }

            else -> {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.coroutineContext.cancel()
    }
}

