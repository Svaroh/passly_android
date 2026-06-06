package net.svaroh.passly.feature.authentication.auth.usecase

import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.domain.accounts.usecase.GetAccountDataUseCase
import net.svaroh.passly.domain.auth.model.SignInFailureType
import net.svaroh.passly.domain.auth.model.SignInResult
import net.svaroh.passly.domain.auth.usecase.GetSessionUseCase
import net.svaroh.passly.dto.response.ChallengeResponseDto
import net.svaroh.passly.feature.authentication.auth.challenge.ChallengeDecryptor
import net.svaroh.passly.feature.authentication.auth.challenge.ChallengeProvider
import net.svaroh.passly.feature.authentication.auth.challenge.ChallengeVerifier
import net.svaroh.passly.mappers.AccountModelMapper
import timber.log.Timber

class SignInVerifyInteractor(
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val challengeProvider: ChallengeProvider,
    private val getSessionUseCase: GetSessionUseCase,
    private val signInUseCase: SignInUseCase,
    private val challengeDecryptor: ChallengeDecryptor,
    private val challengeVerifier: ChallengeVerifier,
) {
    suspend fun signInVerify(
        serverPgpPublicKey: String,
        passphrase: ByteArray,
        userId: String,
        serverRsaKey: String,
        onError: (Error) -> Unit,
        onSuccess: suspend (Success) -> Unit,
    ) {
        Timber.d("Preparing sign in challenge")

        val configInput =
            SignInConfigInput(
                serverPgpPublicKey = serverPgpPublicKey,
                passphrase = passphrase,
                userId = userId,
                serverRsaKey = serverRsaKey,
            )

        val accountData = getAccountDataUseCase.execute(UserIdInput(userId))
        val challenge =
            challengeProvider.get(
                domain = accountData.url,
                serverPublicKey = serverPgpPublicKey,
                passphrase = passphrase,
                userId = userId,
            )

        when (challenge) {
            is ChallengeProvider.Output.Success -> {
                Timber.d("Prepared sign in challenge")
                sendSignInRequest(
                    input =
                        SendSignInRequestInput(
                            config = configInput,
                            challenge = challenge.challenge,
                            sentVerifyToken = challenge.verifyToken,
                            accountData = accountData,
                            serverId = requireNotNull(accountData.serverId),
                        ),
                    onError = onError,
                    onSuccess = onSuccess,
                )
            }
            ChallengeProvider.Output.WrongPassphrase -> {
                Timber.d("Error during preparing challenge - incorrect passphrase")
                onError(Error.IncorrectPassphrase)
            }
        }
    }

    private suspend fun sendSignInRequest(
        input: SendSignInRequestInput,
        onError: (Error) -> Unit,
        onSuccess: suspend (Success) -> Unit,
    ) {
        Timber.d("Signing in")
        val currentMfaToken = getSessionUseCase.execute(Unit).mfaToken

        val signInInput =
            SignInUseCase.Input(
                userId = input.serverId,
                challenge = input.challenge,
                mfaToken = currentMfaToken,
            )

        when (val result = signInUseCase.execute(signInInput)) {
            is SignInResult.Failure -> {
                Timber.e("Failure during sign in: ${result.message}")
                val error =
                    when (result.type) {
                        SignInFailureType.ACCOUNT_DOES_NOT_EXIST -> {
                            Error.AccountDoesNotExist(
                                label =
                                    input.accountData.label ?: AccountModelMapper.defaultLabel(
                                        input.accountData.firstName,
                                        input.accountData.lastName,
                                    ),
                                email = input.accountData.email,
                                serverUrl = input.accountData.url,
                            )
                        }
                        SignInFailureType.SERVER_NOT_REACHABLE -> {
                            Error.ServerNotReachable(input.accountData.url)
                        }
                        SignInFailureType.NO_NETWORK -> {
                            Error.NoNetwork
                        }
                        SignInFailureType.OTHER -> {
                            Error.SignInFailure(result.message)
                        }
                    }
                onError(error)
            }
            is SignInResult.Success -> {
                Timber.d("Sign in success")
                decryptChallenge(
                    input =
                        DecryptChallengeInput(
                            config = input.config,
                            currentMfaToken = currentMfaToken,
                            signInResult = result,
                            sentVerifyToken = input.sentVerifyToken,
                            sentDomain = input.accountData.url,
                        ),
                    onSuccess = onSuccess,
                    onError = onError,
                )
            }
        }
    }

    private suspend fun decryptChallenge(
        input: DecryptChallengeInput,
        onSuccess: suspend (Success) -> Unit,
        onError: (Error) -> Unit,
    ) {
        Timber.d("Decrypting challenge.")
        val challengeDecryptResult =
            challengeDecryptor.decrypt(
                serverPublicKey = input.config.serverPgpPublicKey,
                passphrase = input.config.passphrase,
                userId = input.config.userId,
                challenge = input.signInResult.challenge,
            )

        when (challengeDecryptResult) {
            is ChallengeDecryptor.Output.DecryptedChallenge -> {
                Timber.d("Challenge decrypted successfully")
                verifyChallenge(
                    input =
                        VerifyChallengeInput(
                            challengeResponseDto = challengeDecryptResult.challenge,
                            mfaToken = input.signInResult.mfaToken,
                            currentMfaToken = input.currentMfaToken,
                            rsaKey = input.config.serverRsaKey,
                            sentVerifyToken = input.sentVerifyToken,
                            sentDomain = input.sentDomain,
                        ),
                    onError = onError,
                    onSuccess = onSuccess,
                )
            }
            is ChallengeDecryptor.Output.DecryptionError -> {
                challengeDecryptResult.message.let {
                    Timber.e("Challenge decryption error: $it")
                    onError(Error.ChallengeDecryptionError(it))
                }
            }
            is ChallengeDecryptor.Output.ServerSignatureInvalid -> {
                Timber.e("Server signature on the challenge response is invalid: ${challengeDecryptResult.message}")
                onError(Error.ServerSignatureInvalid)
            }
        }
    }

    private suspend fun verifyChallenge(
        input: VerifyChallengeInput,
        onError: (Error) -> Unit,
        onSuccess: suspend (Success) -> Unit,
    ) {
        Timber.d("Verifying challenge")
        val verifyResult =
            challengeVerifier.verify(
                challengeResponseDto = input.challengeResponseDto,
                rsaPublicKey = input.rsaKey,
                sentVerifyToken = input.sentVerifyToken,
                sentDomain = input.sentDomain,
            )
        when (verifyResult) {
            ChallengeVerifier.Output.Failure -> {
                Timber.e("Challenge verification error")
                onError(Error.ChallengeVerificationError(Error.ChallengeVerificationError.Type.FAILURE))
            }
            ChallengeVerifier.Output.InvalidSignature -> {
                Timber.e("Challenge verification error: invalid signature")
                onError(Error.ChallengeVerificationError(Error.ChallengeVerificationError.Type.INVALID_SIGNATURE))
            }
            ChallengeVerifier.Output.TokenExpired -> {
                Timber.e("Challenge verification error: token expired")
                onError(Error.ChallengeVerificationError(Error.ChallengeVerificationError.Type.TOKEN_EXPIRED))
            }
            ChallengeVerifier.Output.VerifyTokenMismatch -> {
                Timber.e("Challenge verification error: verify token mismatch")
                onError(Error.ChallengeVerificationError(Error.ChallengeVerificationError.Type.VERIFY_TOKEN_MISMATCH))
            }
            ChallengeVerifier.Output.DomainMismatch -> {
                Timber.e("Challenge verification error: domain mismatch")
                onError(Error.ChallengeVerificationError(Error.ChallengeVerificationError.Type.DOMAIN_MISMATCH))
            }
            is ChallengeVerifier.Output.Verified -> {
                Timber.d("Challenge verified with success")
                onSuccess(
                    Success(
                        challengeResponseDto = input.challengeResponseDto,
                        accessToken = verifyResult.accessToken,
                        refreshToken = verifyResult.refreshToken,
                        mfaToken = input.mfaToken,
                        currentMfaToken = input.currentMfaToken,
                    ),
                )
            }
        }
    }

    private class SignInConfigInput(
        val serverPgpPublicKey: String,
        val passphrase: ByteArray,
        val userId: String,
        val serverRsaKey: String,
    )

    private data class SendSignInRequestInput(
        val config: SignInConfigInput,
        val challenge: String,
        val sentVerifyToken: String,
        val accountData: GetAccountDataUseCase.Output,
        val serverId: String,
    )

    private data class DecryptChallengeInput(
        val config: SignInConfigInput,
        val currentMfaToken: String?,
        val signInResult: SignInResult.Success,
        val sentVerifyToken: String,
        val sentDomain: String,
    )

    private data class VerifyChallengeInput(
        val challengeResponseDto: ChallengeResponseDto,
        val mfaToken: String?,
        val currentMfaToken: String?,
        val rsaKey: String,
        val sentVerifyToken: String,
        val sentDomain: String,
    )

    data class Success(
        val challengeResponseDto: ChallengeResponseDto,
        val accessToken: String,
        val refreshToken: String,
        val mfaToken: String?,
        val currentMfaToken: String?,
    )

    sealed class Error {
        data object IncorrectPassphrase : Error()

        data object NoNetwork : Error()

        data class ServerNotReachable(
            val serverUrl: String,
        ) : Error()

        data class AccountDoesNotExist(
            val label: String,
            val email: String?,
            val serverUrl: String,
        ) : Error()

        data class SignInFailure(
            val message: String,
        ) : Error()

        data class ChallengeDecryptionError(
            val message: String?,
        ) : Error()

        data object ServerSignatureInvalid : Error()

        data class ChallengeVerificationError(
            val type: Type,
        ) : Error() {
            enum class Type {
                INVALID_SIGNATURE,
                TOKEN_EXPIRED,
                VERIFY_TOKEN_MISMATCH,
                DOMAIN_MISMATCH,
                FAILURE,
            }
        }
    }
}
