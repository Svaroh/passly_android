package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.passbolt.mobile.android.core.navigation.compose.keys.HomeNavigationKey.Home
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEventBus
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import org.koin.compose.koinInject

@Composable
@Suppress("ktlint:compose:vm-forwarding-check", "ViewModelForwarding")
fun HomeNavigation(
    initialHomeDisplay: HomeDisplayViewModel,
    navigator: AppNavigator = koinInject(),
) {
    val resultBus = remember { ResultEventBus() }

    TabNavigationHost(
        initialKey = Home(initialHomeDisplay),
        featureModulesNavigation = injectFeatureModulesNavigation(NavigationHostFeatures.home),
        resultBus = resultBus,
        navigator = navigator,
    )
}
