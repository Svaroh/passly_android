/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2024-2026 Passbolt SA
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

package net.svaroh.passly.scenarios.resource.details.sharewithsubsection

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import net.svaroh.passly.core.idlingresource.ResourceDetailActionIdlingResource
import net.svaroh.passly.core.idlingresource.ResourcesFullRefreshIdlingResource
import net.svaroh.passly.core.idlingresource.SignInIdlingResource
import net.svaroh.passly.core.localization.R.string.filters_menu_all_items
import net.svaroh.passly.core.navigation.ActivityIntents
import net.svaroh.passly.core.navigation.AppContext
import net.svaroh.passly.feature.authentication.AuthenticationMainActivity
import net.svaroh.passly.helpers.chooseFilter
import net.svaroh.passly.helpers.getString
import net.svaroh.passly.helpers.searchAndOpenFirstResourceByName
import net.svaroh.passly.helpers.signIn
import net.svaroh.passly.instrumentationTestsModule
import net.svaroh.passly.intents.ManagedAccountIntentCreator
import net.svaroh.passly.rules.IdlingResourceRule
import net.svaroh.passly.rules.lazyActivitySetupScenarioRule
import net.svaroh.passly.scenarios.resource.TestResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.koin.core.component.inject
import org.koin.test.KoinTest
import net.svaroh.passly.core.localization.R as LocalizationR

@RunWith(Parameterized::class)
@MediumTest
class SharedWithSubsectionTest(
    private val resourceType: TestResourceType,
) : KoinTest {
    @get:Rule(order = 0)
    val startUpActivityRule =
        lazyActivitySetupScenarioRule<AuthenticationMainActivity>(
            koinOverrideModules = listOf(instrumentationTestsModule),
            intentSupplier = {
                ActivityIntents.authentication(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    ActivityIntents.AuthConfig.Startup,
                    AppContext.APP,
                    managedAccountIntentCreator.getUserLocalId(),
                )
            },
        )

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
            val resourceDetailActionIdlingResource: ResourceDetailActionIdlingResource by inject()
            IdlingResourceRule(
                arrayOf(
                    signInIdlingResource,
                    resourcesFullRefreshIdlingResource,
                    resourceDetailActionIdlingResource,
                ),
            )
        }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        composeTestRule.apply {
            signIn(managedAccountIntentCreator.getPassphrase())
            chooseFilter(filters_menu_all_items)
            searchAndOpenFirstResourceByName(resourceType.displayName)
        }
    }

    /**
     * [On the resource screen I can see Shared with subsection](https://passbolt.testrail.io/index.php?/cases/view/10599)
     *
     * Given I am a user on the <resource> display screen
     * And   I have Shared with permission
     * When  I review screen content
     * Then  I see Shared with subsection with corresponding title
     * And   Shared with subsection is filled with icons of users
     * And   At least one icon is presented
     * And   Shared with subsection contains caret
     *
     *     Examples:
     *     | resource                       |
     *     | Simple password                |
     *     | Password with description      |
     *     | Password description totp      |
     *     | TOTP - v4                      |
     *     | Simple Password (Deprecated)   |
     *     | Default resource type          |
     *     | Default resource type with TOTP|
     *     | Standalone TOTP                |
     *     | Standalone note                |
     *     | Standalone Custom Fields       |
     */
    @Test
    fun onTheResourceScreenICanSeeSharedWithSubsection() {
        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.shared_with)).assertExists()
        }
    }

    private companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Resource type: {0}")
        fun resourceTypes() = TestResourceType.entries
    }
}
