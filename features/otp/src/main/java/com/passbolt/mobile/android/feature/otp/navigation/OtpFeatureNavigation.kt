package net.svaroh.passly.feature.otp.navigation

import PassboltTheme
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.Otp
import net.svaroh.passly.core.navigation.compose.results.OtpScanCompleteResult
import net.svaroh.passly.core.navigation.compose.results.ResourceFormCompleteResult
import net.svaroh.passly.core.navigation.compose.results.ResultEffect
import net.svaroh.passly.feature.otp.screen.OtpIntent.OtpQRScanReturned
import net.svaroh.passly.feature.otp.screen.OtpIntent.ResourceFormReturned
import net.svaroh.passly.feature.otp.screen.OtpScreen
import net.svaroh.passly.feature.otp.screen.OtpViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

class OtpFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<Otp> {
                val navigator: AppNavigator = koinInject()
                val viewModel: OtpViewModel = koinViewModel()

                ResultEffect<OtpScanCompleteResult> { result ->
                    viewModel.onIntent(OtpQRScanReturned(result.otpCreated, result.otpManualCreationChosen))
                }
                ResultEffect<ResourceFormCompleteResult> { result ->
                    viewModel.onIntent(ResourceFormReturned(result.resourceCreated, result.resourceEdited, result.resourceName))
                }

                PassboltTheme {
                    OtpScreen(navigator = navigator, viewModel = viewModel)
                }
            }
        }
}
