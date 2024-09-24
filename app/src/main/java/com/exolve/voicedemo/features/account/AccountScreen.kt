package com.exolve.voicedemo.features.account

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.exolve.voicedemo.R
import com.exolve.voicesdk.RegistrationState

private const val ACCOUNT_SCREEN = "AccountScreen"

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onEvent: (event: AccountContract.Event) -> Unit,
    barPaddingValues: PaddingValues
) {
    val state by viewModel.uiState.collectAsState()
    Log.d(ACCOUNT_SCREEN, "AccountScreen: updated state is $state")
    AccountContent(
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
        registrationState = state.registrationState
    )
}

@Composable
fun AccountContent(
    onEvent: (event: AccountContract.Event) -> Unit,
    number: String,
    password: String,
    token: String,
    registrationState: RegistrationState,
    modifier: Modifier,
) {
    val focusManager = LocalFocusManager.current
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                    }
                }
        ) {
            AccountView(
                onEvent = onEvent,
                number = number,
                password = password,
                token = token,
                registrationState = registrationState
            )

        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AccountView(
    onEvent: (event: AccountContract.Event) -> Unit,
    number: String,
    password: String,
    token: String,
    registrationState: RegistrationState
) {
    val customTextSelectionColor = TextSelectionColors(
        handleColor = colorResource(id = R.color.mts_red),
        backgroundColor = colorResource(id = R.color.mts_grey)
    )


    ConstraintLayout(
        Modifier
            .fillMaxWidth()
    ) {
        val (
            numberEditText,
            numberHeader,
            passwordEditText,
            passwordHeader,
            activateButton,
            copyTokenButton,
            tokenEditText,
            tokenHeader,
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
                onValueChange = { onEvent(AccountContract.Event.UserTexFieldChanged(it)) },
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
        CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColor) {
            OutlinedTextField(
                value = password,
                onValueChange = { onEvent(AccountContract.Event.PasswordTexFieldChanged(it)) },
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
                onEvent(AccountContract.Event.OnActivateClicked)
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
                text = if (registrationState == RegistrationState.NOT_REGISTERED) stringResource(id = R.string.activate_button) else stringResource(
                    id = R.string.deactivate_button
                ),
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
                .fillMaxWidth(),
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
                onEvent(AccountContract.Event.OnCopyButtonClicked)
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
    }
}

