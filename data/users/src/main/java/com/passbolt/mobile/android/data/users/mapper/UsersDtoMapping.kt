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
import net.svaroh.passly.domain.users.model.UserProfile
import net.svaroh.passly.dto.response.GpgKeyDto
import net.svaroh.passly.dto.response.UserDto
import java.time.ZonedDateTime

fun List<UserDto>.toDomain(): List<UserProfile> =
    filter { it.active && !it.deleted }
        .map { it.toDomain() }

fun UserDto.toDomain(): UserProfile =
    UserProfile(
        id = id.toString(),
        username = username,
        // if the disabled date is in the past the user is disabled
        disabled = disabled?.let { ZonedDateTime.parse(it).isBefore(ZonedDateTime.now()) } ?: false,
        role = role?.name,
        firstName = profile?.firstName,
        lastName = profile?.lastName,
        avatarUrl =
            profile
                ?.avatar
                ?.url
                ?.medium,
        gpgKey = gpgKey?.toDomain(),
    )

fun GpgKeyDto.toDomain(): GpgKey =
    GpgKey(
        id = id.toString(),
        armoredKey = armoredKey,
        fingerprint = fingerprint,
        bits = bits,
        uid = uid,
        keyId = keyId,
        type = type,
        keyExpirationDate = expires?.let { ZonedDateTime.parse(it) },
        keyCreationDate = keyCreated?.let { ZonedDateTime.parse(it) },
    )
