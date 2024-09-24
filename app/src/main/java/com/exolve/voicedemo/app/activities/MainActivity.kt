package com.exolve.voicedemo.app.activities

import android.Manifest
import android.content.Intent
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.exolve.voicedemo.app.navigation.BottomNavigationDestinations
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import com.exolve.voicedemo.core.uiCommons.theme.AndroidVoiceExampleTheme
import com.exolve.voicedemo.features.account.AccountScreen
import com.exolve.voicedemo.features.account.AccountViewModel
import com.exolve.voicedemo.features.bars.AppBottomNavigation
import com.exolve.voicedemo.features.bars.AppTopBar
import com.exolve.voicedemo.features.dialer.DialerContract
import com.exolve.voicedemo.features.dialer.DialerScreen
import com.exolve.voicedemo.features.dialer.DialerViewModel
import com.exolve.voicedemo.features.settings.*
import kotlinx.coroutines.launch

private const val MAIN_ACTIVITY = "MainActivity"

class MainActivity : ComponentActivity() {
    private val dialerViewModel: DialerViewModel by viewModels()
    private val accountViewModel: AccountViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var granted = true
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
        Log.d(
            MAIN_ACTIVITY, "startActivity for get contact with intent $intent"
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
                applicationContext.contentResolver.query(uri, null, null, null, null)
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
                    MAIN_ACTIVITY, "handleContactResult: invalid cursor"
                )
            }
        } else {
            Log.d(MAIN_ACTIVITY, "handleContactResult: Result is not OK")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
        initEventListeners(settingsViewModel, dialerViewModel)
        handleDeepLink(intent)
        setContent {
            AndroidVoiceExampleTheme {
                Surface { MainAppScreen(dialerViewModel, accountViewModel, settingsViewModel) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        accountViewModel.handleDeepLink(intent.data)
    }

    private fun requestPermissions() {
        var permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO,
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                Manifest.permission.BLUETOOTH_CONNECT
            else
                Manifest.permission.BLUETOOTH)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        requestPermissionLauncher.launch(permissions)
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
}

@Composable
fun MainAppScreen(
    dialerViewModel: ViewModel,
    accountViewModel: ViewModel,
    settingsViewModel: ViewModel
) {
    val navController = rememberNavController()
    Scaffold(
        modifier = remember { Modifier },
        bottomBar = { AppBottomNavigation(navController = navController) },
        topBar = { AppTopBar(accountViewModel as AccountViewModel, remember { Modifier }) }

    ) { paddingValues ->
        val dialerRoute = stringResource(id = BottomNavigationDestinations.Dialer.screenRouteStringId)
        val accountRoute = stringResource(id = BottomNavigationDestinations.Account.screenRouteStringId)
        val settingsRoute = stringResource(id = BottomNavigationDestinations.Settings.screenRouteStringId)
        NavHost(
            navController = navController,
            startDestination = dialerRoute,
        ) {
            composable(dialerRoute) {
                DialerScreen(
                    viewModel = dialerViewModel as DialerViewModel,
                    onEvent  = dialerViewModel::setEvent,
                    barPaddingValues = paddingValues
                )
            }
            composable(accountRoute) {
                AccountScreen(
                    viewModel = accountViewModel as AccountViewModel,
                    onEvent = accountViewModel::setEvent,
                    barPaddingValues = paddingValues,
                )
            }
            composable(settingsRoute) {
                SettingsScreen(
                    viewModel = settingsViewModel as SettingsViewModel,
                    onEvent = settingsViewModel::setEvent,
                    barPaddingValues = paddingValues,
                )
            }
        }
    }
}
