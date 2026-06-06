package net.svaroh.passly.permissions.userpermissionsdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.svaroh.passly.core.compose.SideEffectDispatcher
import net.svaroh.passly.core.formatter.FingerprintFormatter
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.results.NavigationResultEventBus
import net.svaroh.passly.core.ui.button.PrimaryButton
import net.svaroh.passly.core.ui.circularimage.CircularProfileImage
import net.svaroh.passly.core.ui.dialogs.PermissionDeleteAlertDialog
import net.svaroh.passly.core.ui.header.ItemWithHeader
import net.svaroh.passly.core.ui.permissions.PermissionLabel
import net.svaroh.passly.core.ui.permissions.PermissionSelector
import net.svaroh.passly.core.ui.topbar.BackNavigationIcon
import net.svaroh.passly.core.ui.topbar.TitleAppBar
import net.svaroh.passly.permissions.navigation.UserPermissionDeletedResult
import net.svaroh.passly.permissions.navigation.UserPermissionModifiedResult
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsIntent.CancelPermissionDelete
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsIntent.ConfirmPermissionDelete
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsIntent.DeletePermission
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsIntent.GoBack
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsIntent.Save
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsIntent.SelectPermission
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsSideEffect.NavigateBack
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsSideEffect.SetDeletePermissionResult
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsSideEffect.SetUpdatedPermissionResult
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PermissionsMode
import net.svaroh.passly.ui.UserModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

@Composable
fun UserPermissionsScreen(
    permission: PermissionModelUi.UserPermissionModel,
    mode: PermissionsMode,
    modifier: Modifier = Modifier,
    viewModel: UserPermissionsViewModel = koinViewModel(parameters = { parametersOf(mode, permission) }),
    navigator: AppNavigator = koinInject(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val resultBus = NavigationResultEventBus.current

    UserPermissionsScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) { effect ->
        when (effect) {
            NavigateBack -> navigator.navigateBack()
            is SetUpdatedPermissionResult -> {
                resultBus.sendResult(result = UserPermissionModifiedResult(effect.permission))
                navigator.navigateBack()
            }
            is SetDeletePermissionResult -> {
                resultBus.sendResult(result = UserPermissionDeletedResult(effect.permission))
                navigator.navigateBack()
            }
        }
    }
}

@Composable
private fun UserPermissionsScreen(
    state: UserPermissionsState,
    onIntent: (UserPermissionsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.user_permission_permission),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                UserHeader(user = state.user)

                Spacer(modifier = Modifier.height(32.dp))

                ItemWithHeader(
                    headerText = stringResource(LocalizationR.string.user_permission_permission),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    if (state.isEditMode) {
                        PermissionSelector(
                            selectedPermission = state.permission.permission,
                            onPermissionSelect = { onIntent(SelectPermission(it)) },
                        )
                    } else {
                        PermissionLabel(permission = state.permission.permission)
                    }
                }
            }

            if (state.isEditMode) {
                // TODO migrate to bottom app bar after all from this module are compose
                SaveLayout(onIntent)
            }
        }

        PermissionDeleteAlertDialog(
            isVisible = state.isDeleteConfirmationVisible,
            onConfirm = { onIntent(ConfirmPermissionDelete) },
            onDismiss = { onIntent(CancelPermissionDelete) },
        )
    }
}

@Composable
private fun UserHeader(
    user: UserModel?,
    modifier: Modifier = Modifier,
    disabledUserAlpha: Float = 0.5f,
    fingerprintFormatter: FingerprintFormatter = koinInject(),
) {
    val isDisabled = user?.disabled == true
    val alpha = if (isDisabled) disabledUserAlpha else 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        CircularProfileImage(
            imageUrl = user?.profile?.avatarUrl,
            width = 96.dp,
            height = 96.dp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text =
                if (isDisabled) {
                    stringResource(LocalizationR.string.name_suspended, user.fullName)
                } else {
                    user?.fullName.orEmpty()
                },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .alpha(alpha),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = user?.userName.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(CoreUiR.color.text_secondary),
            modifier = Modifier.alpha(alpha),
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (user?.gpgKey?.fingerprint != null) {
            Text(
                text =
                    fingerprintFormatter.formatWithRawFallback(
                        user.gpgKey.fingerprint,
                        appendMiddleSpacing = false,
                    ),
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily(Font(CoreUiR.font.inconsolata)),
                        fontSize = 18.sp,
                    ),
                color = colorResource(CoreUiR.color.text_secondary),
            )
        }
    }
}

@Composable
private fun SaveLayout(onIntent: (UserPermissionsIntent) -> Unit) {
    Surface(
        shadowElevation = 24.dp,
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            TextButton(onClick = { onIntent(DeletePermission) }) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.ic_trash),
                    contentDescription = null,
                    tint = colorResource(CoreUiR.color.text_secondary),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(LocalizationR.string.user_permission_delete),
                    color = colorResource(CoreUiR.color.text_secondary),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }

            PrimaryButton(
                text = stringResource(LocalizationR.string.apply),
                onClick = { onIntent(Save) },
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 16.dp),
            )
        }
    }
}
