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

package com.passbolt.mobile.android.core.ui.screenshot

import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Starts the global Koin context for screenshot rendering, once per render JVM.
 *
 * Screenshot subjects are called with plain state, but some resolve dependencies from Koin below
 * the state-driven tier: `koinInject()` default parameters and `KoinComponent`-based delegates
 * (e.g. `MetadataJsonModel`'s JsonPath delegates) both read the global context during composition.
 * A composition-local `KoinApplication` does not reach `KoinComponent` lookups, so the global
 * context is the one that works for both.
 *
 * Reference it from a top-level `private val` in the screenshot test file so it runs before the
 * first render. The guard makes repeated calls within one render JVM no-ops - keep one Koin
 * fixture per module.
 */
fun ensureScreenshotKoinStarted(vararg modules: Module) {
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            modules(*modules)
        }
    }
}
