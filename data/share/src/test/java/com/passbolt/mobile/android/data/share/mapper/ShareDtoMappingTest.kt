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

package net.svaroh.passly.data.share.mapper

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.domain.share.model.EncryptedSecret
import net.svaroh.passly.domain.share.model.SharePermission
import net.svaroh.passly.dto.response.ShareChangeUser
import net.svaroh.passly.dto.response.ShareChangesDto
import net.svaroh.passly.dto.response.ShareRecipientDto
import net.svaroh.passly.dto.response.SimulateShareResponse
import org.junit.Test
import java.util.UUID
import net.svaroh.passly.dto.request.SharePermission as SharePermissionDto

class ShareDtoMappingTest {
    @Test
    fun `new permission maps to dto with the is-new flag set`() {
        val dto = SharePermission.NewSharePermission("User", "u1", "Resource", "r1", 1).toDto()

        assertThat(dto).isInstanceOf(SharePermissionDto.NewSharePermission::class.java)
        dto as SharePermissionDto.NewSharePermission
        assertThat(dto.isNew).isTrue()
        assertThat(dto.aro).isEqualTo("User")
        assertThat(dto.aroForeignKey).isEqualTo("u1")
        assertThat(dto.aco).isEqualTo("Resource")
        assertThat(dto.acoForeignKey).isEqualTo("r1")
        assertThat(dto.type).isEqualTo(1)
    }

    @Test
    fun `updated permission maps to dto preserving the permission id`() {
        val dto = SharePermission.UpdatedSharePermission("perm-1", "User", "u1", "Resource", "r1", 7).toDto()

        assertThat(dto).isInstanceOf(SharePermissionDto.UpdatedSharePermission::class.java)
        dto as SharePermissionDto.UpdatedSharePermission
        assertThat(dto.id).isEqualTo("perm-1")
        assertThat(dto.aroForeignKey).isEqualTo("u1")
        assertThat(dto.type).isEqualTo(7)
    }

    @Test
    fun `deleted permission maps to dto with the delete flag set`() {
        val dto = SharePermission.DeletedSharePermission("perm-2", "Group", "g1", "Folder", "f1", 1).toDto()

        assertThat(dto).isInstanceOf(SharePermissionDto.DeletedSharePermission::class.java)
        dto as SharePermissionDto.DeletedSharePermission
        assertThat(dto.delete).isTrue()
        assertThat(dto.id).isEqualTo("perm-2")
        assertThat(dto.aroForeignKey).isEqualTo("g1")
    }

    @Test
    fun `encrypted secret maps to dto`() {
        val dto = EncryptedSecret(resourceId = "r1", userId = "u1", data = "cipher").toDto()

        assertThat(dto.resourceId).isEqualTo("r1")
        assertThat(dto.userId).isEqualTo("u1")
        assertThat(dto.data).isEqualTo("cipher")
    }

    @Test
    fun `simulate response maps recipient user ids to domain changes`() {
        val addedId = UUID.randomUUID()
        val removedId = UUID.randomUUID()
        val response =
            SimulateShareResponse(
                ShareChangesDto(
                    added = listOf(ShareRecipientDto(ShareChangeUser(addedId))),
                    removed = listOf(ShareRecipientDto(ShareChangeUser(removedId))),
                ),
            )

        val changes = response.toDomain()

        assertThat(changes.added.map { it.userId }).containsExactly(addedId.toString())
        assertThat(changes.removed.map { it.userId }).containsExactly(removedId.toString())
    }
}
