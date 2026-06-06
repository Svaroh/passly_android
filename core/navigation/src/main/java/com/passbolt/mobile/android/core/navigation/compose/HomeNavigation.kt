package net.svaroh.passly.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.svaroh.passly.core.navigation.compose.keys.HomeNavigationKey.Home
import net.svaroh.passly.core.navigation.compose.results.ResultEventBus
import net.svaroh.passly.ui.HomeDisplayViewModel
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
