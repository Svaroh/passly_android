package net.svaroh.passly.feature.otp.scanotp.navigation

import PassboltTheme
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.ScanOtp
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.ScanOtpMode.SCAN_FOR_RESULT
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.ScanOtpSuccess
import net.svaroh.passly.core.navigation.compose.results.ResourcePickerResultEvent
import net.svaroh.passly.core.navigation.compose.results.ResultEffect
import net.svaroh.passly.feature.otp.scanotp.ScanOtpMode
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpScreen
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.LinkedResourceReceived
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessScreen
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessViewModel
import net.svaroh.passly.ui.OtpParseResult
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.ScanOtpMode as NavigationScanOtpMode

class ScanOtpFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<ScanOtp> { key ->
                PassboltTheme {
                    ScanOtpScreen(
                        mode = key.mode.toFeatureMode(),
                        parentFolderId = key.parentFolderId,
                    )
                }
            }

            entry<ScanOtpSuccess> { key ->
                val viewModel: ScanOtpSuccessViewModel =
                    koinViewModel { parametersOf(key.toTotpQr(), key.parentFolderId) }

                ResultEffect<ResourcePickerResultEvent> { result ->
                    viewModel.onIntent(LinkedResourceReceived(result.resource))
                }

                PassboltTheme {
                    ScanOtpSuccessScreen(
                        scannedTotp = key.toTotpQr(),
                        parentFolderId = key.parentFolderId,
                        viewModel = viewModel,
                    )
                }
            }
        }
}

private fun NavigationScanOtpMode.toFeatureMode(): ScanOtpMode =
    when (this) {
        SCAN_FOR_RESULT -> ScanOtpMode.SCAN_FOR_RESULT
        SCAN_WITH_SUCCESS_SCREEN -> ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN
    }

private fun ScanOtpSuccess.toTotpQr(): OtpParseResult.OtpQr.TotpQr =
    OtpParseResult.OtpQr.TotpQr(
        label = totpLabel,
        secret = totpSecret,
        issuer = totpIssuer,
        algorithm = OtpParseResult.OtpQr.Algorithm.valueOf(totpAlgorithm),
        digits = totpDigits,
        period = totpPeriod,
    )
