package com.exolve.voicedemo.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.exolve.voicedemo.features.call.*

@Composable
fun SetupCallNavigation(
    navController: NavHostController,
    ongoingCallViewModel: CallViewModel
) {
    val controlDestination = stringResource(id = OngoingCallNavigationDestinations.Control.screenRouteStringId)
    NavHost(
        navController = navController,
        startDestination = controlDestination
    ) {
        composable(controlDestination) {
            ControlOngoingCallScreenDestination(
                ongoingCallViewModel = ongoingCallViewModel
            )
        }
    }
}