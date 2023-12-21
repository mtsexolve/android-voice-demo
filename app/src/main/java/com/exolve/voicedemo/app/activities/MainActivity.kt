package com.exolve.voicedemo.app.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i(MAIN_ACTIVITY, "permission granted")
        } else {
            Log.i(MAIN_ACTIVITY, "permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d(MAIN_ACTIVITY, "requesting notification permissions")
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
