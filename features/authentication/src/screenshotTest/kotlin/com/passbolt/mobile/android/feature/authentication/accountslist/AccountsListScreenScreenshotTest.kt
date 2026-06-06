package net.svaroh.passly.feature.authentication.accountslist

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper
import net.svaroh.passly.ui.AccountModelUi.AccountModel
import net.svaroh.passly.ui.AccountModelUi.AddNewAccount

private const val SCREEN_WIDTH_DP = 360
private const val SCREEN_HEIGHT_DP = 800

private val adaAccount =
    AccountModel(
        userId = "1",
        title = "Ada Lovelace",
        email = "ada@passbolt.com",
        avatar = null,
        url = "https://passbolt.com",
    )

private val bettyAccount =
    AccountModel(
        userId = "2",
        title = "Betty Holberton",
        email = "betty@passbolt.com",
        avatar = null,
        url = "https://passbolt.com",
    )

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun AccountsListScreenScreenshot() {
    AccountsListScreen(
        state =
            AccountsListState(
                showManageAccountsTopBar = false,
                showHeader = true,
                accounts = listOf(adaAccount, bettyAccount, AddNewAccount),
                currentUserId = "1",
            ),
        onIntent = {},
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun AccountsListScreenManageModeScreenshot() {
    AccountsListScreen(
        state =
            AccountsListState(
                showManageAccountsTopBar = true,
                showHeader = false,
                accounts = listOf(adaAccount, bettyAccount, AddNewAccount),
                isRemoveMode = false,
            ),
        onIntent = {},
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun AccountsListScreenRemoveModeScreenshot() {
    AccountsListScreen(
        state =
            AccountsListState(
                showManageAccountsTopBar = true,
                showHeader = false,
                accounts = listOf(adaAccount, bettyAccount, AddNewAccount),
                isRemoveMode = true,
            ),
        onIntent = {},
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun AccountsListScreenDarkThemeScreenshot() {
    AccountsListScreen(
        state =
            AccountsListState(
                showManageAccountsTopBar = false,
                showHeader = true,
                accounts = listOf(adaAccount, bettyAccount, AddNewAccount),
                currentUserId = "1",
            ),
        onIntent = {},
    )
}
