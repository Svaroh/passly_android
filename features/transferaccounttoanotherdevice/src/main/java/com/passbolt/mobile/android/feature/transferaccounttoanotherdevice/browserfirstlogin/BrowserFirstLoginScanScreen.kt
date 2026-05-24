package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.browserfirstlogin

import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.TransferAccountToAnotherDeviceKey.TransferStatus
import com.passbolt.mobile.android.core.qrscan.SCAN_MANAGER_SCOPE
import com.passbolt.mobile.android.core.qrscan.manager.ScanManager
import com.passbolt.mobile.android.core.security.flagsecure.FlagSecureEffect
import com.passbolt.mobile.android.core.ui.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.progresstoolbar.ProgressToolbar
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanIntent.GoBack
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanIntent.Initialize
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanSideEffect.NavigateToResult
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanSideEffect.ShowErrorSnackbar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.compose.scope.KoinScope
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.qualifier.named
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@OptIn(KoinExperimentalAPI::class)
@Composable
internal fun BrowserFirstLoginScanScreen(
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: BrowserFirstLoginScanViewModel = koinViewModel(),
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    FlagSecureEffect()

    val scanScopeId = remember { "${SCAN_MANAGER_SCOPE}_${java.util.UUID.randomUUID()}" }
    KoinScope(
        scopeID = scanScopeId,
        scopeQualifier = named(SCAN_MANAGER_SCOPE),
    ) {
        val scanManager: ScanManager = koinInject()

        LaunchedEffect(scanManager) {
            viewModel.onIntent(Initialize(scanManager.barcodeScanPublisher))
        }

        DisposableEffect(scanManager) {
            onDispose {
                scanManager.detach()
            }
        }

        BrowserFirstLoginScanScreen(
            modifier = modifier,
            state = state,
            snackbarHostState = snackbarHostState,
            onIntent = viewModel::onIntent,
            scanManager = scanManager,
            lifecycleOwner = lifecycleOwner,
        )
    }

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            NavigateBack -> navigator.navigateBack()
            is NavigateToResult -> navigator.navigateToKey(TransferStatus(sideEffect.statusType))
            is ShowErrorSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(sideEffect.message, duration = Short)
                }
        }
    }
}

@Composable
private fun BrowserFirstLoginScanScreen(
    state: BrowserFirstLoginScanState,
    snackbarHostState: SnackbarHostState,
    onIntent: (BrowserFirstLoginScanIntent) -> Unit,
    scanManager: ScanManager,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    BackHandler {
        onIntent(GoBack)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ProgressToolbar(
                progress = 0f,
                onBackClick = { onIntent(GoBack) },
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = colorResource(CoreUiR.color.red),
                        contentColor = colorResource(CoreUiR.color.white),
                    )
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        try {
                            scanManager.attach(lifecycleOwner, this)
                        } catch (exception: Exception) {
                            onIntent(BrowserFirstLoginScanIntent.StartCameraError(exception))
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            Text(
                text = getTooltipMessage(context, state.tooltipMessage),
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(4.dp),
                        ).padding(8.dp),
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    ProgressDialog(isVisible = state.showProgress)
}
