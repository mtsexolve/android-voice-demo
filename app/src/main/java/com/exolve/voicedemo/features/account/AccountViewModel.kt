package com.exolve.voicedemo.features.account

import android.Manifest
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.exolve.voicedemo.R
import com.exolve.voicedemo.app.CallApplication
import com.exolve.voicedemo.core.models.Account
import com.exolve.voicedemo.core.repositories.SettingsRepository
import com.exolve.voicedemo.core.telecom.TelecomContract.RegistrationEvent
import com.exolve.voicedemo.core.telecom.TelecomEvent
import com.exolve.voicedemo.core.uiCommons.BaseViewModel
import com.exolve.voicedemo.core.utils.CancelPermissionRequestCallback
import com.exolve.voicesdk.RegistrationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch


private const val ACCOUNT_VIEWMODEL = "AccountViewModel"

@Immutable
class AccountViewModel(application: Application) :
    BaseViewModel<AccountContract.Event, AccountContract.State, AccountContract.Effect>(application) {
    private val accountRepository: SettingsRepository = SettingsRepository(context = application)
    private var cancelPermissionRequestCallback: CancelPermissionRequestCallback = {}
    private var toast: Toast? = null

    init {
        Log.d(ACCOUNT_VIEWMODEL, "init: telecomManager = $telecomManager")

        viewModelScope.launch {
            launch(Dispatchers.IO) {
                telecomManager.telecomEvents.collect {
                    handleTelecomEvent(it)
                }
            }
            launch {
                telecomManager.telecomManagerState.collect {
                    if (uiState.value.token != it.token) {
                        Log.d(ACCOUNT_VIEWMODEL, "new token received: ${it.token}")
                        setState {
                            copy( token = it.token?: "" )
                        }
                    }
                }
            }
            launch(Dispatchers.IO) {
                Log.d(ACCOUNT_VIEWMODEL, "launch getAccountDetail")
                accountRepository.fetchAccountDetails()?.let {
                    setState {
                        copy(
                            number = it.number,
                            password = it.password,
                            registrationState = telecomManager.getRegistrationState()
                        )
                    }
                    Log.d(ACCOUNT_VIEWMODEL, "setState() loaded ")
                }
            }
        }
    }

    override fun initializeState(): AccountContract.State {
        Log.d(ACCOUNT_VIEWMODEL, "initializeState")
        return AccountContract.State(
            number = "",
            password = "",
            token = getApplication<CallApplication>().getString(R.string.default_token).lowercase(),
            registrationState = telecomManager.getRegistrationState(),
        )
    }

    override suspend fun handleTelecomEvent(event: TelecomEvent) {
        when (event) {
            is RegistrationEvent -> handleRegistrationEvent(event = event)
        }
    }

    private fun handleRegistrationEvent(event: RegistrationEvent) {
        Log.d(ACCOUNT_VIEWMODEL, "handleRegistrationEvent: $event")
        setState { copy(registrationState = telecomManager.getRegistrationState()) }
    }

    override fun handleUiEvent(event: AccountContract.Event) {
        when (event) {
            is AccountContract.Event.OnActivateClicked -> {
                toggleAccountActivation()
            }

            is AccountContract.FillableLoginField -> {
                updateTextFieldState(event, event.textState)
            }

            is AccountContract.Event.OnCopyButtonClicked -> {
                copyTokenToClipBoard()
            }

            else -> {}
        }
    }

    fun handleDeepLink(data: Uri?){
        data?.let {
            val args = (it.host + it.path).split("&", limit=2)
            if(args.size == 2){
                val number = args[0]
                val password = args[1]
                if(telecomManager.getRegistrationState() != RegistrationState.NOT_REGISTERED){
                    toggleAccountActivation()
                    viewModelScope.launch {
                        launch(Dispatchers.IO) {
                            telecomManager.telecomEvents.collect {
                                when (it) {
                                    is RegistrationEvent.OnNotRegistered -> {
                                        setState {
                                            copy(
                                                number = number,
                                                password = password
                                            )
                                        }
                                        toggleAccountActivation()
                                        this.coroutineContext.job.cancel()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    setState {
                        copy(
                            number = number,
                            password = password
                        )
                    }
                    toggleAccountActivation()
                }
            }
        }
    }


    private fun toggleAccountActivation() {
        if (telecomManager.getRegistrationState() == RegistrationState.NOT_REGISTERED) {
            Log.d(
                ACCOUNT_VIEWMODEL,
                "activateAccount: username = ${uiState.value.number}"
            )
            if (accountRepository.isDetectLocationEnabled()) {
                cancelPermissionRequestCallback = requestPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    onRequestedResult = {
                        telecomManager.activateAccount(
                            Account(
                                number = uiState.value.number, password = uiState.value.password,
                            ),
                        )
                    }
                )
            } else {
                telecomManager.activateAccount(
                    Account(
                        number = uiState.value.number, password = uiState.value.password,
                    ),
                )
            }

        } else {
            Log.d(ACCOUNT_VIEWMODEL, "deactivate }")
            telecomManager.unregisterAccount()
        }
    }


    private fun copyTokenToClipBoard() {
        (getApplication<CallApplication>()
            .applicationContext.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager)?.let {
            val clipData = ClipData.newPlainText("token", uiState.value.token)
            it.setPrimaryClip(clipData)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                toast?.cancel()
                toast = Toast.makeText(
                    getApplication(),
                    R.string.settings_token_copied,
                    Toast.LENGTH_LONG
                )
                toast?.show()
            }
        }
    }

    private fun updateTextFieldState(event: AccountContract.Event, newState: String) {
        if (event is AccountContract.FillableLoginField)
            when (event) {
                is AccountContract.Event.UserTexFieldChanged -> {
                    setState { copy(number = newState) }
                }

                is AccountContract.Event.PasswordTexFieldChanged -> {
                    setState { copy(password = newState) }
                }

                else -> {}
            }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.coroutineContext.cancel()
        cancelPermissionRequestCallback()
    }
}

