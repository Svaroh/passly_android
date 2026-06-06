package net.svaroh.passly.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.svaroh.passly.core.navigation.compose.base.Feature.ACCOUNT_DETAILS
import net.svaroh.passly.core.navigation.compose.base.Feature.OTP
import net.svaroh.passly.core.navigation.compose.base.Feature.RESOURCE_FORM
import net.svaroh.passly.core.navigation.compose.base.Feature.RESOURCE_PICKER
import net.svaroh.passly.core.navigation.compose.base.Feature.SCAN_OTP
import net.svaroh.passly.core.navigation.compose.base.Feature.TRANSFER_ACCOUNT_TO_ANOTHER_DEVICE
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.Otp
import net.svaroh.passly.core.navigation.compose.results.ResultEventBus
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun OtpNavigation(navigator: AppNavigator = koinInject()) {
    val resultBus = remember { ResultEventBus() }

    TabNavigationHost(
        initialKey = Otp,
        featureModulesNavigation =
            setOf(
                koinInject<FeatureModuleNavigation>(named(OTP)),
                koinInject<FeatureModuleNavigation>(named(SCAN_OTP)),
                koinInject<FeatureModuleNavigation>(named(RESOURCE_FORM)),
                koinInject<FeatureModuleNavigation>(named(RESOURCE_PICKER)),
                koinInject<FeatureModuleNavigation>(named(ACCOUNT_DETAILS)),
                koinInject<FeatureModuleNavigation>(named(TRANSFER_ACCOUNT_TO_ANOTHER_DEVICE)),
            ),
        resultBus = resultBus,
        navigator = navigator,
    )
}
