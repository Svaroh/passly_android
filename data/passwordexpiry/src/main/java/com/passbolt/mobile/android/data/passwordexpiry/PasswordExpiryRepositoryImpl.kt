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

package net.svaroh.passly.data.passwordexpiry

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.domain.passwordexpiry.PasswordExpiryLocalDataSource
import net.svaroh.passly.domain.passwordexpiry.PasswordExpiryRemoteDataSource
import net.svaroh.passly.domain.passwordexpiry.PasswordExpiryRepository
import net.svaroh.passly.domain.passwordexpiry.model.PasswordExpirySettings

internal class PasswordExpiryRepositoryImpl(
    private val memoryDataSource: PasswordExpiryLocalDataSource,
    private val remoteDataSource: PasswordExpiryRemoteDataSource,
) : PasswordExpiryRepository {
    override suspend fun getPasswordExpirySettings(userId: String): DomainResult<PasswordExpirySettings> =
        when (val memory = memoryDataSource.getPasswordExpirySettings(userId)) {
            is DomainResult.Finished -> memory
            is DomainResult.Incomplete ->
                remoteDataSource.getPasswordExpirySettings().also {
                    if (it is DomainResult.Finished) {
                        setPasswordExpirySettings(userId, it.value)
                    }
                }
        }

    override suspend fun setPasswordExpirySettings(
        userId: String,
        passwordExpirySettings: PasswordExpirySettings,
    ) {
        memoryDataSource.setPasswordExpirySettings(userId, passwordExpirySettings)
    }
}
