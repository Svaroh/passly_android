package com.passbolt.mobile.android.core.navigation.compose

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.passbolt.mobile.android.core.navigation.compose.base.Feature
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import timber.log.Timber
import com.passbolt.mobile.android.core.localization.R as LocalizationR

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

@Composable
fun injectFeatureModulesNavigation(features: Set<Feature>): Set<FeatureModuleNavigation> =
    features.mapTo(mutableSetOf()) { koinInject<FeatureModuleNavigation>(named(it)) }

fun featureEntryProvider(
    featureModulesNavigation: Set<FeatureModuleNavigation>,
    fallback: (NavKey) -> NavEntry<NavKey>,
): (NavKey) -> NavEntry<NavKey> =
    entryProvider(fallback = fallback) {
        featureModulesNavigation.forEach { installer ->
            installer.provideEntryProviderInstaller().invoke(this)
        }
    }

fun unknownDestinationFallback(navigator: AppNavigator): (NavKey) -> NavEntry<NavKey> =
    { key ->
        NavEntry(key) {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                Timber.e("No destination registered in this navigation host for ${key.loggableName}")
                Toast.makeText(context, LocalizationR.string.common_failure, Toast.LENGTH_SHORT).show()
                navigator.navigateBack()
            }
        }
    }

private val NavKey.loggableName: String?
    get() = this::class.simpleName
