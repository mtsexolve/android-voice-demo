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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.exolve.voicedemo.R
import com.exolve.voicedemo.core.telecom.TelecomContract
import com.exolve.voicedemo.core.telecom.TelecomManager
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import com.exolve.voicedemo.core.uiCommons.theme.AndroidVoiceExampleTheme
import com.exolve.voicedemo.features.call.CallContract
import com.exolve.voicedemo.features.call.CallViewModel
import com.exolve.voicedemo.features.call.OngoingCallScreen
import com.exolve.voicedemo.features.dialer.DialerContract
import com.exolve.voicedemo.features.dialer.DialerViewModel
import com.exolve.voicedemo.core.permissions.RequestPermissionsResult
import com.exolve.voicesdk.NotificationIntents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private const val CALL_ACTIVITY = "CallActivity"

class CallActivity : BaseActivity() {
    private lateinit var telecomManager: TelecomManager

    private val viewModel: CallViewModel by viewModels()

    private val dialerViewModel: DialerViewModel by viewModels()

    private var callIntentFlow : MutableStateFlow<String> = MutableStateFlow("")
    private val callIntentStateFlow = callIntentFlow.asStateFlow()

    private val getContactResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleContactResult(result)
        }

    private fun processCallIntent(intent: Intent) {
        intent.getStringExtra(NotificationIntents.EXTRA_CALL_ID)?.let {
            when(intent.action) {
                NotificationIntents.ACTION_ANSWER_CALL -> {
                    viewModel.setEvent(CallContract.Event.OnAcceptCallButtonClicked(it))
                    callIntentFlow.value = "Opened with accept notification button"
                }
                NotificationIntents.ACTION_RESUME_CALL -> {
                    viewModel.setEvent(CallContract.Event.OnResumeButtonClicked(it))
                    callIntentFlow.value = "Opened with resume notification button"
                }
                NotificationIntents.ACTION_FULLSCREEN_CALL_ACTIVITY -> {
                    callIntentFlow.value = "Opened with notification fullscreen"
                    Log.i(CALL_ACTIVITY, "Call notification fullscreen $it")
                }
                NotificationIntents.ACTION_CLICKED_CALL_NOTIFICATION -> {
                    callIntentFlow.value = "Open with notification clicked"
                    Log.i(CALL_ACTIVITY, "Call notification clicked $it")
                }
                else -> {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        telecomManager.setCallActivityVisible(true)
        lifecycleScope.launch {
            delay(3.seconds)
            callIntentFlow.value = ""
        }
    }

    override fun onPause() {
        super.onPause()
        telecomManager.setCallActivityVisible(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindViewModelForPermissions(viewModel)

        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        telecomManager = TelecomManager.getInstance()
        if (telecomManager.getCalls().isEmpty()) {
            finish()
            return
        }
        configRequiredPermissions()
        processCallIntent(intent)
        lifecycleScope.launch {
            viewModel.event.collect {
                handleUiEvent(it)
            }
        }
        lifecycleScope.launch {
            dialerViewModel.event.collect {
                handleUiEvent(it)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            telecomManager.telecomEvents.collect { telecomEvent ->
                if(telecomEvent is TelecomContract.CallEvent.OnCallDisconnected ||
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
                    modifier = Modifier
                        .safeDrawingPadding()
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val controlDestination = stringResource(id = R.string.navigation_ongoing_call_control)
                    NavHost(
                        navController = navController,
                        startDestination = controlDestination
                    ) {
                        composable(controlDestination) {
                            OngoingCallScreen(
                                ongoingCallViewModel = viewModel,
                                dialerViewModel = dialerViewModel,
                                onEvent = viewModel::setEvent,
                                onDialerEvent = dialerViewModel::setEvent
                            )

                            ConstraintLayout(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val (textView) = createRefs()
                                Text(
                                    text = callIntentStateFlow.collectAsState().value,
                                    modifier = Modifier
                                        .padding(bottom = 5.dp)
                                        .constrainAs(textView) {
                                            bottom.linkTo(parent.bottom)
                                            start.linkTo(parent.start)
                                            end.linkTo(parent.end)
                                        },
                                    style = TextStyle(
                                        fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                                        fontSize = 14.sp,
                                        color = colorResource(
                                            id = R.color.black
                                        )
                                    ),
                                )
                            }
                        }
                    }
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
            is CallContract.Event.OnNewCallButtonClicked -> {
                finish()
            }
            is DialerContract.Event.OnContactsButtonClicked -> {
                requestPermissions(
                    listOf(Manifest.permission.READ_CONTACTS),
                    { result ->
                        when (result) {
                            RequestPermissionsResult.GRANTED_ALL -> {
                                Log.i(CALL_ACTIVITY, "contacts permission granted")
                                startActivityForGetContact()
                            }
                            RequestPermissionsResult.DENIED_ALL -> {
                                Log.i(CALL_ACTIVITY, "contacts permission denied")
                            }
                            else -> {}
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(CALL_ACTIVITY, "onNewIntent")
        processCallIntent(intent)
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
                dialerViewModel.setState {
                    copy(dialerText = number)
                }
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
