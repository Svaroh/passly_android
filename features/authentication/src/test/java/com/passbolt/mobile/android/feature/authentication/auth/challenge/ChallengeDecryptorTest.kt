package net.svaroh.passly.feature.authentication.auth.challenge

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.domain.privatekey.model.PrivateKey
import net.svaroh.passly.gopenpgp.exception.OpenPgpError
import net.svaroh.passly.gopenpgp.exception.OpenPgpFailure
import net.svaroh.passly.gopenpgp.exception.OpenPgpResult
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ChallengeDecryptorTest : KoinTest {
    private val challengeDecryptor: ChallengeDecryptor by inject()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            modules(challengeTestModule)
        }

    @Test
    fun `challenge properly decrypted`() =
        runTest {
            val privateKey = "private_key"
            val publicKey = "public_key"
            val challenge =
                "{version: \"1.0\", domain: \"domain\", verify_token: \"verify_token\"," +
                    " access_token: \"access_token\", refresh_token: \"refresh_token\"}"
            whenever(privateKeyRepository.getPrivateKey(any())).thenReturn(PrivateKey(privateKey))
            whenever(openPgp.decryptVerifyMessageArmored(eq(publicKey), eq(privateKey), any(), any())).thenReturn(
                OpenPgpResult.Result(challenge),
            )

            val result =
                challengeDecryptor.decrypt(
                    publicKey,
                    "pass".toByteArray(),
                    "userId",
                    "challenge",
                )
            assertThat(result).isInstanceOf(ChallengeDecryptor.Output.DecryptedChallenge::class.java)
            val decryptedChallenge = (result as ChallengeDecryptor.Output.DecryptedChallenge).challenge
            assertEquals("access_token", decryptedChallenge.accessToken)
            assertEquals("verify_token", decryptedChallenge.verifyToken)
            assertEquals("refresh_token", decryptedChallenge.refreshToken)
            assertEquals("domain", decryptedChallenge.domain)
            assertEquals("1.0", decryptedChallenge.version)
        }

    @Test
    fun `challenge value is correct when decryption failure`() =
        runTest {
            val privateKey = "private_key"
            val publicKey = "public_key"
            val errorMessage = "message"
            whenever(privateKeyRepository.getPrivateKey(any())).thenReturn(PrivateKey(privateKey))
            whenever(openPgp.decryptVerifyMessageArmored(any(), any(), any(), any()))
                .thenReturn(OpenPgpResult.Error(OpenPgpFailure.Generic(OpenPgpError(errorMessage))))

            val result =
                challengeDecryptor.decrypt(
                    publicKey,
                    "pass".toByteArray(),
                    "userId",
                    "challenge",
                )
            assertThat(result).isInstanceOf(ChallengeDecryptor.Output.DecryptionError::class.java)
            val decryptionError = (result as ChallengeDecryptor.Output.DecryptionError)
            assertThat(decryptionError.message).isEqualTo(errorMessage)
        }

    @Test
    fun `challenge maps to server signature invalid when signature verification fails`() =
        runTest {
            val privateKey = "private_key"
            val publicKey = "public_key"
            val errorMessage = "signature is invalid"
            whenever(privateKeyRepository.getPrivateKey(any())).thenReturn(PrivateKey(privateKey))
            whenever(openPgp.decryptVerifyMessageArmored(any(), any(), any(), any()))
                .thenReturn(
                    OpenPgpResult.Error(OpenPgpFailure.SignatureVerificationFailed(OpenPgpError(errorMessage))),
                )

            val result =
                challengeDecryptor.decrypt(
                    publicKey,
                    "pass".toByteArray(),
                    "userId",
                    "challenge",
                )
            assertThat(result).isInstanceOf(ChallengeDecryptor.Output.ServerSignatureInvalid::class.java)
            val signatureError = (result as ChallengeDecryptor.Output.ServerSignatureInvalid)
            assertThat(signatureError.message).isEqualTo(errorMessage)
        }

    @Test
    fun `passphrase copy is wiped after use on success and failure but caller array is intact`() =
        runTest {
            val privateKey = "private_key"
            val publicKey = "public_key"
            val challenge =
                "{version: \"1.0\", domain: \"domain\", verify_token: \"verify_token\"," +
                    " access_token: \"access_token\", refresh_token: \"refresh_token\"}"
            val passphrasesPassedToPgp = mutableListOf<ByteArray>()
            val passphraseContentsAtPgpCall = mutableListOf<ByteArray>()

            whenever(privateKeyRepository.getPrivateKey(any())).thenReturn(PrivateKey(privateKey))
            whenever(openPgp.decryptVerifyMessageArmored(eq(publicKey), eq(privateKey), any(), any()))
                .thenAnswer { invocation ->
                    val pgpPassphraseInput = invocation.arguments[2] as ByteArray
                    passphrasesPassedToPgp += pgpPassphraseInput
                    passphraseContentsAtPgpCall += pgpPassphraseInput.copyOf()
                    OpenPgpResult.Result(challenge)
                }
            val callerPassphrase = "pass".toByteArray()

            challengeDecryptor.decrypt(publicKey, callerPassphrase, "userId", "challenge")

            whenever(openPgp.decryptVerifyMessageArmored(eq(publicKey), eq(privateKey), any(), any()))
                .thenAnswer { invocation ->
                    val pgpPassphraseInput = invocation.arguments[2] as ByteArray
                    passphrasesPassedToPgp += pgpPassphraseInput
                    passphraseContentsAtPgpCall += pgpPassphraseInput.copyOf()
                    OpenPgpResult.Error(OpenPgpFailure.Generic(OpenPgpError("error")))
                }

            challengeDecryptor.decrypt(publicKey, callerPassphrase, "userId", "challenge")

            assertThat(passphraseContentsAtPgpCall).hasSize(2)
            assertThat(passphraseContentsAtPgpCall.all { it.contentEquals("pass".toByteArray()) }).isTrue()
            assertThat(passphrasesPassedToPgp.all { pgpInput -> pgpInput.all { it == 0.toByte() } }).isTrue()
            assertThat(callerPassphrase).isEqualTo("pass".toByteArray())
        }
}
