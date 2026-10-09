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

package net.svaroh.passly.data.users.mapper

import net.svaroh.passly.domain.users.model.GpgKey
import net.svaroh.passly.entity.user.User
import net.svaroh.passly.entity.user.UserGpgKey
import net.svaroh.passly.entity.user.UserUpdateState
import net.svaroh.passly.domain.users.model.UserProfile as DomainUserProfile
import net.svaroh.passly.entity.user.UserProfile as UserProfileEntity

internal fun User.toDomain(): DomainUserProfile =
    DomainUserProfile(
        id = id,
        username = userName,
        disabled = disabled,
        role = null,
        firstName = profile.firstName,
        lastName = profile.lastName,
        avatarUrl = profile.avatarUrl,
        gpgKey = gpgKey.toDomain(),
    )

internal fun UserGpgKey.toDomain(): GpgKey =
    GpgKey(
        id = id,
        armoredKey = armoredKey,
        fingerprint = fingerprint,
        bits = bits,
        uid = uid,
        keyId = keyId,
        type = type,
        keyExpirationDate = expires,
        keyCreationDate = created,
    )

internal fun DomainUserProfile.toEntity(): User =
    User(
        id = id,
        userName = username,
        disabled = disabled,
        profile =
            UserProfileEntity(
                firstName = firstName,
                lastName = lastName,
                avatarUrl = avatarUrl,
            ),
        gpgKey = requireNotNull(gpgKey).toEntity(),
        updateState = UserUpdateState.UPDATED,
    )

internal fun GpgKey.toEntity(): UserGpgKey =
    UserGpgKey(
        id = id,
        armoredKey = armoredKey,
        bits = bits,
        uid = uid,
        keyId = keyId,
        fingerprint = fingerprint,
        type = type,
        expires = keyExpirationDate,
        created = keyCreationDate,
    )
