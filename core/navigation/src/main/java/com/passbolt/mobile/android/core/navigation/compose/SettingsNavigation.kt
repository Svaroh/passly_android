package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey
import org.koin.compose.koinInject

@Composable
fun SettingsNavigation(navigator: AppNavigator = koinInject()) {
    rememberNavBackStack(SettingsNavigationKey.SettingsMain).let { backstack ->
        navigator.setActiveBackStack(backstack)
    }

    LaunchedEffect(Unit) {
        navigator.consumePendingNavigation()?.let { pendingKey ->
            navigator.navigateToKey(pendingKey)
        }
    }

    val featureModulesNavigation = injectFeatureModulesNavigation(NavigationHostFeatures.settings)

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
