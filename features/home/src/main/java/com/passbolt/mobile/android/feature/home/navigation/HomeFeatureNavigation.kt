package net.svaroh.passly.feature.home.navigation

import PassboltTheme
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.remember
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.HomeNavigationKey.Home
import net.svaroh.passly.core.navigation.compose.results.CreateFolderCompleteResult
import net.svaroh.passly.core.navigation.compose.results.OtpScanCompleteResult
import net.svaroh.passly.core.navigation.compose.results.ResourceDetailsCompleteResult
import net.svaroh.passly.core.navigation.compose.results.ResourceFormCompleteResult
import net.svaroh.passly.core.navigation.compose.results.ResultEffect
import net.svaroh.passly.core.navigation.compose.results.ShareCompleteResult
import net.svaroh.passly.feature.home.screen.DefaultResourceHandlingStrategy
import net.svaroh.passly.feature.home.screen.HomeIntent.FolderCreateReturned
import net.svaroh.passly.feature.home.screen.HomeIntent.OtpQRScanReturned
import net.svaroh.passly.feature.home.screen.HomeIntent.ResourceDetailsReturned
import net.svaroh.passly.feature.home.screen.HomeIntent.ResourceFormReturned
import net.svaroh.passly.feature.home.screen.HomeIntent.ResourceShareReturned
import net.svaroh.passly.feature.home.screen.HomeScreen
import net.svaroh.passly.feature.home.screen.HomeViewModel
import net.svaroh.passly.feature.home.screen.ResourceHandlingStrategy
import net.svaroh.passly.feature.home.screen.ResourceHandlingStrategyProvider
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

class HomeFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<Home> { key ->
                val navigator: AppNavigator = koinInject()
                val viewModel: HomeViewModel = koinViewModel()
                val activity = LocalActivity.current
                val resourceHandlingStrategy =
                    remember(activity, navigator) {
                        when (activity) {
                            is ResourceHandlingStrategyProvider -> activity.resourceHandlingStrategy
                            is ResourceHandlingStrategy -> activity
                            else -> DefaultResourceHandlingStrategy(navigator)
                        }
                    }
                val showSuggestedModel =
                    remember(resourceHandlingStrategy) {
                        resourceHandlingStrategy.showSuggestedModel()
                    }

                ResultEffect<ResourceFormCompleteResult> { result ->
                    viewModel.onIntent(ResourceFormReturned(result.resourceCreated, result.resourceEdited, result.resourceName))
                }
                ResultEffect<OtpScanCompleteResult> { result ->
                    viewModel.onIntent(OtpQRScanReturned(result.otpCreated, result.otpManualCreationChosen))
                }
                ResultEffect<ResourceDetailsCompleteResult> { result ->
                    viewModel.onIntent(ResourceDetailsReturned(result.resourceEdited, result.resourceDeleted, result.resourceName))
                }
                ResultEffect<ShareCompleteResult> { result ->
                    viewModel.onIntent(ResourceShareReturned(result.shared))
                }
                ResultEffect<CreateFolderCompleteResult> { result ->
                    viewModel.onIntent(FolderCreateReturned(result.folderName))
                }

                PassboltTheme {
                    HomeScreen(
                        resourceHandlingStrategy = resourceHandlingStrategy,
                        showSuggestedModel = showSuggestedModel,
                        homeView = key.homeDisplayViewModel,
                        navigator = navigator,
                        viewModel = viewModel,
                    )
                }
            }
        }
}
