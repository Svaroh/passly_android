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
package net.svaroh.passly.core.accounts.usecase.biometrickey

import net.svaroh.passly.common.usecase.UseCase
import net.svaroh.passly.core.accounts.BiometricKeyIvFileName
import net.svaroh.passly.core.accounts.usecase.accounts.GetAllAccountsDataUseCase
import net.svaroh.passly.encryptedstorage.EncryptedSharedPreferencesFactory

class RemoveAllBiometricKeyIvUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
    private val getAllAccountsDataUseCase: GetAllAccountsDataUseCase,
) : UseCase<Unit, Unit> {
    override fun execute(input: Unit) {
        getAllAccountsDataUseCase
            .execute(Unit)
            .accounts
            .mapNotNull { it.userId }
            .map { BiometricKeyIvFileName(it).name }
            .forEach { fileName ->
                encryptedSharedPreferencesFactory
                    .get(fileName)
                    .edit()
                    .clear()
                    .apply()
            }
    }
}
