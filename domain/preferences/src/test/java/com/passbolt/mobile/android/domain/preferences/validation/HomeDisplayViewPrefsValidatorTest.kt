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

package net.svaroh.passly.domain.preferences.validation

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.domain.rbac.usecase.GetRbacRulesUseCase
import net.svaroh.passly.entity.featureflags.FeatureFlagsModel
import net.svaroh.passly.featureflags.usecase.GetFeatureFlagsUseCase
import net.svaroh.passly.ui.DefaultFilterUiModel
import net.svaroh.passly.ui.HomeDisplayViewPreferencesUiModel
import net.svaroh.passly.ui.HomeDisplayViewUiModel
import net.svaroh.passly.ui.RbacModel
import net.svaroh.passly.ui.RbacRuleModel
import net.svaroh.passly.ui.RbacRuleModel.ALLOW
import net.svaroh.passly.ui.RbacRuleModel.DENY
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class HomeDisplayViewPrefsValidatorTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetFeatureFlagsUseCase>() }
                        single { mock<GetRbacRulesUseCase>() }
                        factory {
                            HomeDisplayViewPrefsValidator(
                                getFeatureFlagsUseCase = get(),
                                getRbacRulesUseCase = get(),
                            )
                        }
                    },
                ),
            )
        }

    private lateinit var getFeatureFlagsUseCase: GetFeatureFlagsUseCase
    private lateinit var getRbacRulesUseCase: GetRbacRulesUseCase
    private lateinit var validator: HomeDisplayViewPrefsValidator

    @Before
    fun setUp() {
        getFeatureFlagsUseCase = get()
        getRbacRulesUseCase = get()
        validator = get()
    }

    @Test
    fun `validated keeps folders and tags views when available and allowed`() {
        stubFeatureFlags(areFoldersAvailable = true, areTagsAvailable = true)
        stubRbac(foldersUseRule = ALLOW, tagsUseRule = ALLOW)
        val preferences =
            HomeDisplayViewPreferencesUiModel(
                lastUsedHomeView = HomeDisplayViewUiModel.FOLDERS,
                userSetHomeView = DefaultFilterUiModel.TAGS,
            )

        val result = validator.validated(preferences)

        assertThat(result).isEqualTo(preferences)
    }

    @Test
    fun `validated falls back to all items when folders feature flag is off`() {
        stubFeatureFlags(areFoldersAvailable = false, areTagsAvailable = true)
        stubRbac(foldersUseRule = ALLOW, tagsUseRule = ALLOW)
        val preferences =
            HomeDisplayViewPreferencesUiModel(
                lastUsedHomeView = HomeDisplayViewUiModel.FOLDERS,
                userSetHomeView = DefaultFilterUiModel.FOLDERS,
            )

        val result = validator.validated(preferences)

        assertThat(result.lastUsedHomeView).isEqualTo(HomeDisplayViewUiModel.ALL_ITEMS)
        assertThat(result.userSetHomeView).isEqualTo(DefaultFilterUiModel.ALL_ITEMS)
    }

    @Test
    fun `validated falls back to all items when tags are denied by rbac`() {
        stubFeatureFlags(areFoldersAvailable = true, areTagsAvailable = true)
        stubRbac(foldersUseRule = ALLOW, tagsUseRule = DENY)
        val preferences =
            HomeDisplayViewPreferencesUiModel(
                lastUsedHomeView = HomeDisplayViewUiModel.TAGS,
                userSetHomeView = DefaultFilterUiModel.TAGS,
            )

        val result = validator.validated(preferences)

        assertThat(result.lastUsedHomeView).isEqualTo(HomeDisplayViewUiModel.ALL_ITEMS)
        assertThat(result.userSetHomeView).isEqualTo(DefaultFilterUiModel.ALL_ITEMS)
    }

    @Test
    fun `validated does not touch views unrelated to folders and tags`() {
        stubFeatureFlags(areFoldersAvailable = false, areTagsAvailable = false)
        stubRbac(foldersUseRule = DENY, tagsUseRule = DENY)
        val preferences =
            HomeDisplayViewPreferencesUiModel(
                lastUsedHomeView = HomeDisplayViewUiModel.FAVOURITES,
                userSetHomeView = DefaultFilterUiModel.OWNED_BY_ME,
            )

        val result = validator.validated(preferences)

        assertThat(result).isEqualTo(preferences)
    }

    @Test
    fun `validatedDefaultFiltersList contains all filters when available and allowed`() {
        stubFeatureFlags(areFoldersAvailable = true, areTagsAvailable = true)
        stubRbac(foldersUseRule = ALLOW, tagsUseRule = ALLOW)

        val result = validator.validatedDefaultFiltersList()

        assertThat(result).containsExactlyElementsIn(DefaultFilterUiModel.entries)
    }

    @Test
    fun `validatedDefaultFiltersList removes folders and tags when feature flags are off`() {
        stubFeatureFlags(areFoldersAvailable = false, areTagsAvailable = false)
        stubRbac(foldersUseRule = ALLOW, tagsUseRule = ALLOW)

        val result = validator.validatedDefaultFiltersList()

        assertThat(result).doesNotContain(DefaultFilterUiModel.FOLDERS)
        assertThat(result).doesNotContain(DefaultFilterUiModel.TAGS)
        assertThat(result).containsExactlyElementsIn(
            DefaultFilterUiModel.entries - listOf(DefaultFilterUiModel.FOLDERS, DefaultFilterUiModel.TAGS),
        )
    }

    @Test
    fun `validatedDefaultFiltersList removes folders and tags when denied by rbac`() {
        stubFeatureFlags(areFoldersAvailable = true, areTagsAvailable = true)
        stubRbac(foldersUseRule = DENY, tagsUseRule = DENY)

        val result = validator.validatedDefaultFiltersList()

        assertThat(result).doesNotContain(DefaultFilterUiModel.FOLDERS)
        assertThat(result).doesNotContain(DefaultFilterUiModel.TAGS)
    }

    private fun stubFeatureFlags(
        areFoldersAvailable: Boolean,
        areTagsAvailable: Boolean,
    ) {
        getFeatureFlagsUseCase.stub {
            on { execute(Unit) }.thenReturn(
                GetFeatureFlagsUseCase.Output(
                    FeatureFlagsModel(
                        privacyPolicyUrl = null,
                        termsAndConditionsUrl = null,
                        isPreviewPasswordAvailable = true,
                        areFoldersAvailable = areFoldersAvailable,
                        areTagsAvailable = areTagsAvailable,
                        isTotpAvailable = true,
                        isRbacAvailable = true,
                        isPasswordExpiryAvailable = true,
                        arePasswordPoliciesAvailable = true,
                        canUpdatePasswordPolicies = true,
                        isV5MetadataAvailable = false,
                    ),
                ),
            )
        }
    }

    private fun stubRbac(
        foldersUseRule: RbacRuleModel,
        tagsUseRule: RbacRuleModel,
    ) {
        getRbacRulesUseCase.stub {
            on { execute(Unit) }.thenReturn(
                GetRbacRulesUseCase.Output(
                    RbacModel(
                        passwordPreviewRule = ALLOW,
                        passwordCopyRule = ALLOW,
                        tagsUseRule = tagsUseRule,
                        shareViewRule = ALLOW,
                        foldersUseRule = foldersUseRule,
                    ),
                ),
            )
        }
    }
}
