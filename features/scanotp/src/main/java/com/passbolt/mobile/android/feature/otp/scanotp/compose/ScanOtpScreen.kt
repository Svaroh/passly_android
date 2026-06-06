package net.svaroh.passly.feature.otp.scanotp.compose

import android.Manifest.permission.CAMERA
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.svaroh.passly.core.compose.SideEffectDispatcher
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.Otp
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.ScanOtpSuccess
import net.svaroh.passly.core.navigation.compose.results.NavigationResultEventBus
import net.svaroh.passly.core.navigation.compose.results.OtpScanCompleteResult
import net.svaroh.passly.core.navigation.compose.results.ScanOtpResultEvent
import net.svaroh.passly.core.qrscan.SCAN_MANAGER_SCOPE
import net.svaroh.passly.core.qrscan.manager.ScanManager
import net.svaroh.passly.core.security.flagsecure.FlagSecureEffect
import net.svaroh.passly.core.ui.button.PrimaryButton
import net.svaroh.passly.core.ui.dialogs.CameraPermissionRequiredAlertDialog
import net.svaroh.passly.core.ui.dialogs.CameraRequiredAlertDialog
import net.svaroh.passly.core.ui.topbar.BackNavigationIcon
import net.svaroh.passly.core.ui.topbar.TitleAppBar
import net.svaroh.passly.feature.otp.scanotp.ScanOtpMode
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.CreateTotpManually
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.DismissCameraPermissionRequiredDialog
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.DismissCameraRequiredDialog
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.GoBack
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.GoToSettings
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.Initialize
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.RejectCameraPermission
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.StartCameraError
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.NavigateBack
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.NavigateToAppSettings
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.NavigateToSuccess
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.RequestCameraPermission
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.SetManualCreationResultAndNavigateBack
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.SetResultAndNavigateBack
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.compose.scope.KoinScope
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.qualifier.named
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

@OptIn(KoinExperimentalAPI::class)
@Composable
internal fun ScanOtpScreen(
    mode: ScanOtpMode,
    parentFolderId: String?,
    modifier: Modifier = Modifier,
    viewModel: ScanOtpViewModel = koinViewModel(),
    navigator: AppNavigator = koinInject(),
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val resultBus = NavigationResultEventBus.current

    FlagSecureEffect()

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (!isGranted) {
                viewModel.onIntent(RejectCameraPermission)
            }
        }

    val scanScopeId = remember { "${SCAN_MANAGER_SCOPE}_${java.util.UUID.randomUUID()}" }
    KoinScope(
        scopeID = scanScopeId,
        scopeQualifier = named(SCAN_MANAGER_SCOPE),
    ) {
        val scanManager: ScanManager = koinInject()

        LaunchedEffect(scanManager) {
            viewModel.onIntent(
                Initialize(
                    barcodeScanFlow = scanManager.barcodeScanPublisher,
                    mode = mode,
                ),
            )
        }

        DisposableEffect(scanManager) {
            onDispose {
                scanManager.detach()
            }
        }

        ScanOtpScreen(
            modifier = modifier,
            state = state,
            onIntent = viewModel::onIntent,
            scanManager = scanManager,
            lifecycleOwner = lifecycleOwner,
        )
    }

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            RequestCameraPermission -> requestPermissionLauncher.launch(CAMERA)
            is NavigateToSuccess ->
                navigator.navigateToKey(
                    ScanOtpSuccess(
                        totpLabel = sideEffect.totpQr.label,
                        totpSecret = sideEffect.totpQr.secret,
                        totpIssuer = sideEffect.totpQr.issuer,
                        totpAlgorithm = sideEffect.totpQr.algorithm.name,
                        totpDigits = sideEffect.totpQr.digits,
                        totpPeriod = sideEffect.totpQr.period,
                        parentFolderId = parentFolderId,
                    ),
                )
            is SetResultAndNavigateBack -> {
                resultBus.sendResult(result = ScanOtpResultEvent(false, sideEffect.totpQr))
                navigator.navigateBack()
            }
            SetManualCreationResultAndNavigateBack ->
                when (mode) {
                    ScanOtpMode.SCAN_FOR_RESULT -> {
                        resultBus.sendResult(result = ScanOtpResultEvent(true, null))
                        navigator.navigateBack()
                    }
                    ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN -> {
                        resultBus.sendResult(
                            result = OtpScanCompleteResult(otpCreated = false, otpManualCreationChosen = true),
                        )
                        navigator.popToKey(Otp)
                    }
                }
            NavigateToAppSettings -> {
                navigator.navigateBack()
                navigator.openAppOsSettings(context)
            }
            NavigateBack -> navigator.navigateBack()
        }
    }
}

@Composable
private fun ScanOtpScreen(
    state: ScanOtpState,
    onIntent: (ScanOtpIntent) -> Unit,
    scanManager: ScanManager,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.otp_scan_title),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
        containerColor = colorResource(CoreUiR.color.background_gray_dark),
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            Text(
                text = getTooltipMessage(state.tooltipMessage, state.scanErrorMessage),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colorResource(CoreUiR.color.background))
                        .padding(vertical = 8.dp),
                color = colorResource(CoreUiR.color.text_primary),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            try {
                                scanManager.attach(lifecycleOwner, this)
                            } catch (exception: Exception) {
                                onIntent(StartCameraError(exception))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(colorResource(CoreUiR.color.background)),
                contentAlignment = Alignment.Center,
            ) {
                PrimaryButton(
                    text = stringResource(LocalizationR.string.scan_qr_or_create_totp_manually),
                    onClick = { onIntent(CreateTotpManually) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }

    CameraRequiredAlertDialog(
        isVisible = state.showCameraRequiredDialog,
        onDismissRequest = { onIntent(DismissCameraRequiredDialog) },
    )

    CameraPermissionRequiredAlertDialog(
        isVisible = state.showCameraPermissionRequiredDialog,
        onDismissRequest = { onIntent(DismissCameraPermissionRequiredDialog) },
        onSettingsClick = { onIntent(GoToSettings) },
    )
}
