package com.passbolt.mobile.android.feature.otp.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.remember
import com.passbolt.mobile.android.core.compose.PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.Otp
import com.passbolt.mobile.android.core.navigation.compose.results.OtpScanCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.PermissionsConfirmedResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResourceFormCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEffect
import com.passbolt.mobile.android.feature.home.screen.ResourceHandlingStrategy
import com.passbolt.mobile.android.feature.home.screen.ResourceHandlingStrategyProvider
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel.DoNotShow
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.ConfirmedPermissionsResult
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OtpQRScanReturned
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.ResourceFormReturned
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.RevealOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpResourceHandlingStrategy
import com.passbolt.mobile.android.feature.otp.screen.OtpScreen
import com.passbolt.mobile.android.feature.otp.screen.OtpViewModel
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
