package com.exolve.voicedemo.core.telecom

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.exolve.voicesdk.IRegistrationListener
import com.exolve.voicesdk.RegistrationError
import com.exolve.voicesdk.RegistrationState
import com.exolve.voicedemo.core.telecom.TelecomContract.RegistrationEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val REGISTRATION_LISTENER = "RegistrationListener"

class RegistrationListener(
    private val telecomManager: TelecomManager,
    private val context: Context
) : IRegistrationListener {

    override fun notRegistered() {
        handleRegistrationCallback(RegistrationEvent.OnNotRegistered())
    }
    override fun offline() {
        handleRegistrationCallback(RegistrationEvent.OnOffline())
    }
    override fun noConnection() {
        handleRegistrationCallback(RegistrationEvent.OnNoConnection())
    }
    override fun error(p0: RegistrationError, p1: String ) {
        handleRegistrationCallback(RegistrationEvent.OnError(p0, p1))
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(
                context,
                "Registration error: " + p1,
                Toast.LENGTH_SHORT)
                .show();
        }
    }
    override fun registered() {
        handleRegistrationCallback(RegistrationEvent.OnRegistered())
    }
    override fun registering() {
        handleRegistrationCallback(RegistrationEvent.OnRegistering())
    }
    private fun handleRegistrationCallback(
        registrationEvent: RegistrationEvent
    ) {
        Log.d(REGISTRATION_LISTENER, "handleRegistrationCallback: event = $registrationEvent")
        CoroutineScope(Dispatchers.Default).launch {
            telecomManager.setState { copy(registrationState = telecomManager.getRegistrationState()) }
            telecomManager.emitTelecomEvent(registrationEvent)
        }
    }
}


