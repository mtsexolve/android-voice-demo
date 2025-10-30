package com.exolve.voicedemo.features.dialer

import android.Manifest
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.exolve.voicedemo.R
import com.exolve.voicedemo.core.repositories.SettingsRepository
import com.exolve.voicedemo.core.telecom.TelecomEvent
import com.exolve.voicedemo.core.uiCommons.BaseViewModel
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import com.exolve.voicedemo.core.telecom.TelecomContract.CallEvent
import com.exolve.voicedemo.core.utils.CancelPermissionRequestCallback
import com.exolve.voicesdk.RegistrationMode
import com.exolve.voicesdk.RegistrationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.annotation.concurrent.Immutable
private const val DIALER_VIEW_MODEL = "DialerViewModel"

@Immutable
class DialerViewModel(application: Application) :
    BaseViewModel<UiEvent, DialerContract.State, DialerContract.Effect>(application) {

    private val settingsRepository: SettingsRepository = SettingsRepository(context = application)
    private var dialerToast = Toast.makeText(getApplication(), R.string.dialer_toast_activate, Toast.LENGTH_LONG)
    private var cancelPermissionRequestCallback: CancelPermissionRequestCallback = {}
    init {
        viewModelScope.launch(Dispatchers.IO) {
            telecomManager.telecomEvents.collect {telecomEvent ->
                Log.d(DIALER_VIEW_MODEL, "DialerViewModel: collect telecom events event is CallEvent $telecomEvent")
                (telecomEvent as? CallEvent)?.let { handleTelecomEvent(telecomEvent) }
            }
        }
    }

    override fun initializeState(): DialerContract.State {
        return DialerContract.State(
            dialerText = "",
            hasCurrentCall = telecomManager.getCalls().isNotEmpty()
        )
    }

    override fun handleUiEvent(event: UiEvent) {
        when(event) {
            is DialerContract.Event.OnRemoveButtonClicked -> { removeDigits(onLongClicked = event.longClicked) }
            is DialerContract.Event.OnCallButtonClicked -> { call() }
            is DialerContract.Event.OnCallButtonLongPressed -> { restoreCallNumber() }
            is DialerContract.Event.OnDigitButtonClicked -> {updateTextFieldState(event.index)}
            is DialerContract.Event.OnSetCallingNumber -> {setTextFieldState(event.value)}
        }
    }

    private fun updateTextFieldState(value: String) {
        if (uiState.value.dialerText.length < 32) {
            setState { copy(dialerText = dialerText + value) }
        }
    }

    private fun setTextFieldState(value: String) {
        setState { copy(dialerText = value) }
    }

    private fun call() {
        val placeCall: (String) -> Unit  = {
            try {
                telecomManager.call(it)
            } catch (e: IllegalArgumentException) {
                dialerToast?.cancel()
                dialerToast = Toast.makeText(getApplication(), e.message, Toast.LENGTH_LONG)
                dialerToast.show()
            }
        }
        settingsRepository.setLastCallNumber(uiState.value.dialerText)
        if (settingsRepository.isDetectLocationEnabled()) {
            cancelPermissionRequestCallback = requestPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                onRequestedResult = { placeCall(uiState.value.dialerText) }
            )
        } else {
            placeCall(uiState.value.dialerText)
        }
    }

    private fun removeDigits(onLongClicked: Boolean) {
        if (onLongClicked) setState { copy(dialerText = "") }
        else setState { copy(dialerText = dialerText.dropLast(1)) }
    }

    private fun restoreCallNumber() {
        settingsRepository.getLastCallNumber().let {
            setState { copy( dialerText = it ) }
        }
    }

    override suspend fun handleTelecomEvent(event: TelecomEvent) {
        Log.d(DIALER_VIEW_MODEL, "DialerViewModel: telecomEvent: event is $event")
        when (event) {
            is CallEvent.OnNewCall -> setState { copy(hasCurrentCall = true) }
            is CallEvent.OnCallDisconnected -> {
                if (telecomManager.getCalls().isEmpty()) {
                    setState { copy(hasCurrentCall = false) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.coroutineContext.cancel()
        cancelPermissionRequestCallback()
    }

}
