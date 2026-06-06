package net.svaroh.passly.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.svaroh.passly.core.navigation.compose.base.Feature.ACCOUNT_DETAILS
import net.svaroh.passly.core.navigation.compose.base.Feature.CREATE_FOLDER
import net.svaroh.passly.core.navigation.compose.base.Feature.FOLDER_DETAILS
import net.svaroh.passly.core.navigation.compose.base.Feature.GROUP_DETAILS
import net.svaroh.passly.core.navigation.compose.base.Feature.HOME
import net.svaroh.passly.core.navigation.compose.base.Feature.LOCATION_DETAILS
import net.svaroh.passly.core.navigation.compose.base.Feature.PERMISSIONS
import net.svaroh.passly.core.navigation.compose.base.Feature.RESOURCE_DETAILS
import net.svaroh.passly.core.navigation.compose.base.Feature.RESOURCE_FORM
import net.svaroh.passly.core.navigation.compose.base.Feature.RESOURCE_PICKER
import net.svaroh.passly.core.navigation.compose.base.Feature.SCAN_OTP
import net.svaroh.passly.core.navigation.compose.base.Feature.TAGS_DETAILS
import net.svaroh.passly.core.navigation.compose.base.Feature.TRANSFER_ACCOUNT_TO_ANOTHER_DEVICE
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.HomeNavigationKey.Home
import net.svaroh.passly.core.navigation.compose.results.ResultEventBus
import net.svaroh.passly.ui.HomeDisplayViewModel
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
@Suppress("ktlint:compose:vm-forwarding-check", "ViewModelForwarding")
fun HomeNavigation(
    initialHomeDisplay: HomeDisplayViewModel,
    navigator: AppNavigator = koinInject(),
) {
    val resultBus = remember { ResultEventBus() }

    TabNavigationHost(
        initialKey = Home(initialHomeDisplay),
        featureModulesNavigation =
            setOf(
                koinInject<FeatureModuleNavigation>(named(HOME)),
                koinInject<FeatureModuleNavigation>(named(RESOURCE_FORM)),
                koinInject<FeatureModuleNavigation>(named(SCAN_OTP)),
                koinInject<FeatureModuleNavigation>(named(RESOURCE_PICKER)),
                koinInject<FeatureModuleNavigation>(named(PERMISSIONS)),
                koinInject<FeatureModuleNavigation>(named(GROUP_DETAILS)),
                koinInject<FeatureModuleNavigation>(named(RESOURCE_DETAILS)),
                koinInject<FeatureModuleNavigation>(named(FOLDER_DETAILS)),
                koinInject<FeatureModuleNavigation>(named(CREATE_FOLDER)),
                koinInject<FeatureModuleNavigation>(named(TAGS_DETAILS)),
                koinInject<FeatureModuleNavigation>(named(LOCATION_DETAILS)),
                koinInject<FeatureModuleNavigation>(named(ACCOUNT_DETAILS)),
                koinInject<FeatureModuleNavigation>(named(TRANSFER_ACCOUNT_TO_ANOTHER_DEVICE)),
            ),
        resultBus = resultBus,
        navigator = navigator,
    )
}
