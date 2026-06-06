package net.svaroh.passly.feature.resourceform.main

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper
import net.svaroh.passly.ui.LeadingContentType
import net.svaroh.passly.ui.PasswordStrength
import net.svaroh.passly.ui.ResourceFormMode.Create
import net.svaroh.passly.ui.ResourceFormMode.Edit
import net.svaroh.passly.ui.ResourceFormUiModel

private const val SCREEN_WIDTH_DP = 360
private const val SCREEN_HEIGHT_DP = 800
private const val TALL_SCREEN_HEIGHT_DP = 1400

private const val PASSWORD_ENTROPY_BITS = 60.0

private val passwordData =
    PasswordData(
        password = "p@ssb0lt!",
        passwordStrength = PasswordStrength.Strong,
        passwordEntropyBits = PASSWORD_ENTROPY_BITS,
        mainUri = "https://passbolt.com",
        username = "ada@passbolt.com",
    )

private fun createPasswordState(showUpgradePanel: Boolean = false) =
    ResourceFormState(
        mode = Create(LeadingContentType.PASSWORD, null),
        name = "Passbolt",
        shouldShowScreenProgress = false,
        isPrimaryButtonVisible = true,
        leadingContentType = LeadingContentType.PASSWORD,
        passwordData = passwordData,
        areAdvancedSettingsVisible = true,
        showUpgradePanel = showUpgradePanel,
        supportedAdditionalSecrets =
            listOf(
                ResourceFormUiModel.Secret.NOTE,
                ResourceFormUiModel.Secret.TOTP,
            ),
        supportedMetadata =
            listOf(
                ResourceFormUiModel.Metadata.DESCRIPTION,
                ResourceFormUiModel.Metadata.ADDITIONAL_URIS,
                ResourceFormUiModel.Metadata.APPEARANCE,
            ),
    )

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = TALL_SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ResourceFormScreenCreatePasswordScreenshot() {
    ResourceFormScreen(
        state = createPasswordState(),
        onIntent = {},
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = TALL_SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ResourceFormScreenCreatePasswordUpgradePanelScreenshot() {
    ResourceFormScreen(
        state = createPasswordState(showUpgradePanel = true),
        onIntent = {},
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ResourceFormScreenEditPasswordScreenshot() {
    ResourceFormScreen(
        state =
            ResourceFormState(
                mode = Edit(resourceId = "resource-1", resourceName = "Passbolt"),
                name = "Passbolt",
                shouldShowScreenProgress = false,
                isPrimaryButtonVisible = true,
                leadingContentType = LeadingContentType.PASSWORD,
                passwordData = passwordData,
            ),
        onIntent = {},
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ResourceFormScreenCreateTotpScreenshot() {
    ResourceFormScreen(
        state =
            ResourceFormState(
                mode = Create(LeadingContentType.TOTP, null),
                name = "Passbolt TOTP",
                shouldShowScreenProgress = false,
                isPrimaryButtonVisible = true,
                leadingContentType = LeadingContentType.TOTP,
                totpData =
                    TotpData(
                        totpSecret = "JBSWY3DPEHPK3PXP",
                        totpIssuer = "passbolt.com",
                    ),
                supportedAdditionalSecrets = listOf(ResourceFormUiModel.Secret.NOTE),
                supportedMetadata = listOf(ResourceFormUiModel.Metadata.DESCRIPTION),
            ),
        onIntent = {},
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ResourceFormScreenCreateNoteScreenshot() {
    ResourceFormScreen(
        state =
            ResourceFormState(
                mode = Create(LeadingContentType.STANDALONE_NOTE, null),
                name = "Secret note",
                shouldShowScreenProgress = false,
                isPrimaryButtonVisible = true,
                leadingContentType = LeadingContentType.STANDALONE_NOTE,
                noteData = NoteData(note = "This is a secret note"),
                supportedMetadata =
                    listOf(
                        ResourceFormUiModel.Metadata.DESCRIPTION,
                        ResourceFormUiModel.Metadata.APPEARANCE,
                    ),
            ),
        onIntent = {},
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ResourceFormScreenCreatePinCodeScreenshot() {
    ResourceFormScreen(
        state =
            ResourceFormState(
                mode = Create(LeadingContentType.PIN_CODE, null),
                name = "Door pin",
                shouldShowScreenProgress = false,
                isPrimaryButtonVisible = true,
                leadingContentType = LeadingContentType.PIN_CODE,
                pinCodeData = PinCodeData(pinCode = "1234"),
                supportedMetadata = listOf(ResourceFormUiModel.Metadata.DESCRIPTION),
            ),
        onIntent = {},
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = TALL_SCREEN_HEIGHT_DP, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ResourceFormScreenDarkThemeScreenshot() {
    ResourceFormScreen(
        state = createPasswordState(),
        onIntent = {},
    )
}
