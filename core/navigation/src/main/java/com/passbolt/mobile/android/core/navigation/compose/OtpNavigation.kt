package net.svaroh.passly.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.Otp
import net.svaroh.passly.core.navigation.compose.results.ResultEventBus
import org.koin.compose.koinInject

@Composable
fun OtpNavigation(navigator: AppNavigator = koinInject()) {
    val resultBus = remember { ResultEventBus() }

    TabNavigationHost(
        initialKey = Otp,
        featureModulesNavigation = injectFeatureModulesNavigation(NavigationHostFeatures.otp),
        resultBus = resultBus,
        navigator = navigator,
    )
}
