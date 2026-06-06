package net.svaroh.passly.feature.otp.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.remember
import net.svaroh.passly.core.compose.PassboltTheme
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.Otp
import net.svaroh.passly.core.navigation.compose.results.OtpScanCompleteResult
import net.svaroh.passly.core.navigation.compose.results.PermissionsConfirmedResult
import net.svaroh.passly.core.navigation.compose.results.ResourceFormCompleteResult
import net.svaroh.passly.core.navigation.compose.results.ResultEffect
import net.svaroh.passly.feature.home.screen.ResourceHandlingStrategy
import net.svaroh.passly.feature.home.screen.ResourceHandlingStrategyProvider
import net.svaroh.passly.feature.home.screen.ShowSuggestedModel.DoNotShow
import net.svaroh.passly.feature.otp.screen.OtpIntent.ConfirmedPermissionsResult
import net.svaroh.passly.feature.otp.screen.OtpIntent.OtpQRScanReturned
import net.svaroh.passly.feature.otp.screen.OtpIntent.ResourceFormReturned
import net.svaroh.passly.feature.otp.screen.OtpIntent.RevealOtp
import net.svaroh.passly.feature.otp.screen.OtpResourceHandlingStrategy
import net.svaroh.passly.feature.otp.screen.OtpScreen
import net.svaroh.passly.feature.otp.screen.OtpViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class OtpFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<Otp> {
                val navigator: AppNavigator = koinInject()
                val activity = LocalActivity.current
                val showSuggestedModel =
                    remember(activity) {
                        when (activity) {
                            is ResourceHandlingStrategyProvider -> activity.resourceHandlingStrategy.showSuggestedModel()
                            else -> DoNotShow
                        }
                    }
                val viewModel: OtpViewModel = koinViewModel(parameters = { parametersOf(showSuggestedModel) })
                val resourceHandlingStrategy: ResourceHandlingStrategy =
                    remember(activity, viewModel) {
                        when (activity) {
                            is ResourceHandlingStrategyProvider -> activity.resourceHandlingStrategy
                            else ->
                                OtpResourceHandlingStrategy(
                                    onItemClick = { resource -> viewModel.onIntent(RevealOtp(resource)) },
                                )
                        }
                    }

                ResultEffect<OtpScanCompleteResult> { result ->
                    viewModel.onIntent(OtpQRScanReturned(result.otpCreated, result.otpManualCreationChosen))
                }
                ResultEffect<ResourceFormCompleteResult> { result ->
                    viewModel.onIntent(ResourceFormReturned(result.resourceCreated, result.resourceEdited, result.resourceName))
                }
                ResultEffect<PermissionsConfirmedResult> { result ->
                    viewModel.onIntent(ConfirmedPermissionsResult(result.permissions))
                }

                PassboltTheme {
                    OtpScreen(
                        navigator = navigator,
                        resourceHandlingStrategy = resourceHandlingStrategy,
                        viewModel = viewModel,
                    )
                }
            }
        }
}
