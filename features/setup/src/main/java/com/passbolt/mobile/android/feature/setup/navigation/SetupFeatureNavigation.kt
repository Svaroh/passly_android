package net.svaroh.passly.feature.setup.navigation

import PassboltTheme
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.SetupNavigationKey.AccessibilityPolicies
import net.svaroh.passly.core.navigation.compose.keys.SetupNavigationKey.BiometricSetup
import net.svaroh.passly.core.navigation.compose.keys.SetupNavigationKey.ImportProfile
import net.svaroh.passly.core.navigation.compose.keys.SetupNavigationKey.ScanQrCodes
import net.svaroh.passly.core.navigation.compose.keys.SetupNavigationKey.Summary
import net.svaroh.passly.core.navigation.compose.keys.SetupNavigationKey.TransferDetails
import net.svaroh.passly.core.navigation.compose.keys.SetupNavigationKey.Welcome
import net.svaroh.passly.feature.accessibilitypolicies.AccessibilityPoliciesFlow
import net.svaroh.passly.feature.accessibilitypolicies.AccessibilityPoliciesScreen
import net.svaroh.passly.feature.setup.biometric.BiometricSetupScreen
import net.svaroh.passly.feature.setup.importprofile.ImportProfileScreen
import net.svaroh.passly.feature.setup.scanqr.ScanQrScreen
import net.svaroh.passly.feature.setup.summary.SummaryScreen
import net.svaroh.passly.feature.setup.transferdetails.TransferDetailsScreen
import net.svaroh.passly.feature.setup.welcome.WelcomeScreen

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */
class SetupFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<AccessibilityPolicies> {
                PassboltTheme { AccessibilityPoliciesScreen(flow = AccessibilityPoliciesFlow.SETUP) }
            }
            entry<Welcome> {
                PassboltTheme { WelcomeScreen() }
            }
            entry<TransferDetails> {
                PassboltTheme { TransferDetailsScreen() }
            }
            entry<ScanQrCodes> {
                PassboltTheme { ScanQrScreen() }
            }
            entry<BiometricSetup> {
                PassboltTheme { BiometricSetupScreen() }
            }
            entry<ImportProfile> {
                PassboltTheme { ImportProfileScreen() }
            }
            entry<Summary> {
                PassboltTheme { SummaryScreen(status = it.status) }
            }
        }
}
