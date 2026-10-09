/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package net.svaroh.passly.scenarios.settings.appsettings.expertsettings.pagesize

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import net.svaroh.passly.core.idlingresource.ResourcesFullRefreshIdlingResource
import net.svaroh.passly.core.idlingresource.SignInIdlingResource
import net.svaroh.passly.core.idlingresource.SignOutIdlingResource
import net.svaroh.passly.core.navigation.ActivityIntents
import net.svaroh.passly.core.navigation.AppContext
import net.svaroh.passly.domain.preferences.GlobalPreferencesRepository
import net.svaroh.passly.domain.preferences.GlobalPreferencesUpdate
import net.svaroh.passly.domain.preferences.PreferencesDefaults
import net.svaroh.passly.feature.authentication.AuthenticationMainActivity
import net.svaroh.passly.helpers.getString
import net.svaroh.passly.helpers.signIn
import net.svaroh.passly.instrumentationTestsModule
import net.svaroh.passly.intents.ManagedAccountIntentCreator
import net.svaroh.passly.rules.IdlingResourceRule
import net.svaroh.passly.rules.lazyActivitySetupScenarioRule
import net.svaroh.passly.scenarios.setup.autofill.autofillConfiguredModuleTests
import net.svaroh.passly.scenarios.setup.configurebiometric.biometricSetupUnavailableModuleTests
import net.svaroh.passly.testtags.composetags.BackNavigation
import net.svaroh.passly.testtags.composetags.BottomNav
import net.svaroh.passly.testtags.composetags.PageSize
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import net.svaroh.passly.core.localization.R as LocalizationR

@RunWith(AndroidJUnit4::class)
@LargeTest
class ApiFetchPageSizeScreenTest : KoinTest {
    @get:Rule(order = 1)
    val startUpActivityRule =
        lazyActivitySetupScenarioRule<AuthenticationMainActivity>(
            koinOverrideModules =
                listOf(
                    instrumentationTestsModule,
                    biometricSetupUnavailableModuleTests,
                    autofillConfiguredModuleTests,
                ),
            intentSupplier = {
                ActivityIntents.authentication(
                    getInstrumentation().targetContext,
                    ActivityIntents.AuthConfig.Startup,
                    AppContext.APP,
                    managedAccountIntentCreator.getUserLocalId(),
                )
            },
        )

    @get:Rule(order = 0)
    val composeTestRule = createEmptyComposeRule()

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()
    private val globalPreferencesRepository: GlobalPreferencesRepository by inject()

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            val signOutIdlingResource: SignOutIdlingResource by inject()
            val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
            IdlingResourceRule(arrayOf(signInIdlingResource, resourcesFullRefreshIdlingResource, signOutIdlingResource))
        }

    /**
     * **#MOBILE_USER_ON_SETTINGS_PAGE:**
     * Given    I am a mobile user with the application installed
     * And      I am logged in
     * And      I am on the Settings page
     * And      the persisted API fetch page size has been reset to the default in automatic mode,
     *          so previous test runs don't leak state through EncryptedSharedPreferences
     */
    @Before
    fun resetPrefsAndOpenSettings() {
        setPersistedPageSize(PreferencesDefaults.API_FETCH_PAGE_SIZE)
        composeTestRule.apply {
            signIn(managedAccountIntentCreator.getPassphrase())
            onNodeWithTag(BottomNav.SETTINGS_TAB).performClick()
        }
    }

    /**
     * **API fetch page size screen shows title, description, slider bounds and action buttons**
     *
     * TestRail: this test is not placed in TestRail
     *
     * Given    I am #MOBILE_USER_ON_SETTINGS_PAGE
     * When     I open the "API fetch page size" screen
     * Then     the screen title is "API fetch page size"
     * And      a description explains the memory / network trade-off
     * And      the slider min label is "250"
     * And      the slider max label is "10,000"
     * And      the headline number above the slider matches the persisted default ("2,000")
     * And      the save button is shown but disabled (nothing changed yet)
     * And      the restore default values button is shown
     */
    @Test
    fun pageSizeScreenShowsTitleDescriptionSliderBoundsAndActionButtons() {
        openPageSizeScreen()
        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.settings_app_settings_expert_settings_fetch_page_size))
                .assertIsDisplayed()
            onNodeWithText(getString(LocalizationR.string.settings_page_size_description))
                .assertIsDisplayed()
            onNodeWithText("250").assertIsDisplayed()
            onNodeWithText("10,000").assertIsDisplayed()
            // "2,000" mirrors PreferencesDefaults.API_FETCH_PAGE_SIZE; update both together if the default changes.
            onNodeWithTag(PageSize.HEADLINE).assertTextEquals("2,000")
            onNodeWithText(getString(LocalizationR.string.save)).assertIsDisplayed().assertIsNotEnabled()
            onNodeWithText(getString(LocalizationR.string.settings_page_size_restore_default_values))
                .assertIsDisplayed()
        }
    }

    /**
     * **Slider opens at the currently persisted value**
     *
     * TestRail: this test is not placed in TestRail
     *
     * Given    I am #MOBILE_USER_ON_SETTINGS_PAGE
     * And      the persisted API fetch page size is 500
     * When     I open the "API fetch page size" screen
     * Then     the headline number above the slider reads "500"
     */
    @Test
    fun sliderOpensAtTheCurrentlyPersistedValue() {
        setPersistedPageSize(persistedValue = 500)
        openPageSizeScreen()
        composeTestRule.onNodeWithTag(PageSize.HEADLINE).assertTextEquals("500")
    }

    /**
     * **Slider snaps to allowed page sizes and persists only after save**
     *
     * TestRail: this test is not placed in TestRail
     *
     * Given    I am #MOBILE_USER_ON_SETTINGS_PAGE
     * And      I am on the "API fetch page size" screen
     * When     I move the slider to a step
     * Then     the headline number above the slider reads the value formatted for that step
     * And      the persisted API fetch page size is unchanged until I tap save
     * When     I tap save
     * Then     the raw integer value for that step is persisted as a manual choice
     * And      the save button becomes disabled again
     *
     * Cases covered: step 0 → 250, step 2 → 1,000, step 6 → 10,000.
     */
    @Test
    fun sliderSnapsToAllowedPageSizesAndPersistsOnlyAfterSave() {
        openPageSizeScreen()
        // Driving the Slider via the SetProgress semantics action is the
        // accessibility-equivalent of a precise drag — robust regardless of
        // screen size / density.
        listOf(
            SliderStep(step = 0, persisted = 250, displayed = "250"),
            SliderStep(step = 2, persisted = 1_000, displayed = "1,000"),
            SliderStep(step = 6, persisted = 10_000, displayed = "10,000"),
        ).forEach { (step, persisted, displayed) ->
            composeTestRule
                .onNodeWithTag(PageSize.SLIDER)
                .performSemanticsAction(SemanticsActions.SetProgress) { it(step.toFloat()) }

            composeTestRule.onNodeWithTag(PageSize.HEADLINE).assertTextEquals(displayed)

            val persistedBeforeSave = globalPreferencesRepository.getGlobalPreferences().apiFetchPageSize
            check(persistedBeforeSave != persisted) {
                "Expected apiFetchPageSize to remain unsaved before tapping save on slider step $step"
            }

            composeTestRule.onNodeWithText(getString(LocalizationR.string.save)).assertIsEnabled().performClick()

            val globalPreferences = globalPreferencesRepository.getGlobalPreferences()
            check(globalPreferences.apiFetchPageSize == persisted) {
                "Expected persisted apiFetchPageSize=$persisted after saving slider step $step, " +
                    "was ${globalPreferences.apiFetchPageSize}"
            }
            check(globalPreferences.isApiFetchPageSizeManuallySet) {
                "Expected the page size to be flagged as manually set after saving"
            }

            composeTestRule.onNodeWithText(getString(LocalizationR.string.save)).assertIsNotEnabled()
        }
    }

    /**
     * **Saved page size survives leaving the screen, unsaved change is discarded**
     *
     * TestRail: this test is not placed in TestRail
     *
     * Given    I am #MOBILE_USER_ON_SETTINGS_PAGE
     * And      I am on the "API fetch page size" screen
     * And      I have moved the slider to step 2 (1,000) and tapped save
     * When     I move the slider to step 4 (3,000) without saving
     * And      I tap the back navigation icon and re-open the "API fetch page size" screen
     * Then     the headline number above the slider reads the saved "1,000", not the discarded "3,000"
     */
    @Test
    fun savedPageSizeSurvivesLeavingTheScreenWhileUnsavedChangeIsDiscarded() {
        openPageSizeScreen()
        composeTestRule.apply {
            onNodeWithTag(PageSize.SLIDER)
                .performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }
            onNodeWithTag(PageSize.HEADLINE).assertTextEquals("1,000")
            onNodeWithText(getString(LocalizationR.string.save)).performClick()

            onNodeWithTag(PageSize.SLIDER)
                .performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
            onNodeWithTag(PageSize.HEADLINE).assertTextEquals("3,000")

            onNode(hasTestTag(BackNavigation.ICON), useUnmergedTree = true).performClick()
            onNodeWithText(getString(LocalizationR.string.settings_app_settings_expert_settings_fetch_page_size))
                .performClick()

            onNodeWithTag(PageSize.HEADLINE).assertTextEquals("1,000")
        }
    }

    /**
     * **Exceeding the recommended limit shows a warning**
     *
     * TestRail: this test is not placed in TestRail
     *
     * Given    I am #MOBILE_USER_ON_SETTINGS_PAGE
     * And      I am on the "API fetch page size" screen
     * When     I move the slider to the maximum step (10,000 — above every tier's recommended limit)
     * Then     the "exceeds recommended limit" warning is shown
     * When     I move the slider to the minimum step (250 — below every tier's recommended limit)
     * Then     the warning is hidden
     */
    @Test
    fun exceedingTheRecommendedLimitShowsAWarning() {
        openPageSizeScreen()
        composeTestRule.apply {
            onNodeWithTag(PageSize.SLIDER)
                .performSemanticsAction(SemanticsActions.SetProgress) { it(6f) }
            onNodeWithText(getString(LocalizationR.string.settings_page_size_exceeds_recommended_limit))
                .assertIsDisplayed()

            onNodeWithTag(PageSize.SLIDER)
                .performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
            onNodeWithText(getString(LocalizationR.string.settings_page_size_exceeds_recommended_limit))
                .assertDoesNotExist()
        }
    }

    /**
     * **Restore default values returns to the automatic page size**
     *
     * TestRail: this test is not placed in TestRail
     *
     * Given    I am #MOBILE_USER_ON_SETTINGS_PAGE
     * And      I am on the "API fetch page size" screen
     * And      I have saved a manual page size (10,000)
     * When     I tap "Restore default values"
     * Then     the automatic (device tier based) page size is persisted — one of 1,000 / 2,000 / 3,000
     * And      the manual flag is cleared
     * And      the save button is disabled (slider matches the persisted value)
     */
    @Test
    fun restoreDefaultValuesReturnsToTheAutomaticPageSize() {
        openPageSizeScreen()
        composeTestRule.apply {
            onNodeWithTag(PageSize.SLIDER)
                .performSemanticsAction(SemanticsActions.SetProgress) { it(6f) }
            onNodeWithText(getString(LocalizationR.string.save)).performClick()

            onNodeWithText(getString(LocalizationR.string.settings_page_size_restore_default_values)).performClick()

            val globalPreferences = globalPreferencesRepository.getGlobalPreferences()
            check(globalPreferences.apiFetchPageSize in setOf(1_000, 2_000, 3_000)) {
                "Expected a tier default page size after restore, was ${globalPreferences.apiFetchPageSize}"
            }
            check(!globalPreferences.isApiFetchPageSizeManuallySet) {
                "Expected the manual flag to be cleared after restore"
            }

            onNodeWithText(getString(LocalizationR.string.save)).assertIsNotEnabled()
        }
    }

    private fun setPersistedPageSize(persistedValue: Int) {
        globalPreferencesRepository.updateGlobalPreferences(
            GlobalPreferencesUpdate(
                apiFetchPageSize = persistedValue,
                isApiFetchPageSizeManuallySet = false,
            ),
        )
    }

    private fun openPageSizeScreen() {
        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.settings_app_settings)).performClick()
            onNodeWithText(getString(LocalizationR.string.settings_app_settings_expert_settings)).performClick()
            onNodeWithText(getString(LocalizationR.string.settings_app_settings_expert_settings_fetch_page_size))
                .performClick()
        }
    }

    private data class SliderStep(
        val step: Int,
        val persisted: Int,
        val displayed: String,
    )
}
