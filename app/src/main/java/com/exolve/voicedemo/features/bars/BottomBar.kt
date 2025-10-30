package com.exolve.voicedemo.features.bars

import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.exolve.voicedemo.R
import com.exolve.voicedemo.app.navigation.BottomNavigationDestinations
import com.exolve.voicedemo.core.utils.Utils
import okhttp3.internal.immutableListOf

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppBottomNavigation(navController: NavController) {
    val destinations = remember {
        immutableListOf(
            BottomNavigationDestinations.Dialer,
            BottomNavigationDestinations.Account,
            BottomNavigationDestinations.Settings,
        )
    }
    BottomNavigation(
        backgroundColor = Color.White
    ) {
        val navigationBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navigationBackStackEntry?.destination?.route
        destinations.forEach { destination ->
            val desiredDestination = stringResource(id = destination.screenRouteStringId)
            BottomNavigationItem(
                modifier = remember {
                    Modifier
                        .semantics { testTagsAsResourceId = true }
                        .testTag("button_bar_${desiredDestination.lowercase()}")
                },
                icon = {
                    Icon(
                        painterResource(id = destination.icon),
                        contentDescription = stringResource(id = destination.titleStringId),
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = destination.titleStringId),
                        fontSize = 12.sp,
                        style = TextStyle(fontFamily = FontFamily(Font(R.font.mtscompact_medium)))
                    )
                },
                selectedContentColor = colorResource(id = R.color.mts_red),
                unselectedContentColor = colorResource(id = R.color.mts_grey),
                alwaysShowLabel = true,
                selected = currentRoute == desiredDestination,
                onClick = {
                    Utils.navigate(desiredDestination)
                }
            )
        }
    }
}