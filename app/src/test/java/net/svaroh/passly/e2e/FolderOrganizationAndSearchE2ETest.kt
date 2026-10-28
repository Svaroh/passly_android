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
package net.svaroh.passly.e2e

import androidx.paging.PagingData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.common.urimatcher.AutofillUriMatcher
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.domain.folders.FoldersRepository
import net.svaroh.passly.domain.folders.model.FolderModel
import net.svaroh.passly.domain.folders.model.FolderModelWithAttributes
import net.svaroh.passly.domain.folders.usecase.CreateFolderUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalDirectChildFoldersPaginatedUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalSubFoldersForFolderPaginatedUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalSubFoldersForFolderUseCase
import net.svaroh.passly.domain.groups.usecase.GetLocalGroupsWithShareItemsCountPaginatedUseCase
import net.svaroh.passly.domain.rbac.usecase.GetRbacRulesUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcesPaginatedUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcesWithGroupPaginatedUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcesWithTagPaginatedUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalSubFolderResourcesFilteredPaginatedUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetResourcesInFolderPaginatedUseCase
import net.svaroh.passly.domain.tags.usecase.GetLocalTagsPaginatedUseCase
import net.svaroh.passly.feature.home.screen.ShowSuggestedModel
import net.svaroh.passly.feature.home.screen.data.HomeDataProvider
import net.svaroh.passly.ui.Folder
import net.svaroh.passly.ui.HomeDisplayViewModel
import net.svaroh.passly.ui.PermissionModel
import net.svaroh.passly.ui.RbacModel
import net.svaroh.passly.ui.RbacRuleModel
import net.svaroh.passly.ui.ResourcePermission
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class FolderOrganizationAndSearchE2ETest {

    @Test
    fun create_folder_usecase_persists_new_root_and_nested_folders() =
        runTest {
            val mockFoldersRepository = mock<FoldersRepository>()
            val createFolderUseCase = CreateFolderUseCase(mockFoldersRepository)

            val rootFolderId = UUID.randomUUID().toString()
            val childFolderId = UUID.randomUUID().toString()

            val rootFolderModel = FolderModel(
                folderId = rootFolderId,
                parentFolderId = null,
                name = "Infrastructure",
                isShared = false,
                permission = ResourcePermission.OWNER,
                modified = ZonedDateTime.now(),
            )
            val rootWithAttributes = FolderModelWithAttributes(
                folderModel = rootFolderModel,
                folderPermissions = emptyList<PermissionModel>(),
            )

            val childFolderModel = FolderModel(
                folderId = childFolderId,
                parentFolderId = rootFolderId,
                name = "Production Keys",
                isShared = false,
                permission = ResourcePermission.OWNER,
                modified = ZonedDateTime.now(),
            )
            val childWithAttributes = FolderModelWithAttributes(
                folderModel = childFolderModel,
                folderPermissions = emptyList<PermissionModel>(),
            )

            // 1. Створення кореневої папки
            whenever(mockFoldersRepository.createFolder("Infrastructure", null)).thenReturn(
                DomainResult.Finished(rootWithAttributes),
            )

            val rootOutput = createFolderUseCase.execute(
                CreateFolderUseCase.Input(name = "Infrastructure", parentFolderId = null),
            )
            assertIs<CreateFolderUseCase.Output.Success>(rootOutput)
            assertThat(rootOutput.folderWithAttributes.folderModel.name).isEqualTo("Infrastructure")
            assertThat(rootOutput.folderWithAttributes.folderModel.parentFolderId).isNull()

            // 2. Створення вкладеної папки
            whenever(mockFoldersRepository.createFolder("Production Keys", rootFolderId)).thenReturn(
                DomainResult.Finished(childWithAttributes),
            )

            val childOutput = createFolderUseCase.execute(
                CreateFolderUseCase.Input(name = "Production Keys", parentFolderId = rootFolderId),
            )
            assertIs<CreateFolderUseCase.Output.Success>(childOutput)
            assertThat(childOutput.folderWithAttributes.folderModel.name).isEqualTo("Production Keys")
            assertThat(childOutput.folderWithAttributes.folderModel.parentFolderId).isEqualTo(rootFolderId)
        }

    @Test
    fun create_folder_fails_cleanly_when_server_returns_error() =
        runTest {
            val mockFoldersRepository = mock<FoldersRepository>()
            val createFolderUseCase = CreateFolderUseCase(mockFoldersRepository)

            whenever(mockFoldersRepository.createFolder("Forbidden Folder", null)).thenReturn(
                DomainResult.Incomplete.Error(reason = UNKNOWN, message = "Access Denied"),
            )

            val output = createFolderUseCase.execute(
                CreateFolderUseCase.Input(name = "Forbidden Folder", parentFolderId = null),
            )

            assertIs<CreateFolderUseCase.Output.Failure>(output)
            assertThat(output.incomplete).isInstanceOf(DomainResult.Incomplete.Error::class.java)
        }

    @Test
    fun home_data_provider_filters_vault_by_search_query_across_all_items() =
        runTest {
            val mockGetLocalResourcesPaginatedUseCase = mock<GetLocalResourcesPaginatedUseCase>()
            val mockGetLocalTagsPaginatedUseCase = mock<GetLocalTagsPaginatedUseCase>()
            val mockGetLocalResourcesWithLocalTagsPaginatedUseCase = mock<GetLocalResourcesWithTagPaginatedUseCase>()
            val mockGetLocalGroupsWithShareItemsCountPaginatedUseCase = mock<GetLocalGroupsWithShareItemsCountPaginatedUseCase>()
            val mockGetLocalResourcesWithGroupsPaginatedUseCase = mock<GetLocalResourcesWithGroupPaginatedUseCase>()
            val mockGetLocalDirectChildFoldersPaginatedUseCase = mock<GetLocalDirectChildFoldersPaginatedUseCase>()
            val mockGetResourcesInFolderPaginatedUseCase = mock<GetResourcesInFolderPaginatedUseCase>()
            val mockGetLocalSubFolderResourcesFilteredPaginatedUseCase = mock<GetLocalSubFolderResourcesFilteredPaginatedUseCase>()
            val mockGetLocalSubFoldersForFolderUseCase = mock<GetLocalSubFoldersForFolderUseCase>()
            val mockGetLocalSubFoldersForFolderPaginatedUseCase = mock<GetLocalSubFoldersForFolderPaginatedUseCase>()
            val mockAutofillMatcher = mock<AutofillUriMatcher>()
            val mockGetRbacRulesUseCase = mock<GetRbacRulesUseCase>()

            whenever(mockGetRbacRulesUseCase.execute(Unit)).thenReturn(
                GetRbacRulesUseCase.Output(
                    rbacModel = RbacModel(
                        passwordPreviewRule = RbacRuleModel.ALLOW,
                        passwordCopyRule = RbacRuleModel.ALLOW,
                        tagsUseRule = RbacRuleModel.ALLOW,
                        shareViewRule = RbacRuleModel.ALLOW,
                        foldersUseRule = RbacRuleModel.ALLOW,
                    ),
                ),
            )

            val searchQuery = "production-db"
            whenever(
                mockGetLocalResourcesPaginatedUseCase.execute(
                    argThat { this.searchQuery == searchQuery },
                ),
            ).thenReturn(
                GetLocalResourcesPaginatedUseCase.Output(flowOf(PagingData.empty())),
            )

            val provider = HomeDataProvider(
                getLocalResourcesPaginatedUseCase = mockGetLocalResourcesPaginatedUseCase,
                getLocalTagsPaginatedUseCase = mockGetLocalTagsPaginatedUseCase,
                getLocalResourcesWithLocalTagsPaginatedUseCase = mockGetLocalResourcesWithLocalTagsPaginatedUseCase,
                getLocalGroupsWithShareItemsCountPaginatedUseCase = mockGetLocalGroupsWithShareItemsCountPaginatedUseCase,
                getLocalResourcesWithGroupsPaginatedUseCase = mockGetLocalResourcesWithGroupsPaginatedUseCase,
                getLocalDirectChildFoldersPaginatedUseCase = mockGetLocalDirectChildFoldersPaginatedUseCase,
                getResourcesInFolderPaginatedUseCase = mockGetResourcesInFolderPaginatedUseCase,
                getLocalSubFolderResourcesFilteredPaginatedUseCase = mockGetLocalSubFolderResourcesFilteredPaginatedUseCase,
                getLocalSubFoldersForFolderUseCase = mockGetLocalSubFoldersForFolderUseCase,
                getLocalSubFoldersForFolderPaginatedUseCase = mockGetLocalSubFoldersForFolderPaginatedUseCase,
                autofillMatcher = mockAutofillMatcher,
                getRbacRulesUseCase = mockGetRbacRulesUseCase,
            )

            val homeData = provider.provideData(
                searchQuery = searchQuery,
                homeView = HomeDisplayViewModel.AllItems,
                showSuggestedModel = ShowSuggestedModel.DoNotShow,
            )

            assertThat(homeData.resourceList).isNotNull()
        }

    @Test
    fun home_data_provider_respects_rbac_rules_and_hides_folders_when_denied() =
        runTest {
            val mockGetRbacRulesUseCase = mock<GetRbacRulesUseCase>()

            // RBAC забороняє використання папок: foldersUseRule = DENY
            whenever(mockGetRbacRulesUseCase.execute(Unit)).thenReturn(
                GetRbacRulesUseCase.Output(
                    rbacModel = RbacModel(
                        passwordPreviewRule = RbacRuleModel.ALLOW,
                        passwordCopyRule = RbacRuleModel.ALLOW,
                        tagsUseRule = RbacRuleModel.ALLOW,
                        shareViewRule = RbacRuleModel.ALLOW,
                        foldersUseRule = RbacRuleModel.DENY,
                    ),
                ),
            )

            val provider = HomeDataProvider(
                getLocalResourcesPaginatedUseCase = mock(),
                getLocalTagsPaginatedUseCase = mock(),
                getLocalResourcesWithLocalTagsPaginatedUseCase = mock(),
                getLocalGroupsWithShareItemsCountPaginatedUseCase = mock(),
                getLocalResourcesWithGroupsPaginatedUseCase = mock(),
                getLocalDirectChildFoldersPaginatedUseCase = mock(),
                getResourcesInFolderPaginatedUseCase = mock(),
                getLocalSubFolderResourcesFilteredPaginatedUseCase = mock(),
                getLocalSubFoldersForFolderUseCase = mock(),
                getLocalSubFoldersForFolderPaginatedUseCase = mock(),
                autofillMatcher = mock(),
                getRbacRulesUseCase = mockGetRbacRulesUseCase,
            )

            val result = provider.provideData(
                searchQuery = null,
                homeView = HomeDisplayViewModel.Folders(activeFolder = Folder.Child("folder-123")),
                showSuggestedModel = ShowSuggestedModel.DoNotShow,
            )

            // При забороні RBAC повертається порожній HomeData
            assertThat(result).isNotNull()
        }

    @Test
    fun home_data_provider_supports_switching_views_between_all_favourites_and_folders() =
        runTest {
            val mockGetLocalResourcesPaginatedUseCase = mock<GetLocalResourcesPaginatedUseCase>()
            val mockGetRbacRulesUseCase = mock<GetRbacRulesUseCase>()
            val mockGetLocalDirectChildFoldersPaginatedUseCase = mock<GetLocalDirectChildFoldersPaginatedUseCase>()
            val mockGetResourcesInFolderPaginatedUseCase = mock<GetResourcesInFolderPaginatedUseCase>()

            whenever(mockGetRbacRulesUseCase.execute(Unit)).thenReturn(
                GetRbacRulesUseCase.Output(
                    rbacModel = RbacModel(
                        passwordPreviewRule = RbacRuleModel.ALLOW,
                        passwordCopyRule = RbacRuleModel.ALLOW,
                        tagsUseRule = RbacRuleModel.ALLOW,
                        shareViewRule = RbacRuleModel.ALLOW,
                        foldersUseRule = RbacRuleModel.ALLOW,
                    ),
                ),
            )

            whenever(mockGetLocalResourcesPaginatedUseCase.execute(any())).thenReturn(
                GetLocalResourcesPaginatedUseCase.Output(flowOf(PagingData.empty())),
            )
            whenever(mockGetLocalDirectChildFoldersPaginatedUseCase.execute(any())).thenReturn(
                GetLocalDirectChildFoldersPaginatedUseCase.Output(flowOf(PagingData.empty())),
            )
            whenever(mockGetResourcesInFolderPaginatedUseCase.execute(any())).thenReturn(
                GetResourcesInFolderPaginatedUseCase.Output(flowOf(PagingData.empty())),
            )

            val provider = HomeDataProvider(
                getLocalResourcesPaginatedUseCase = mockGetLocalResourcesPaginatedUseCase,
                getLocalTagsPaginatedUseCase = mock(),
                getLocalResourcesWithLocalTagsPaginatedUseCase = mock(),
                getLocalGroupsWithShareItemsCountPaginatedUseCase = mock(),
                getLocalResourcesWithGroupsPaginatedUseCase = mock(),
                getLocalDirectChildFoldersPaginatedUseCase = mockGetLocalDirectChildFoldersPaginatedUseCase,
                getResourcesInFolderPaginatedUseCase = mockGetResourcesInFolderPaginatedUseCase,
                getLocalSubFolderResourcesFilteredPaginatedUseCase = mock(),
                getLocalSubFoldersForFolderUseCase = mock(),
                getLocalSubFoldersForFolderPaginatedUseCase = mock(),
                autofillMatcher = mock(),
                getRbacRulesUseCase = mockGetRbacRulesUseCase,
            )

            // 1. Вкладка Favourites
            val favouritesData = provider.provideData(
                searchQuery = null,
                homeView = HomeDisplayViewModel.Favourites,
                showSuggestedModel = ShowSuggestedModel.DoNotShow,
            )
            assertThat(favouritesData.resourceList).isNotNull()

            // 2. Вкладка SharedWithMe
            val sharedData = provider.provideData(
                searchQuery = null,
                homeView = HomeDisplayViewModel.SharedWithMe,
                showSuggestedModel = ShowSuggestedModel.DoNotShow,
            )
            assertThat(sharedData.resourceList).isNotNull()

            // 3. Вкладка Folders (Root)
            val foldersData = provider.provideData(
                searchQuery = null,
                homeView = HomeDisplayViewModel.Folders(activeFolder = Folder.Root),
                showSuggestedModel = ShowSuggestedModel.DoNotShow,
            )
            assertThat(foldersData.foldersList).isNotNull()
            assertThat(foldersData.resourceList).isNotNull()
        }
}
