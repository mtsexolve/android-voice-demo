package com.exolve.voicedemo.features.settings

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.exolve.voicedemo.R
import com.exolve.voicedemo.app.CallApplication
import com.exolve.voicedemo.core.models.Account
import com.exolve.voicedemo.core.repositories.SettingsRepository
import com.exolve.voicedemo.core.telecom.TelecomContract.RegistrationEvent
import com.exolve.voicedemo.core.telecom.TelecomEvent
import com.exolve.voicedemo.core.uiCommons.BaseViewModel
import com.exolve.voicedemo.core.utils.SharingProvider
import com.exolve.voicesdk.RegistrationState
import kotlinx.coroutines.*


const val SETTINGS_VIEWMODEL = "SettingsViewModel"

@Immutable
class SettingsViewModel(application: Application) :
    BaseViewModel<SettingsContract.Event, SettingsContract.State, SettingsContract.Effect>(application) {
    private val sharingProvider = SharingProvider(application)
    private val accountRepository: SettingsRepository = SettingsRepository(context = application)

    init {
        Log.d(SETTINGS_VIEWMODEL, "init: telecomManager = $telecomManager" )

        viewModelScope.launch {
            launch(Dispatchers.IO) {
                telecomManager.telecomEvents.collect {
                    handleTelecomEvent(it)
                }
            }
            launch {
                telecomManager.telecomManagerState.collect {
                    if (uiState.value.token != it.token) {
                        Log.d(SETTINGS_VIEWMODEL, "Setting VM: new token collected: ${it.token}")
                        setState {
                            copy(
                                token = it.token
                                    ?: getApplication<CallApplication>()
                                        .getString(R.string.default_token)
                                        .lowercase(),
                            )
                        }
                    }
                }
            }
            launch(Dispatchers.IO) {
                Log.d(SETTINGS_VIEWMODEL, "launch getAccountDetail")
                accountRepository.fetchAccountDetails().let {
                    setState {
                        copy(
                            number = it.number ?: "",
                            password = it.password ?: "",
                            registrationState = telecomManager.getRegistrationState()
                        )
                    }
                    Log.d(SETTINGS_VIEWMODEL, "setState() loaded ")
                }
            }
            launch {
                setState { copy(versionDescription = telecomManager.getVersionDescription() ) }
            }
        }
    }

    override fun initializeState(): SettingsContract.State {
        Log.d(SETTINGS_VIEWMODEL, "initializeState")
        return SettingsContract.State(
            number = "" ,
            password = "",
            token = getApplication<CallApplication>().getString(R.string.default_token).lowercase(),
            versionDescription = "",
            registrationState = telecomManager.getRegistrationState(),
            voipBackgroundRunning = telecomManager.isBackgroundRunningEnabled()
        )
    }

    override suspend fun handleTelecomEvent(event: TelecomEvent) {
        when (event) {
            is RegistrationEvent -> handleRegistrationEvent(event = event)
        }
    }

    private fun handleRegistrationEvent(event: RegistrationEvent) {
        Log.d(SETTINGS_VIEWMODEL, "handleRegistrationEvent: $event")
        setState { copy(registrationState = telecomManager.getRegistrationState()) }
    }

    override fun handleUiEvent(event: SettingsContract.Event) {
        when (event) {
            is SettingsContract.Event.OnActivateClicked -> { activateAccount() }
            is SettingsContract.FillableLoginField -> { updateTextFieldState(event, event.textState) }
            is SettingsContract.Event.OnSendLogsClicked -> { sharingProvider.share("Share sdk logs") }
            is SettingsContract.Event.OnCopyButtonClicked -> { copyTokenToClipBoard() }
            is SettingsContract.Event.OnBackgroundRunningChanged -> {  enableBackgroundRunning(event.enabled)
                setState { copy(voipBackgroundRunning = telecomManager.isBackgroundRunningEnabled()) }
            }
            else -> {}
        }
    }

    private fun activateAccount() {
        if(telecomManager.getRegistrationState() == RegistrationState.NOT_REGISTERED){
            Log.d(
                SETTINGS_VIEWMODEL,
                "activateAccount: username = ${uiState.value.number}"
            )

            telecomManager.activateAccount(
                Account(
                    number = uiState.value.number, password = uiState.value.password,
                    ),
                )
        } else {
            Log.d(SETTINGS_VIEWMODEL, "settings_vm: deactivate }")
            telecomManager.unregisterAccount()
        }
    }
    

    private fun copyTokenToClipBoard() {
        (getApplication<CallApplication>()
            .applicationContext.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager)?.let {
                val clipData = ClipData.newPlainText("token", uiState.value.token)
                it.setPrimaryClip(clipData)
        }
    }

    private fun updateTextFieldState(event: SettingsContract.Event, newState: String) {
        if (event is SettingsContract.FillableLoginField)
            when (event) {
                is SettingsContract.Event.UserTexFieldChanged -> { setState { copy(number = newState) } }
                is SettingsContract.Event.PasswordTexFieldChanged -> { setState { copy(password = newState) } }
                else -> {}
        }
    }

    private fun enableBackgroundRunning(enable: Boolean) {
        telecomManager.setBackgroundRunningEnabled(enable)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.coroutineContext.cancel()
    }
}

