package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.Welcome
import org.koin.compose.koinInject

@Composable
fun SetupNavigation(navigator: AppNavigator = koinInject()) {
    rememberNavBackStack(Welcome).let { backstack ->
        navigator.setActiveBackStack(backstack)
    }

    val featureModulesNavigation = injectFeatureModulesNavigation(NavigationHostFeatures.setup)

    NavDisplay(
        backStack = navigator.backStack,
        onBack = { navigator.navigateBack() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider = featureEntryProvider(featureModulesNavigation, unknownDestinationFallback(navigator)),
        transitionSpec = { horizontalSlideTransition },
        popTransitionSpec = { horizontalSlidePopTransition },
        predictivePopTransitionSpec = { horizontalSlidePopTransition },
    )
}
