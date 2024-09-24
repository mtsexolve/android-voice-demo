package com.exolve.voicedemo.features.bars

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.exolve.voicedemo.R
import com.exolve.voicedemo.features.account.AccountViewModel
import com.exolve.voicesdk.RegistrationState

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppTopBar(viewModel: AccountViewModel, modifier: Modifier) {
    val state by viewModel.uiState.collectAsState()
    TopAppBar(
        backgroundColor = colorResource(id = R.color.white),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier)

            if (state.registrationState == RegistrationState.REGISTERING) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp),
                    color = Color.Black,
                    strokeWidth = 1.7.dp
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_status),
                    contentDescription = stringResource(id = R.string.reg_status),
                    modifier = Modifier.size(20.dp),
                    tint = Color.Black,
                )
            }
            Text(
                modifier = Modifier
                    .semantics { testTagsAsResourceId = true }
                    .testTag("text_view_top_bar_status"),
                text = "${stringResource(id = R.string.reg_status)}  ${stringResource(state.registrationState.stringResourceId)} ",
                color = Color.Black,
                style = TextStyle(fontFamily = FontFamily(Font(R.font.mtscompact_medium)))

            )
        }
    }
}
