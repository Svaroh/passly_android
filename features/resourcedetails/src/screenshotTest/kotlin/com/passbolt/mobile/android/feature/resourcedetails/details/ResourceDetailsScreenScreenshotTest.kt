package com.passbolt.mobile.android.feature.resourcedetails.details

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.formatter.OtpFormatter
import com.passbolt.mobile.android.core.ui.controller.TotpComposeController
import com.passbolt.mobile.android.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper
import com.passbolt.mobile.android.core.ui.screenshot.ensureScreenshotKoinStarted
import com.passbolt.mobile.android.jsonmodel.jsonModelModule
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.PermissionModelUi.GroupPermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi.UserPermissionModel
import com.passbolt.mobile.android.ui.ResourcePermission.OWNER
import com.passbolt.mobile.android.ui.ResourcePermission.READ
import com.passbolt.mobile.android.ui.ResourceUiModel
import com.passbolt.mobile.android.ui.UserWithAvatar
import org.koin.dsl.module
import java.time.ZonedDateTime

private const val SCREEN_WIDTH_DP = 360
private const val SCREEN_HEIGHT_DP = 800
private const val TALL_SCREEN_HEIGHT_DP = 1400

private const val OTP_EXPIRY_SECONDS = 30L
private const val OTP_REMAINING_SECONDS = 20L

private val fixedModifiedDate: ZonedDateTime = ZonedDateTime.parse("2024-01-15T12:00:00Z")
private val pastExpiryDate: ZonedDateTime = ZonedDateTime.parse("2020-01-01T12:00:00Z")

private fun resourceModel(
    metadataJson: String,
    expiry: ZonedDateTime? = null,
    favouriteId: String? = null,
): ResourceUiModel =
    ResourceUiModel(
        resourceId = "resource-1",
        resourceTypeId = "resource-type-1",
        slug = "v5-default",
        folderId = null,
        permission = OWNER,
        favouriteId = favouriteId,
        modified = fixedModifiedDate,
        expiry = expiry,
        metadataKeyId = null,
        metadataKeyType = null,
        metadataJsonModel = MetadataJsonModel(metadataJson),
    )

private val fullResourceModel =
    resourceModel(
        metadataJson =
            """{"name": "Passbolt Cloud", "username": "ada@passbolt.com", "description": "Company password manager account."}""",
        favouriteId = "favourite-1",
    )

private val ownerPermission =
    UserPermissionModel(
        permission = OWNER,
        permissionId = "permission-1",
        user =
            UserWithAvatar(
                userId = "user-1",
                firstName = "Ada",
                lastName = "Lovelace",
                userName = "ada@passbolt.com",
                isDisabled = false,
                avatarUrl = null,
            ),
    )

private val groupPermission =
    GroupPermissionModel(
        permission = READ,
        permissionId = "permission-2",
        group =
            GroupModel(
                groupId = "group-1",
                groupName = "Engineering",
            ),
    )

private fun fullState(resourceModel: ResourceUiModel = fullResourceModel) =
    ResourceDetailsState(
        resourceData = ResourceData(resourceModel = resourceModel),
        passwordData =
            PasswordData(
                showPasswordItem = true,
                showPasswordEyeIcon = true,
                isPasswordVisible = false,
                password = "p@ssb0lt!",
            ),
        totpData =
            TotpData(
                showTotpSection = true,
                totpModel =
                    OtpItemWrapper(
                        resource = resourceModel,
                        isVisible = false,
                        isRefreshing = false,
                        otpExpirySeconds = OTP_EXPIRY_SECONDS,
                        otpValue = null,
                        remainingSecondsCounter = OTP_REMAINING_SECONDS,
                    ),
            ),
        noteData =
            NoteData(
                showNoteSection = true,
                isNoteVisible = false,
                note = "Shared with the mobile team.",
            ),
        metadataData =
            MetadataData(
                showMetadataDescriptionItem = true,
                canViewTags = true,
                tags = listOf("work", "cloud"),
                canViewLocation = true,
                locationPath = listOf("Projects", "Mobile"),
                mainUri = "https://cloud.passbolt.com",
                additionalUris = listOf("https://status.passbolt.com"),
            ),
        sharedWithData =
            SharedWithData(
                canViewPermissions = true,
                permissions = listOf(ownerPermission, groupPermission),
            ),
    )

private val screenshotKoin =
    ensureScreenshotKoinStarted(
        jsonModelModule,
        module {
            single { OtpFormatter() }
            single { TotpComposeController() }
        },
    )

@Composable
private fun ResourceDetailsScreenUnderTest(state: ResourceDetailsState) {
    ResourceDetailsScreen(
        state = state,
        onIntent = {},
        snackbarHostState = SnackbarHostState(),
        resourceIcon = null,
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = TALL_SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ResourceDetailsScreenScreenshot() {
    ResourceDetailsScreenUnderTest(state = fullState())
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ResourceDetailsScreenMinimalScreenshot() {
    ResourceDetailsScreenUnderTest(
        state =
            ResourceDetailsState(
                resourceData =
                    ResourceData(
                        resourceModel = resourceModel(metadataJson = """{"name": "Personal note"}"""),
                    ),
            ),
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = TALL_SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ResourceDetailsScreenExpiredScreenshot() {
    ResourceDetailsScreenUnderTest(
        state =
            fullState(
                resourceModel =
                    resourceModel(
                        metadataJson =
                            """{"name": "Passbolt Cloud", "username": "ada@passbolt.com", "description": "Company password manager account."}""",
                        expiry = pastExpiryDate,
                    ),
            ),
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = TALL_SCREEN_HEIGHT_DP, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ResourceDetailsScreenDarkThemeScreenshot() {
    ResourceDetailsScreenUnderTest(state = fullState())
}
