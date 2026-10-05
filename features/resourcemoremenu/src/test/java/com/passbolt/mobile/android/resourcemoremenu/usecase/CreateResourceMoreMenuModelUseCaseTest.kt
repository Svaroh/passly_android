/**
 * Passly - Open source password manager for teams
 * Copyright (c) 2026 Svaroh
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Svaroh
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://passly.svaroh.net Passly
 * @since v1.0
 */
package net.svaroh.passly.resourcemoremenu.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.domain.rbac.usecase.GetRbacRulesUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.jsonmodel.jsonModelModule
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.ui.MetadataJsonModel
import net.svaroh.passly.ui.RbacModel
import net.svaroh.passly.ui.RbacRuleModel.ALLOW
import net.svaroh.passly.ui.ResourcePermission
import net.svaroh.passly.ui.ResourceUiModel
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.time.ZonedDateTime
import java.util.UUID

class CreateResourceMoreMenuModelUseCaseTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(jsonModelModule)
        }

    private val getLocalResourceUseCase = mock<GetLocalResourceUseCase>()
    private val getRbacRulesUseCase = mock<GetRbacRulesUseCase>()
    private val useCase =
        CreateResourceMoreMenuModelUseCase(
            getLocalResourceUseCase,
            getRbacRulesUseCase,
        )

    @Test
    fun `edit action should be hidden for passkey resources`() =
        runTest {
            getLocalResourceUseCase.stub {
                onBlocking { execute(GetLocalResourceUseCase.Input(RESOURCE_ID)) } doReturn
                    GetLocalResourceUseCase.Output(
                        resourceModel(
                            slug = ContentType.V5Passkey.slug,
                            permission = ResourcePermission.OWNER,
                        ),
                    )
            }
            getRbacRulesUseCase.stub {
                onBlocking { execute(Unit) } doReturn GetRbacRulesUseCase.Output(ALLOW_ALL_RBAC)
            }

            val model = useCase.execute(CreateResourceMoreMenuModelUseCase.Input(RESOURCE_ID)).resourceMenuModel

            assertThat(model.canCopy).isFalse()
            assertThat(model.canDelete).isTrue()
            assertThat(model.canEdit).isFalse()
            assertThat(model.canShare).isTrue()
            assertThat(model.descriptionOptions).isEmpty()
        }

    private fun resourceModel(
        slug: String,
        permission: ResourcePermission,
    ) = ResourceUiModel(
        resourceId = RESOURCE_ID,
        resourceTypeId = UUID.randomUUID().toString(),
        slug = slug,
        folderId = null,
        permission = permission,
        favouriteId = null,
        modified = ZonedDateTime.now(),
        expiry = null,
        metadataKeyId = null,
        metadataKeyType = null,
        metadataJsonModel =
            MetadataJsonModel(
                """
                {
                    "object_type": "PASSBOLT_RESOURCE_METADATA",
                    "name": "www.passkeys.io",
                    "uris": ["https://www.passkeys.io"],
                    "username": "ada@example.com"
                }
                """.trimIndent(),
            ),
    )

    private companion object {
        const val RESOURCE_ID = "resource-id"
        val ALLOW_ALL_RBAC =
            RbacModel(
                passwordPreviewRule = ALLOW,
                passwordCopyRule = ALLOW,
                tagsUseRule = ALLOW,
                shareViewRule = ALLOW,
                foldersUseRule = ALLOW,
            )
    }
}
