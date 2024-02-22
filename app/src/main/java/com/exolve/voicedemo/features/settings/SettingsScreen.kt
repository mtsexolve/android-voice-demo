package com.exolve.voicedemo.features.settings

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.AnnotatedString

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.exolve.voicedemo.R
import com.exolve.voicedemo.core.utils.SharingProvider
import com.exolve.voicesdk.RegistrationState

private const val SETTINGS_SCREEN = "SettingsScreen"

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onEvent: (event: SettingsContract.Event) -> Unit,
    barPaddingValues: PaddingValues
) {
    val state by viewModel.uiState.collectAsState()
    Log.d(SETTINGS_SCREEN, "SettingsScreen: updated state is $state")
    SettingsContent(
        onEvent = onEvent,
        modifier = Modifier.padding(
            top = barPaddingValues.calculateTopPadding(),
            bottom = barPaddingValues.calculateBottomPadding(),
            start = 20.dp,
            end = 20.dp,
        ),
        number = state.number,
        password = state.password,
        token = state.token,
        versionDescription = state.versionDescription,
        registrationState = state.registrationState,
        voipBackgroundRunning = state.voipBackgroundRunning,
    )
}

fun getBundleId(context: Context): String {
    return context.packageName
}

@Composable
fun SettingsContent(
    onEvent: (event: SettingsContract.Event) -> Unit,
    number: String,
    password: String,
    token: String,
    versionDescription: String,
    registrationState: RegistrationState,
    voipBackgroundRunning: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val bundleId = getBundleId(context)
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            AccountView(
                    onEvent = onEvent,
                    number = number,
                    password = password,
                    token = token,
                    registrationState = registrationState,
                    voipBackgroundRunning = voipBackgroundRunning,
                )

            }
        val isPressed = remember { mutableStateOf(false) }

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${bundleId}, ${versionDescription}",
                style = TextStyle(
                    color =  colorResource(id = R.color.mts_text_grey),
                    fontFamily = FontFamily(Font(R.font.mtscompact_regular))
                ),
                modifier = Modifier
                    .padding(bottom = 4.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Absolute.Right) {
                OutlinedButton(
                    onClick = { SharingProvider(context.applicationContext).share("Logs") },
                    modifier = Modifier
                        .padding(bottom = 4.dp),
                    colors = ButtonDefaults.buttonColors(colorResource(id = R.color.white)),
                    border = BorderStroke(1.dp, colorResource(id = R.color.white)),
                    contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp),
                ) {
                    Text(
                        text = AnnotatedString(stringResource(id = R.string.settings_logs_button)),
                        style = TextStyle(
                            textDecoration = TextDecoration.Underline,
                            color = if (isPressed.value) colorResource(id = R.color.black)
                            else colorResource(id = R.color.mts_text_grey),
                            fontFamily = FontFamily(Font(R.font.mtscompact_regular))
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AccountView(
    onEvent: (event: SettingsContract.Event) -> Unit,
    number: String,
    password: String,
    token: String,
    registrationState: RegistrationState,
    voipBackgroundRunning: Boolean,
) {
    val customTextSelectionColor = TextSelectionColors(
        handleColor = colorResource(id = R.color.mts_red),
        backgroundColor = colorResource(id =R.color.mts_grey)
    )


    ConstraintLayout(
        Modifier
            .fillMaxWidth()
    ) {
        val (numberEditText,
            numberHeader,
            passwordEditText,
            passwordHeader,
            activateButton,
            copyTokenButton,
            tokenEditText,
            tokenHeader,
            bgModeSwitchLayout,
        ) = createRefs()

        Text(
            modifier = Modifier.constrainAs(numberHeader) {
                top.linkTo(parent.top, margin = 24.dp)
                start.linkTo(parent.start)
            },
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                fontSize = 14.sp,
                color = colorResource(
                    id = R.color.mts_text_grey
                )
            ),
            text = stringResource(id = R.string.settings_number)
        )
        CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColor) {

            OutlinedTextField(
                value = number,
                onValueChange = { onEvent(SettingsContract.Event.UserTexFieldChanged(it)) },
                placeholder = { Text(stringResource(id = R.string.settings_hint_number)) },
                modifier = Modifier
                    .semantics { testTagsAsResourceId = true }
                    .testTag("edit_text_settings_number")
                    .constrainAs(numberEditText) {
                        top.linkTo(numberHeader.bottom, margin = 8.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = colorResource(id = R.color.mts_grey),
                    unfocusedBorderColor = colorResource(id = R.color.mts_grey),
                    focusedLabelColor = colorResource(id = R.color.mts_grey),
                    cursorColor = colorResource(id = R.color.black),
                    backgroundColor = colorResource(id = R.color.mts_bg_grey),
                ),
                shape = RoundedCornerShape(8.dp),
                maxLines = 1,

                )
        }
        // password
        Text(
            modifier = Modifier.constrainAs(passwordHeader) {
                top.linkTo(numberEditText.bottom, margin = 24.dp)
                start.linkTo(parent.start)
            },
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                fontSize = 14.sp,
                color = colorResource(
                    id = R.color.mts_text_grey
                )
            ),
            text = stringResource(id = R.string.settings_password)
        )
        CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColor){
            OutlinedTextField(
                value = password,
                onValueChange = { onEvent(SettingsContract.Event.PasswordTexFieldChanged(it)) },
                placeholder = { Text(stringResource(id = R.string.settings_hint_password)) },
                modifier = Modifier
                    .semantics { testTagsAsResourceId = true }
                    .testTag("edit_text_settings_password")
                    .constrainAs(passwordEditText) {
                        top.linkTo(passwordHeader.bottom, margin = 8.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = colorResource(id = R.color.mts_grey),
                    unfocusedBorderColor = colorResource(id = R.color.mts_grey),
                    focusedLabelColor = colorResource(id = R.color.mts_grey),
                    cursorColor = colorResource(id = R.color.black),
                    backgroundColor = colorResource(id = R.color.mts_bg_grey),
                ),
                shape = RoundedCornerShape(8.dp),
            )
        }

        Button(
            onClick = {
                onEvent(SettingsContract.Event.OnActivateClicked)
            },
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_settings_activate")
                .constrainAs(activateButton) {
                    top.linkTo(passwordEditText.bottom, margin = 24.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_red)),
        ) {
            Text(
                text =  if(registrationState == RegistrationState.NOT_REGISTERED) stringResource(id = R.string.activate_button) else stringResource(id = R.string.deactivate_button),
                fontSize = 17.sp,
                style = TextStyle(
                    color = colorResource(id = R.color.white),
                    fontFamily = FontFamily(Font(R.font.mtscompact_bold)),
                ),
                modifier = Modifier.padding(vertical = 14.dp)
            )
        }

        // Token
        Text(
            modifier = Modifier.constrainAs(tokenHeader) {
                top.linkTo(activateButton.bottom, margin = 24.dp)
                start.linkTo(parent.start)
            },
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                fontSize = 14.sp,
                color = colorResource(
                    id = R.color.mts_text_grey
                )
            ),
            text = stringResource(id = R.string.settings_token)
        )
        OutlinedTextField(
            value = token,
            onValueChange = { },
            placeholder = { },
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("edit_text_settings_token")
                .constrainAs(tokenEditText) {
                    top.linkTo(tokenHeader.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .fillMaxWidth()
            ,
            maxLines = 1,
            enabled = false,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = colorResource(id = R.color.mts_grey),
                unfocusedBorderColor = colorResource(id = R.color.mts_grey),
                focusedLabelColor = colorResource(id = R.color.mts_grey),
                cursorColor = colorResource(id = R.color.black),
                backgroundColor = colorResource(id = R.color.mts_bg_grey),
            ),
            shape = RoundedCornerShape(8.dp)

        )

        Button(
            onClick = {
                onEvent(SettingsContract.Event.OnCopyButtonClicked)
            },
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_settings_copy_token_button")
                .constrainAs(copyTokenButton) {
                    top.linkTo(tokenEditText.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_grey)),
        ) {
            Text(
                text = stringResource(id = R.string.settings_button_copy),
                fontSize = 17.sp,
                style = TextStyle(
                    color = colorResource(id = R.color.white),
                    fontFamily = FontFamily(Font(R.font.mtscompact_bold)),
                ),
                modifier = Modifier.padding(vertical = 14.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.constrainAs(bgModeSwitchLayout) {
                top.linkTo(copyTokenButton.bottom, margin = 8.dp)
                start.linkTo(parent.start)
            }

        ) {
            Text(
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                    fontSize = 14.sp,
                    color = colorResource(
                        id = R.color.mts_text_grey
                    )
                ),
                text = stringResource(id = R.string.background_running)
            )

            Switch(
                modifier = Modifier
                    .semantics { testTagsAsResourceId = true }
                    .testTag("button_settings_bg_mode_switch"),
                checked = voipBackgroundRunning,
                onCheckedChange = {
                    onEvent(SettingsContract.Event.OnBackgroundRunningChanged(it))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorResource(id = R.color.mts_red),
                )
            )
        }

    }
}

