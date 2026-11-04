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
package net.svaroh.passly.core.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.common.transaction.DatabaseTransactionRunner
import net.svaroh.passly.core.sync.dto.SyncChangesDto
import net.svaroh.passly.core.sync.dto.SyncDeletionsDto
import net.svaroh.passly.core.sync.dto.SyncUpsertsDto
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.database.ResourceDatabase
import net.svaroh.passly.database.impl.folders.FoldersDao
import net.svaroh.passly.database.impl.groups.GroupsDao
import net.svaroh.passly.database.impl.resources.ResourcesDao
import net.svaroh.passly.database.impl.sync.SyncStateDao
import net.svaroh.passly.database.impl.users.UsersDao
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.resourcetypes.usecase.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.mappers.PermissionsModelMapper
import net.svaroh.passly.mappers.ResourceModelMapper
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ChangesApplierTest {
    private val databaseProvider: DatabaseProvider = mock()
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase = mock()
    private val databaseTransactionRunner: DatabaseTransactionRunner = mock()
    private val resourceModelMapper: ResourceModelMapper = mock()
    private val permissionsModelMapper: PermissionsModelMapper = mock()
    private val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider = mock()

    private val db: ResourceDatabase = mock()
    private val resourcesDao: ResourcesDao = mock()
    private val foldersDao: FoldersDao = mock()
    private val usersDao: UsersDao = mock()
    private val groupsDao: GroupsDao = mock()
    private val syncStateDao: SyncStateDao = mock()

    private val changesApplier = ChangesApplier(
        databaseProvider,
        getSelectedAccountUseCase,
        databaseTransactionRunner,
        resourceModelMapper,
        permissionsModelMapper,
        resourceTypeIdToSlugMappingProvider,
    )

    @Before
    fun setUp() = runTest {
        whenever(getSelectedAccountUseCase.execute(Unit)).thenReturn(
            GetSelectedAccountUseCase.Output(selectedAccount = "user-1"),
        )
        whenever(databaseProvider.get("user-1")).thenReturn(db)
        whenever(db.resourcesDao()).thenReturn(resourcesDao)
        whenever(db.foldersDao()).thenReturn(foldersDao)
        whenever(db.usersDao()).thenReturn(usersDao)
        whenever(db.groupsDao()).thenReturn(groupsDao)
        whenever(db.syncStateDao()).thenReturn(syncStateDao)
        whenever(resourceTypeIdToSlugMappingProvider.provideMappingForSelectedAccount()).thenReturn(emptyMap())

        whenever(databaseTransactionRunner.runInTransaction(any<suspend () -> Any?>())).thenAnswer {
            val block = it.getArgument<suspend () -> Any?>(0)
            kotlinx.coroutines.runBlocking { block() }
        }
    }

    @Test
    fun `applyChanges executes deletions and updates cursor atomically`() = runTest {
        val deletions = SyncDeletionsDto(
            resources = listOf("res-1", "res-2"),
            folders = listOf("folder-1"),
            users = listOf("user-deleted"),
            groups = listOf("group-deleted"),
        )
        val changes = SyncChangesDto(
            cursor = 100L,
            hasMore = false,
            upserts = SyncUpsertsDto(),
            deletions = deletions,
        )

        val stats = changesApplier.applyChanges(changes, epoch = "epoch-1")

        assertThat(stats.deleted).isEqualTo(5)
        verify(resourcesDao).deleteAll(listOf("res-1", "res-2"))
        verify(foldersDao).deleteAll(listOf("folder-1"))
        verify(usersDao).deleteAll(listOf("user-deleted"))
        verify(groupsDao).deleteAll(listOf("group-deleted"))
        verify(syncStateDao).upsert(any())
    }
}
