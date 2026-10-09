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

package net.svaroh.passly.domain.secrets.usecase.decrypt

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.passphrasememorycache.PotentialPassphrase
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.privatekey.PrivateKeyRepository
import net.svaroh.passly.domain.privatekey.model.PrivateKey
import net.svaroh.passly.gopenpgp.OpenPgp
import net.svaroh.passly.gopenpgp.exception.OpenPgpError
import net.svaroh.passly.gopenpgp.exception.OpenPgpFailure
import net.svaroh.passly.gopenpgp.exception.OpenPgpResult
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DecryptSecretUseCaseTest {
    private val openPgp = mock<OpenPgp>()
    private val passphraseMemoryCache = mock<PassphraseMemoryCache>()
    private val getSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
    private val privateKeyRepository = mock<PrivateKeyRepository>()

    private val useCase =
        DecryptSecretUseCase(
            gopenPgp = openPgp,
            passphraseMemoryCache = passphraseMemoryCache,
            getSelectedAccountUseCase = getSelectedAccountUseCase,
            privateKeyRepository = privateKeyRepository,
        )

    @Before
    fun setUp() {
        whenever(getSelectedAccountUseCase.execute(Unit)) doReturn GetSelectedAccountUseCase.Output("userId")
        whenever(privateKeyRepository.getPrivateKey("userId")) doReturn PrivateKey("privateKey")
    }

    @Test
    fun `secret is decrypted and the cached passphrase copy is wiped afterwards`() =
        runTest {
            val cachedPassphraseCopy = "passphrase".toByteArray()
            whenever(passphraseMemoryCache.get()) doReturn PotentialPassphrase.Passphrase(cachedPassphraseCopy)
            whenever(openPgp.decryptMessageArmored(any(), any(), any())) doReturn OpenPgpResult.Result("secret")

            val output = useCase.execute(DecryptSecretUseCase.Input("encrypted"))

            assertThat(output).isInstanceOf(DecryptSecretUseCase.Output.DecryptedSecret::class.java)
            assertThat((output as DecryptSecretUseCase.Output.DecryptedSecret).decryptedSecret).isEqualTo("secret")
            assertThat(cachedPassphraseCopy.all { it == 0.toByte() }).isTrue()
        }

    @Test
    fun `cached passphrase copy is wiped also when decryption fails`() =
        runTest {
            val cachedPassphraseCopy = "passphrase".toByteArray()
            whenever(passphraseMemoryCache.get()) doReturn PotentialPassphrase.Passphrase(cachedPassphraseCopy)
            whenever(openPgp.decryptMessageArmored(any(), any(), any())) doReturn
                OpenPgpResult.Error(OpenPgpFailure.Generic(OpenPgpError("decryption error")))

            val output = useCase.execute(DecryptSecretUseCase.Input("encrypted"))

            assertThat(output).isInstanceOf(DecryptSecretUseCase.Output.Failure::class.java)
            assertThat(cachedPassphraseCopy.all { it == 0.toByte() }).isTrue()
        }
}
