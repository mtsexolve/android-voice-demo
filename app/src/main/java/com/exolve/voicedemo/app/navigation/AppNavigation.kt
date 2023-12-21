package com.exolve.voicedemo.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.exolve.voicedemo.features.dialer.DialerScreenDestination
import com.exolve.voicedemo.features.dialer.DialerViewModel
import com.exolve.voicedemo.features.settings.SettingsScreenDestination
import com.exolve.voicedemo.features.settings.SettingsViewModel

@Composable
fun SetupAppNavigation(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    dialerViewModel: DialerViewModel,
    paddingValues: PaddingValues
) {
    val dialerRout = stringResource(id = BottomNavigationDestinations.Dialer.screenRouteStringId)
    val settingsRout = stringResource(id = BottomNavigationDestinations.Settings.screenRouteStringId)
    NavHost(
        navController = navController,
        startDestination = dialerRout,
    ) {
        composable(dialerRout) {
            DialerScreenDestination(dialerViewModel, paddingValues)
        }
        composable(settingsRout) {
            SettingsScreenDestination(settingsViewModel, paddingValues)
        }
    }
}