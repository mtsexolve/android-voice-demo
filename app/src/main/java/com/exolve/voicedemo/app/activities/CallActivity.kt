package com.exolve.voicedemo.app.activities

import android.Manifest
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.exolve.voicedemo.app.navigation.SetupCallNavigation
import com.exolve.voicedemo.core.telecom.PushProvider
import com.exolve.voicedemo.core.telecom.TelecomContract
import com.exolve.voicedemo.core.telecom.TelecomManager
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import com.exolve.voicedemo.core.uiCommons.theme.AndroidVoiceExampleTheme
import com.exolve.voicedemo.features.call.CallContract
import com.exolve.voicedemo.features.call.CallViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private const val CALL_ACTIVITY = "CallActivity"

class CallActivity : ComponentActivity() {
    private lateinit var telecomManager: TelecomManager

    private val viewModel: CallViewModel by viewModels()

    private val getContactResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleContactResult(result)
        }

    private val contactsPermissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i(CALL_ACTIVITY, "contacts permission granted")
            startActivityForGetContact()
        } else {
            Log.i(CALL_ACTIVITY, "contacts permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        telecomManager = TelecomManager.getInstance()
        if (telecomManager.getCalls().isEmpty()) {
            finish()
            return
        }
        configRequiredPermissions()
        intent?.let {
            Log.d(CALL_ACTIVITY, "Start with call accepting, intent = ${intent.data} action ${intent.action}")
            //Handle Android 12 (API 31) trampoline restrictions
            // https://developer.android.com/about/versions/12/behavior-changes-12#notification-trampolines
            PushProvider.broadcastCallIntent(this, intent)
        }
        lifecycleScope.launch {
            viewModel.event.collect {
                handleUiEvent(it)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            telecomManager.telecomEvents.collect { telecomEvent ->
                if(telecomEvent is TelecomContract.CallEvent.OnCallTerminated ||
                    telecomEvent is TelecomContract.CallEvent.OnCallError) {
                    if(telecomManager.getCalls().isEmpty()) {
                        delay(1.seconds)
                        finish()
                    }
                }
            }
        }
        setContent {
            AndroidVoiceExampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    OngoingCallScreen(ongoingCallViewModel = viewModel)
                }
            }
        }
    }

    private fun configRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            this.window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun handleUiEvent(event: UiEvent) {
        Log.d(CALL_ACTIVITY, "handleUiEvent: $event")
        when (event) {
            is CallContract.Event.OnTransferButtonClicked -> {
                contactsPermissionRequestLauncher.launch(Manifest.permission.READ_CONTACTS)
            }

            is CallContract.Event.OnNewCallButtonClicked -> {
                finish()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(CALL_ACTIVITY, "onNewIntent")

        //Handle Android 12 (API 31) trampoline restrictions
        // https://developer.android.com/about/versions/12/behavior-changes-12#notification-trampolines
        intent?.let { PushProvider.broadcastCallIntent(this, intent) }
    }

    private fun startActivityForGetContact() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        }
        Log.d(
            CALL_ACTIVITY,
            "startActivity for get contact with intent = $intent"
        )
        getContactResult.launch(intent)
    }

    private fun handleContactResult(contactResult: ActivityResult) {
        if (contactResult.resultCode == RESULT_OK) {
            Log.d(CALL_ACTIVITY, "handleContactResult: Result is OK")
            val cursor: Cursor? = contactResult.data?.data?.let { uri ->
                applicationContext.contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    null
                )
            }
            if (cursor != null && cursor.moveToFirst()) {
                val numberColumnIndex: Int =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val number: String = cursor.getString(numberColumnIndex)
                cursor.close()
                // Show results
                viewModel.setEvent(CallContract.Event.OnTransferNumberSelected(number))
            } else {
                Log.d(
                    CALL_ACTIVITY,
                    "handleContactResult: invalid cursor"
                )
            }
        } else {
            Log.d(CALL_ACTIVITY, "handleContactResult: Result is not OK")
        }
    }
}

@Composable
fun OngoingCallScreen(
    ongoingCallViewModel: ViewModel
) {
    val navController = rememberNavController()
    if (ongoingCallViewModel is CallViewModel) {
        SetupCallNavigation(
            navController = navController,
            ongoingCallViewModel = ongoingCallViewModel
        )
    }
}
