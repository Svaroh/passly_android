package com.passbolt.mobile.android.feature.authentication.auth

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper
import com.passbolt.mobile.android.feature.authentication.auth.AuthState.RefreshAuthReason.PASSPHRASE
import com.passbolt.mobile.android.feature.authentication.auth.AuthState.RefreshAuthReason.SESSION

private const val SCREEN_WIDTH_DP = 360
private const val SCREEN_HEIGHT_DP = 800

private val accountData =
    AuthState.AccountData(
        label = "John Doe",
        email = "john@passbolt.com",
        domain = "https://passbolt.local",
        avatarUrl = null,
    )

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun AuthScreenScreenshot() {
    AuthScreen(
        state = AuthState(accountData = accountData),
        onIntent = {},
        snackbarHostState = SnackbarHostState(),
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun AuthScreenPassphraseExpiredScreenshot() {
    AuthScreen(
        state =
            AuthState(
                accountData = accountData,
                authReason = PASSPHRASE,
            ),
        onIntent = {},
        snackbarHostState = SnackbarHostState(),
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun AuthScreenSessionExpiredBiometricScreenshot() {
    AuthScreen(
        state =
            AuthState(
                accountData = accountData,
                authReason = SESSION,
                showBiometricButton = true,
            ),
        onIntent = {},
        snackbarHostState = SnackbarHostState(),
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun AuthScreenFilledPassphraseScreenshot() {
    AuthScreen(
        state =
            AuthState(
                accountData = accountData,
                passphrase = "mypassphrase",
                isAuthButtonEnabled = true,
            ),
        onIntent = {},
        snackbarHostState = SnackbarHostState(),
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun AuthScreenDarkThemeScreenshot() {
    AuthScreen(
        state = AuthState(accountData = accountData),
        onIntent = {},
        snackbarHostState = SnackbarHostState(),
    )
}
