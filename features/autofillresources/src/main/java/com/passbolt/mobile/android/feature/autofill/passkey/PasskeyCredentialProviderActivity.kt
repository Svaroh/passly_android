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
package net.svaroh.passly.feature.autofill.passkey

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.domerrors.DomError
import androidx.credentials.exceptions.domerrors.NotAllowedError
import androidx.credentials.exceptions.domerrors.NotSupportedError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException
import androidx.credentials.provider.PendingIntentHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.svaroh.passly.core.mvp.authentication.AuthenticationState
import net.svaroh.passly.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.navigation.ActivityIntents
import net.svaroh.passly.core.navigation.ActivityIntents.AuthConfig
import net.svaroh.passly.core.navigation.ActivityIntents.AuthConfig.RefreshPassphrase
import net.svaroh.passly.core.navigation.ActivityIntents.AuthConfig.RefreshSession
import net.svaroh.passly.core.resources.interactor.create.CreateResourceInteractor
import net.svaroh.passly.core.resources.usecase.db.AddLocalResourcePermissionsUseCase
import net.svaroh.passly.core.resources.usecase.db.AddLocalResourceUseCase
import net.svaroh.passly.core.secrets.usecase.decrypt.SecretInteractor
import net.svaroh.passly.core.secrets.usecase.decrypt.parser.SecretJsonModel
import net.svaroh.passly.core.users.usecase.db.GetLocalCurrentUserUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.RefreshSessionUseCase
import net.svaroh.passly.metadata.usecase.GetMetadataKeysSettingsUseCase
import net.svaroh.passly.metadata.usecase.db.GetLocalMetadataKeysUseCase
import net.svaroh.passly.metadata.usecase.db.GetLocalMetadataKeysUseCase.MetadataKeyPurpose.ENCRYPT
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.ui.CreateResourceModel
import net.svaroh.passly.ui.MetadataJsonModel
import net.svaroh.passly.ui.MetadataKeyTypeModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class PasskeyCredentialProviderActivity :
    AppCompatActivity(),
    KoinComponent {
    private val secretInteractor: SecretInteractor by inject()
    private val createResourceInteractor: CreateResourceInteractor by inject()
    private val addLocalResourceUseCase: AddLocalResourceUseCase by inject()
    private val addLocalResourcePermissionsUseCase: AddLocalResourcePermissionsUseCase by inject()
    private val getMetadataKeysSettingsUseCase: GetMetadataKeysSettingsUseCase by inject()
    private val getMetadataKeysUseCase: GetLocalMetadataKeysUseCase by inject()
    private val getLocalCurrentUserUseCase: GetLocalCurrentUserUseCase by inject()
    private val refreshSessionUseCase: RefreshSessionUseCase by inject()
    private val coroutineLaunchContext: CoroutineLaunchContext by inject()
    private val activityJob = SupervisorJob()
    private val activityScope by lazy { CoroutineScope(activityJob + coroutineLaunchContext.ui) }
    private var authenticationAttempts = 0
    private var isCompletingRequest = false

    private val authLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                completeCurrentRequest()
            } else {
                finishWithCurrentException("Passly authentication was cancelled.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when {
            hasValidGetRequest() -> completeGetRequest()
            hasValidCreateRequest() -> completeCreateRequest()
            PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent) != null ->
                finishWithCreateException("The passkey creation request is invalid.", NotSupportedError())
            else -> finishWithGetException("The passkey request is invalid.")
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun completeCurrentRequest() {
        when {
            hasValidGetRequest() -> completeGetRequest()
            hasValidCreateRequest() -> completeCreateRequest()
            PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent) != null ->
                finishWithCreateException("The passkey creation request is invalid.", NotSupportedError())
            else -> finishWithGetException("The passkey request is invalid.")
        }
    }

    private fun completeGetRequest() {
        if (isCompletingRequest) {
            return
        }
        isCompletingRequest = true
        activityScope.launch {
            val outcome =
                withContext(coroutineLaunchContext.io) {
                    buildAssertionOutcome()
                }

            when (outcome) {
                is AssertionOutcome.Success -> finishWithGetResponse(outcome.responseJson)
                is AssertionOutcome.NeedsAuthentication -> launchAuthentication(outcome.authConfig)
                is AssertionOutcome.Failure -> {
                    outcome.error?.let { Timber.e(it, "Unable to return passkey assertion.") }
                    finishWithGetException(outcome.message)
                }
            }
        }
    }

    private fun completeCreateRequest() {
        if (isCompletingRequest) {
            return
        }
        isCompletingRequest = true
        activityScope.launch {
            val outcome =
                withContext(coroutineLaunchContext.io) {
                    buildCreateOutcome()
                }

            when (outcome) {
                is CreateOutcome.Success -> finishWithCreateResponse(outcome.responseJson)
                is CreateOutcome.NeedsAuthentication -> launchAuthentication(outcome.authConfig)
                is CreateOutcome.Failure -> {
                    outcome.error?.let { Timber.e(it, "Unable to create passkey.") }
                    finishWithCreateException(outcome.message, outcome.domError)
                }
            }
        }
    }

    private fun launchAuthentication(authConfig: AuthConfig) {
        isCompletingRequest = false
        if (authenticationAttempts >= MAX_AUTHENTICATION_ATTEMPTS) {
            finishWithCurrentException("Passly authentication did not unlock this passkey.")
            return
        }
        authenticationAttempts += 1
        authLauncher.launch(
            ActivityIntents.authentication(
                this,
                authConfig,
            ),
        )
    }

    private suspend fun buildAssertionOutcome(): AssertionOutcome {
        val request =
            requireNotNull(PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)) {
                "The passkey request is invalid."
            }
        val resourceId =
            requireNotNull(intent.getStringExtra(EXTRA_RESOURCE_ID)) {
                "The selected passkey resource is missing."
            }

        val secret =
            when (val output = fetchPasskeySecret(resourceId)) {
                is SecretFetchOutcome.Success -> output.secret
                is SecretFetchOutcome.NeedsAuthentication -> return AssertionOutcome.NeedsAuthentication(output.authConfig)
                is SecretFetchOutcome.Failure -> return AssertionOutcome.Failure(output.message, output.error)
            }

        val options = request.credentialOptions.filterIsInstance<GetPublicKeyCredentialOption>()
        if (options.isEmpty()) {
            return AssertionOutcome.Failure("The request does not contain a passkey option.", null)
        }

        var lastError: Throwable? = null
        for (option in options) {
            runCatching {
                return AssertionOutcome.Success(
                    PasskeyWebauthn.buildAssertionResponseJson(
                        option = option,
                        callingAppInfo = request.callingAppInfo,
                        secret = secret,
                    ),
                )
            }.onFailure { lastError = it }
        }
        return AssertionOutcome.Failure(
            lastError?.message ?: "No passkey option matched this resource.",
            lastError,
        )
    }

    private suspend fun buildCreateOutcome(): CreateOutcome =
        runCatching {
            val providerRequest =
                requireNotNull(PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)) {
                    "The passkey creation request is invalid."
                }
            val createRequest =
                providerRequest.callingRequest.toPasskeyCreateRequest()
                    ?: return CreateOutcome.Failure(
                        "The request does not contain a passkey creation option.",
                        null,
                        NotSupportedError(),
                    )
            val registration =
                PasskeyWebauthn.buildRegistration(
                    requestJson = createRequest.requestJson,
                    clientDataHash = createRequest.clientDataHash,
                    callingAppInfo = providerRequest.callingAppInfo,
                )

            when (val output = createPasskeyResource(registration)) {
                is PasskeyResourceCreateOutcome.Success -> CreateOutcome.Success(registration.responseJson)
                is PasskeyResourceCreateOutcome.NeedsAuthentication -> CreateOutcome.NeedsAuthentication(output.authConfig)
                is PasskeyResourceCreateOutcome.Failure ->
                    CreateOutcome.Failure(output.message, output.error, NotAllowedError())
            }
        }.getOrElse { error ->
            CreateOutcome.Failure(
                error.message ?: "Unable to create this passkey.",
                error,
                if (error.message?.contains("ES256") == true) NotSupportedError() else NotAllowedError(),
            )
        }

    private suspend fun fetchPasskeySecret(resourceId: String): SecretFetchOutcome {
        val initialOutput = mapSecretInteractorOutput(secretInteractor.fetchAndDecrypt(resourceId))
        if (initialOutput !is SecretFetchOutcome.NeedsAuthentication ||
            initialOutput.authConfig !is RefreshSession
        ) {
            return initialOutput
        }

        return when (refreshSessionUseCase.execute(Unit)) {
            is RefreshSessionUseCase.Output.Success -> mapSecretInteractorOutput(secretInteractor.fetchAndDecrypt(resourceId))
            is RefreshSessionUseCase.Output.Failure -> initialOutput
        }
    }

    private suspend fun createPasskeyResource(registration: PasskeyWebauthn.PasskeyRegistration): PasskeyResourceCreateOutcome {
        val initialOutput = mapCreateResourceOutput(createStoredPasskeyResource(registration))
        if (initialOutput !is PasskeyResourceCreateOutcome.NeedsAuthentication ||
            initialOutput.authConfig !is RefreshSession
        ) {
            return initialOutput
        }

        return when (refreshSessionUseCase.execute(Unit)) {
            is RefreshSessionUseCase.Output.Success -> mapCreateResourceOutput(createStoredPasskeyResource(registration))
            is RefreshSessionUseCase.Output.Failure -> initialOutput
        }
    }

    private suspend fun createStoredPasskeyResource(registration: PasskeyWebauthn.PasskeyRegistration): CreateResourceInteractor.Output {
        val metadataKeyParams = getMetadataKeyParams()
        return createResourceInteractor.execute(
            resourceInput =
                CreateResourceModel(
                    contentType = ContentType.V5Passkey,
                    folderId = null,
                    expiry = null,
                    metadataKeyId = metadataKeyParams.metadataKeyId,
                    metadataKeyType = metadataKeyParams.metadataKeyType,
                    metadataJsonModel = MetadataJsonModel(registration.metadataJson),
                ),
            secretInput = SecretJsonModel(registration.secretJson),
        )
    }

    private suspend fun getMetadataKeyParams(): MetadataKeyParams {
        val canUsePersonalKeys =
            getMetadataKeysSettingsUseCase
                .execute(Unit)
                .metadataKeysSettingsModel
                .allowUsageOfPersonalKeys

        return if (canUsePersonalKeys) {
            MetadataKeyParams(
                metadataKeyId =
                    getLocalCurrentUserUseCase
                        .execute(Unit)
                        .user
                        .gpgKey
                        .id,
                metadataKeyType = MetadataKeyTypeModel.PERSONAL,
            )
        } else {
            MetadataKeyParams(
                metadataKeyId =
                    getMetadataKeysUseCase
                        .execute(GetLocalMetadataKeysUseCase.Input(ENCRYPT))
                        .firstOrNull()
                        ?.id
                        ?.toString(),
                metadataKeyType = MetadataKeyTypeModel.SHARED,
            )
        }
    }

    private suspend fun mapCreateResourceOutput(output: CreateResourceInteractor.Output): PasskeyResourceCreateOutcome =
        when (output) {
            is CreateResourceInteractor.Output.Success -> {
                runCatching {
                    addLocalResourceUseCase.execute(AddLocalResourceUseCase.Input(output.resource.resourceModel))
                    addLocalResourcePermissionsUseCase.execute(
                        AddLocalResourcePermissionsUseCase.Input(listOf(output.resource)),
                    )
                }.onFailure { Timber.e(it, "Passkey resource was created but local cache update failed.") }
                PasskeyResourceCreateOutcome.Success
            }
            is CreateResourceInteractor.Output.PasswordExpired ->
                PasskeyResourceCreateOutcome.NeedsAuthentication(RefreshPassphrase)
            is CreateResourceInteractor.Output.Failure<*> ->
                output.authenticationState.toAuthConfig()?.let(PasskeyResourceCreateOutcome::NeedsAuthentication)
                    ?: PasskeyResourceCreateOutcome.Failure(
                        output.response.exception.message ?: "Unable to save this passkey.",
                        output.response.exception,
                    )
            is CreateResourceInteractor.Output.OpenPgpError ->
                PasskeyResourceCreateOutcome.Failure(output.message ?: "Unable to encrypt this passkey.", null)
            is CreateResourceInteractor.Output.JsonSchemaValidationFailure ->
                PasskeyResourceCreateOutcome.Failure("The generated passkey resource is not valid.", null)
        }

    private fun mapSecretInteractorOutput(output: SecretInteractor.Output): SecretFetchOutcome =
        when (output) {
            is SecretInteractor.Output.Success -> SecretFetchOutcome.Success(PasskeySecret.parse(output.decryptedSecret))
            is SecretInteractor.Output.Unauthorized -> SecretFetchOutcome.NeedsAuthentication(RefreshPassphrase)
            is SecretInteractor.Output.FetchFailure -> {
                val authenticationState = output.authenticationState
                if (authenticationState is AuthenticationState.Unauthenticated &&
                    authenticationState.reason is AuthenticationState.Unauthenticated.Reason.Session
                ) {
                    SecretFetchOutcome.NeedsAuthentication(RefreshSession)
                } else {
                    SecretFetchOutcome.Failure("Unable to fetch this passkey.", output.exception)
                }
            }
            is SecretInteractor.Output.DecryptFailure ->
                SecretFetchOutcome.Failure(
                    output.error.message ?: "Unable to decrypt this passkey.",
                    null,
                )
        }

    private fun AuthenticationState.toAuthConfig(): AuthConfig? =
        when (this) {
            AuthenticationState.Authenticated -> null
            is AuthenticationState.Unauthenticated ->
                when (val reason = reason) {
                    AuthenticationState.Unauthenticated.Reason.Passphrase -> RefreshPassphrase
                    AuthenticationState.Unauthenticated.Reason.Session -> RefreshSession
                    is Mfa ->
                        reason.providers
                            ?.firstOrNull()
                            ?.providerName
                            ?.let(AuthConfig::Mfa)
                            ?: RefreshSession
                }
        }

    private fun hasValidGetRequest(): Boolean =
        PendingIntentHandler.retrieveProviderGetCredentialRequest(intent) != null &&
            !intent.getStringExtra(EXTRA_RESOURCE_ID).isNullOrBlank()

    private fun hasValidCreateRequest(): Boolean =
        PendingIntentHandler
            .retrieveProviderCreateCredentialRequest(intent)
            ?.callingRequest
            ?.toPasskeyCreateRequest() != null

    private fun CreateCredentialRequest.toPasskeyCreateRequest(): PasskeyCreateRequest? {
        if (this is CreatePublicKeyCredentialRequest) {
            return PasskeyCreateRequest(requestJson, clientDataHash)
        }

        if (!looksLikePublicKeyCreationRequest()) {
            return null
        }

        val requestJson =
            credentialData.getString(CREDENTIAL_BUNDLE_KEY_REQUEST_JSON)
                ?: candidateQueryData.getString(CREDENTIAL_BUNDLE_KEY_REQUEST_JSON)
                ?: return null
        val clientDataHash =
            credentialData.getByteArray(CREDENTIAL_BUNDLE_KEY_CLIENT_DATA_HASH)
                ?: candidateQueryData.getByteArray(CREDENTIAL_BUNDLE_KEY_CLIENT_DATA_HASH)
        return PasskeyCreateRequest(requestJson, clientDataHash)
    }

    private fun CreateCredentialRequest.looksLikePublicKeyCreationRequest(): Boolean =
        type == PUBLIC_KEY_CREDENTIAL_TYPE ||
            type == FRAMEWORK_PUBLIC_KEY_CREDENTIAL_TYPE ||
            credentialData.getString(CREDENTIAL_BUNDLE_KEY_SUBTYPE) ==
            CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST_SUBTYPE ||
            candidateQueryData.getString(CREDENTIAL_BUNDLE_KEY_SUBTYPE) ==
            CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST_SUBTYPE ||
            credentialData.containsKey(CREDENTIAL_BUNDLE_KEY_REQUEST_JSON) ||
            candidateQueryData.containsKey(CREDENTIAL_BUNDLE_KEY_REQUEST_JSON)

    private fun finishWithCurrentException(message: String) {
        if (PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent) != null) {
            finishWithCreateException(message, NotAllowedError())
        } else {
            finishWithGetException(message)
        }
    }

    private fun finishWithGetResponse(responseJson: String) {
        val result = Intent()
        PendingIntentHandler.setGetCredentialResponse(
            result,
            GetCredentialResponse(PublicKeyCredential(responseJson)),
        )
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun finishWithCreateResponse(responseJson: String) {
        val result = Intent()
        PendingIntentHandler.setCreateCredentialResponse(
            result,
            CreatePublicKeyCredentialResponse(responseJson),
        )
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun finishWithGetException(message: String) {
        val result = Intent()
        PendingIntentHandler.setGetCredentialException(
            result,
            GetPublicKeyCredentialDomException(NotAllowedError(), message),
        )
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun finishWithCreateException(
        message: String,
        domError: DomError,
    ) {
        val result = Intent()
        PendingIntentHandler.setCreateCredentialException(
            result,
            CreatePublicKeyCredentialDomException(domError, message),
        )
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    companion object {
        const val EXTRA_RESOURCE_ID = "net.svaroh.passly.extra.RESOURCE_ID"
        private const val MAX_AUTHENTICATION_ATTEMPTS = 3
        private const val PUBLIC_KEY_CREDENTIAL_TYPE = "androidx.credentials.TYPE_PUBLIC_KEY_CREDENTIAL"
        private const val FRAMEWORK_PUBLIC_KEY_CREDENTIAL_TYPE = "android.credentials.TYPE_PUBLIC_KEY_CREDENTIAL"
        private const val CREDENTIAL_BUNDLE_KEY_REQUEST_JSON = "androidx.credentials.BUNDLE_KEY_REQUEST_JSON"
        private const val CREDENTIAL_BUNDLE_KEY_CLIENT_DATA_HASH = "androidx.credentials.BUNDLE_KEY_CLIENT_DATA_HASH"
        private const val CREDENTIAL_BUNDLE_KEY_SUBTYPE = "androidx.credentials.BUNDLE_KEY_SUBTYPE"
        private const val CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST_SUBTYPE =
            "androidx.credentials.BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST"
    }

    private data class PasskeyCreateRequest(
        val requestJson: String,
        val clientDataHash: ByteArray?,
    )

    private data class MetadataKeyParams(
        val metadataKeyId: String?,
        val metadataKeyType: MetadataKeyTypeModel,
    )

    private sealed interface AssertionOutcome {
        data class Success(
            val responseJson: String,
        ) : AssertionOutcome

        data class NeedsAuthentication(
            val authConfig: AuthConfig,
        ) : AssertionOutcome

        data class Failure(
            val message: String,
            val error: Throwable?,
        ) : AssertionOutcome
    }

    private sealed interface CreateOutcome {
        data class Success(
            val responseJson: String,
        ) : CreateOutcome

        data class NeedsAuthentication(
            val authConfig: AuthConfig,
        ) : CreateOutcome

        data class Failure(
            val message: String,
            val error: Throwable?,
            val domError: DomError,
        ) : CreateOutcome
    }

    private sealed interface PasskeyResourceCreateOutcome {
        data object Success : PasskeyResourceCreateOutcome

        data class NeedsAuthentication(
            val authConfig: AuthConfig,
        ) : PasskeyResourceCreateOutcome

        data class Failure(
            val message: String,
            val error: Throwable?,
        ) : PasskeyResourceCreateOutcome
    }

    private sealed interface SecretFetchOutcome {
        data class Success(
            val secret: PasskeySecret,
        ) : SecretFetchOutcome

        data class NeedsAuthentication(
            val authConfig: AuthConfig,
        ) : SecretFetchOutcome

        data class Failure(
            val message: String,
            val error: Throwable?,
        ) : SecretFetchOutcome
    }
}
