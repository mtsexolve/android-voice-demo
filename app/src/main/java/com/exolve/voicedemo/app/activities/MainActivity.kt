package com.exolve.voicedemo.app.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.exolve.voicedemo.app.navigation.SetupAppNavigation
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import com.exolve.voicedemo.core.uiCommons.theme.AndroidVoiceExampleTheme
import com.exolve.voicedemo.features.bars.AppBottomNavigation
import com.exolve.voicedemo.features.bars.AppTopBar
import com.exolve.voicedemo.features.call.CallContract
import com.exolve.voicedemo.features.dialer.DialerContract
import com.exolve.voicedemo.features.dialer.DialerViewModel
import com.exolve.voicedemo.features.settings.SettingsContract
import com.exolve.voicedemo.features.settings.SettingsViewModel
import kotlinx.coroutines.launch

private const val MAIN_ACTIVITY = "MainActivity"

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val dialerViewModel: DialerViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
            var granted: Boolean = true
            for (permission in permissions) {
                granted = granted && permission.value
            }

            if (granted) {
                Log.i(MAIN_ACTIVITY, "permission granted")
            } else {
                Log.i(MAIN_ACTIVITY, "permission denied")
            }
    }
    private val getContactResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleContactResult(result)
        }

    private fun startActivityForGetContact() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        }
        Log.d(MAIN_ACTIVITY, "startActivity for get contact with intent $intent"
        )
        getContactResult.launch(intent)
    }

    private val contactsPermissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i(MAIN_ACTIVITY, "contacts permission granted")
            startActivityForGetContact()
        } else {
            Log.i(MAIN_ACTIVITY, "contacts permission denied")
        }
    }

    private fun handleContactResult(contactResult: ActivityResult) {
        if (contactResult.resultCode == RESULT_OK) {
            Log.d(MAIN_ACTIVITY, "handleContactResult: Result is OK")
            val cursor: Cursor? = contactResult.data?.data?.let { uri ->
                applicationContext.contentResolver.query(uri, null,null,null,null)
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
                Log.d(MAIN_ACTIVITY, "handleContactResult: invalid cursor"
                )
            }
        } else {
            Log.d(MAIN_ACTIVITY, "handleContactResult: Result is not OK")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher.launch(arrayOf(
            Manifest.permission.RECORD_AUDIO,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT
                else Manifest.permission.BLUETOOTH
        ))
        askNotificationPermission()
        initEventListeners(settingsViewModel, dialerViewModel)
        setContent {
            AndroidVoiceExampleTheme {
                Surface { MainAppScreen(settingsViewModel, dialerViewModel) }
            }
        }
    }

    private fun initEventListeners(
        settingsViewModel: SettingsViewModel,
        dialerViewModel: DialerViewModel
    ) {
        lifecycleScope.launch {
            settingsViewModel.event.collect {
                handleUiEvent(it)
            }
        }
        lifecycleScope.launch {
            dialerViewModel.event.collect {
                handleUiEvent(it)
            }
        }
    }

    private fun handleUiEvent(event: UiEvent) {
        Log.d(MAIN_ACTIVITY, "handleUiEvent: $event")
        when (event) {
            is SettingsContract.Event.OnBackToCallActivityClicked -> backToCallActivity()
            is DialerContract.Event.OnBackToCallActivityClicked -> backToCallActivity()
            is DialerContract.Event.OnContactsButtonClicked -> {
                contactsPermissionRequestLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun backToCallActivity() {
        this.startActivity(
            Intent(this, CallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(MAIN_ACTIVITY, "notification permissions granted")
            } else if (shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                //TODO: show and educational UI
                Log.w(MAIN_ACTIVITY, "permission denied, should show rationale")
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            } else {
                Log.d(MAIN_ACTIVITY, "requesting notification permissions")
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }


}

@Composable
fun MainAppScreen(
    settingsViewModel: ViewModel,
    dialerViewModel: ViewModel
) {
    val navController = rememberNavController()
    Scaffold(
        modifier = remember { Modifier },
        bottomBar = { AppBottomNavigation(navController = navController) },
        topBar = { AppTopBar(settingsViewModel as SettingsViewModel, remember { Modifier }) }

    ) {
            paddingValues ->
        SetupAppNavigation(
            navController = navController,
            settingsViewModel as SettingsViewModel,
            dialerViewModel as DialerViewModel,
            paddingValues
        )
    }
}
