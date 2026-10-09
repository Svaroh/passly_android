package net.svaroh.passly.feature.transferaccounttoanotherdevice.usecase

import kotlinx.coroutines.withContext
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.displayMessage
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountDataUseCase
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.mobiletransfer.MobileTransferRepository
import net.svaroh.passly.domain.privatekey.PrivateKeyRepository
import net.svaroh.passly.dto.request.BrowserFirstLoginAccountRequestDto
import net.svaroh.passly.dto.request.BrowserFirstLoginResponseRequestDto
import net.svaroh.passly.dto.response.qrcode.BrowserFirstLoginPageDto
import net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginPrivateKeyPayloadCrypto
import net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginPrivateKeyPayloadCrypto.PrivateKeyPayload
import net.svaroh.passly.gopenpgp.OpenPgp
import net.svaroh.passly.gopenpgp.exception.OpenPgpResult
import timber.log.Timber
import java.net.URI

class CompleteBrowserFirstLoginUseCase(
    private val mobileTransferRepository: MobileTransferRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val privateKeyRepository: PrivateKeyRepository,
    private val openPgp: OpenPgp,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) {
    suspend fun execute(input: Input): Output =
        withContext(coroutineLaunchContext.io) {
            try {
                complete(input.page)
            } catch (exception: IllegalArgumentException) {
                Timber.e(exception, "[BrowserFirstLogin] Invalid input/state")
                Output.Failure(exception.message)
            } catch (exception: IllegalStateException) {
                Timber.e(exception, "[BrowserFirstLogin] Invalid input/state")
                Output.Failure(exception.message)
            }
        }

    private suspend fun complete(page: BrowserFirstLoginPageDto): Output {
        Timber.i(
            "[BrowserFirstLogin] Start request=%s type=%s version=%s qrOrigin=%s",
            page.requestId.redactedId(),
            page.type,
            page.version,
            page.domain.safeOrigin(),
        )
        if (page.type != QR_TYPE || page.version != QR_VERSION) {
            Timber.e(
                "[BrowserFirstLogin] Unsupported QR request=%s type=%s version=%s",
                page.requestId.redactedId(),
                page.type,
                page.version,
            )
            return Output.Failure("Unsupported browser login QR code.")
        }

        val accountData = getSelectedAccountDataUseCase.execute(Unit)
        val serverUserId = requireNotNull(accountData.serverId) { "No server user id is available." }
        val username = requireNotNull(accountData.email) { "No account username is available." }
        val firstName = requireNotNull(accountData.firstName) { "No account first name is available." }
        val lastName = requireNotNull(accountData.lastName) { "No account last name is available." }
        Timber.i(
            "[BrowserFirstLogin] Selected account request=%s accountOrigin=%s serverUserIdPresent=%s",
            page.requestId.redactedId(),
            accountData.url.safeOrigin(),
            accountData.serverId != null,
        )
        if (!isSameOrigin(accountData.url, page.domain)) {
            Timber.e(
                "[BrowserFirstLogin] Domain mismatch request=%s qrOrigin=%s accountOrigin=%s",
                page.requestId.redactedId(),
                page.domain.safeOrigin(),
                accountData.url.safeOrigin(),
            )
            return Output.DomainMismatch(page.domain, accountData.url)
        }

        val userId =
            requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount) {
                "No selected account is available."
            }
        val privateKey =
            requireNotNull(privateKeyRepository.getPrivateKey(userId)?.armoredKey) {
                "Selected account private key is not available."
            }
        val fingerprint =
            when (val result = openPgp.getKeyFingerprint(privateKey)) {
                is OpenPgpResult.Error -> {
                    Timber.e(
                        "[BrowserFirstLogin] Could not read fingerprint request=%s message=%s",
                        page.requestId.redactedId(),
                        result.error.message,
                    )
                    return Output.Failure(result.error.message)
                }
                is OpenPgpResult.Result -> result.result
            }

        Timber.i("[BrowserFirstLogin] Setting account request=%s", page.requestId.redactedId())
        when (
            val response =
                mobileTransferRepository.setBrowserFirstLoginAccount(
                    page.requestId,
                    BrowserFirstLoginAccountRequestDto(page.secret, serverUserId, fingerprint),
                )
        ) {
            is DomainResult.Incomplete -> {
                Timber.e("[BrowserFirstLogin] Network failure step=set account message=%s", response.displayMessage())
                return Output.Failure(response.displayMessage())
            }
            is DomainResult.Finished ->
                Timber.i("[BrowserFirstLogin] Account set request=%s", page.requestId.redactedId())
        }

        Timber.i("[BrowserFirstLogin] Encrypting private key payload request=%s", page.requestId.redactedId())
        val encryptedPrivateKey =
            BrowserFirstLoginPrivateKeyPayloadCrypto.encrypt(
                secret = page.secret,
                privateKey =
                    PrivateKeyPayload(
                        armoredKey = privateKey,
                        userId = serverUserId,
                        fingerprint = fingerprint,
                        username = username,
                        firstName = firstName,
                        lastName = lastName,
                        roleName = accountData.role,
                    ),
            )

        Timber.i("[BrowserFirstLogin] Setting response request=%s", page.requestId.redactedId())
        return when (
            val response =
                mobileTransferRepository.setBrowserFirstLoginResponse(
                    page.requestId,
                    BrowserFirstLoginResponseRequestDto(page.secret, encryptedPrivateKey),
                )
        ) {
            is DomainResult.Incomplete -> {
                Timber.e("[BrowserFirstLogin] Network failure step=set response message=%s", response.displayMessage())
                Output.Failure(response.displayMessage())
            }
            is DomainResult.Finished -> {
                Timber.i("[BrowserFirstLogin] Completed request=%s", page.requestId.redactedId())
                Output.Success
            }
        }
    }

    private fun isSameOrigin(
        first: String,
        second: String,
    ): Boolean =
        runCatching {
            val firstUri = URI(first.trimEnd('/'))
            val secondUri = URI(second.trimEnd('/'))
            firstUri.scheme == secondUri.scheme &&
                firstUri.host == secondUri.host &&
                firstUri.effectivePort() == secondUri.effectivePort()
        }.getOrDefault(false)

    private fun String.redactedId(): String = take(8).ifBlank { "empty" }

    private fun String.safeOrigin(): String =
        runCatching {
            val uri = URI(trimEnd('/'))
            val scheme = uri.scheme ?: "unknown"
            val host = uri.host ?: "unknown"
            "$scheme://$host:${uri.effectivePort()}"
        }.getOrDefault("invalid")

    private fun URI.effectivePort() =
        if (port != -1) {
            port
        } else {
            when (scheme) {
                "https" -> 443
                "http" -> 80
                else -> -1
            }
        }

    data class Input(
        val page: BrowserFirstLoginPageDto,
    )

    sealed class Output {
        data object Success : Output()

        data class Failure(
            val message: String?,
        ) : Output()

        data class DomainMismatch(
            val qrDomain: String,
            val accountDomain: String,
        ) : Output()
    }

    private companion object {
        private const val QR_TYPE = "browser_first_login"
        private const val QR_VERSION = 1
    }
}
