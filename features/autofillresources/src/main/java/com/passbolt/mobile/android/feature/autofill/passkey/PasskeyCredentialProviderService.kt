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

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourcesUseCase
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.ui.ResourceModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.net.URI
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

class PasskeyCredentialProviderService :
    CredentialProviderService(),
    KoinComponent {
    private val getLocalResourcesUseCase: GetLocalResourcesUseCase by inject()
    private val coroutineLaunchContext: CoroutineLaunchContext by inject()
    private val serviceJob = SupervisorJob()
    private val serviceScope by lazy { CoroutineScope(serviceJob + coroutineLaunchContext.ui) }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        receiver: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        serviceScope.launch {
            runCatching {
                withContext(coroutineLaunchContext.io) {
                    buildBeginGetCredentialResponse(request)
                }
            }.onSuccess { response ->
                if (!cancellationSignal.isCanceled) {
                    receiver.onResult(response)
                }
            }.onFailure { error ->
                Timber.e(error, "Unable to prepare passkey credential entries.")
                if (!cancellationSignal.isCanceled) {
                    receiver.onError(GetCredentialUnknownException(error.message))
                }
            }
        }
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        receiver: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        serviceScope.launch {
            runCatching {
                buildBeginCreateCredentialResponse(request)
            }.onSuccess { response ->
                if (!cancellationSignal.isCanceled) {
                    receiver.onResult(response)
                }
            }.onFailure { error ->
                Timber.e(error, "Unable to prepare passkey creation entry.")
                if (!cancellationSignal.isCanceled) {
                    receiver.onError(CreateCredentialUnknownException(error.message))
                }
            }
        }
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        receiver: OutcomeReceiver<Void?, ClearCredentialException>,
    ) {
        receiver.onResult(null)
    }

    @Suppress("DEPRECATION")
    private suspend fun buildBeginGetCredentialResponse(request: BeginGetCredentialRequest): BeginGetCredentialResponse {
        val options = request.beginGetCredentialOptions.filterIsInstance<BeginGetPublicKeyCredentialOption>()
        if (options.isEmpty()) {
            return BeginGetCredentialResponse()
        }

        val resources =
            runCatching {
                getLocalResourcesUseCase
                    .execute(GetLocalResourcesUseCase.Input(slugs = setOf(ContentType.V5Passkey.slug)))
                    .resources
            }.getOrElse {
                emptyList()
            }

        val builder = BeginGetCredentialResponse.Builder()
        options.forEachIndexed { optionIndex, option ->
            val rpId = PasskeyWebauthn.extractRpId(option.requestJson)?.lowercase() ?: return@forEachIndexed
            resources
                .filter { it.matchesRpId(rpId) }
                .forEach { resource ->
                    builder.addCredentialEntry(
                        buildCredentialEntry(
                            resource = resource,
                            option = option,
                            optionIndex = optionIndex,
                        ),
                    )
                }
        }

        return builder.build()
    }

    private fun buildBeginCreateCredentialResponse(request: BeginCreateCredentialRequest): BeginCreateCredentialResponse {
        val requestJson = request.publicKeyCreationRequestJson() ?: return BeginCreateCredentialResponse()
        if (!PasskeyWebauthn.supportsCreateRequest(requestJson)) {
            return BeginCreateCredentialResponse()
        }

        return BeginCreateCredentialResponse
            .Builder()
            .addCreateEntry(buildCreateEntry(requestJson))
            .build()
    }

    private fun buildCredentialEntry(
        resource: ResourceModel,
        option: BeginGetPublicKeyCredentialOption,
        optionIndex: Int,
    ): PublicKeyCredentialEntry {
        val resourceName = runCatching { resource.metadataJsonModel.name }.getOrDefault(PASSKEY_LABEL)
        val username =
            runCatching { resource.metadataJsonModel.username }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: resourceName

        return PublicKeyCredentialEntry
            .Builder(
                this,
                username,
                credentialPendingIntent(resource.resourceId, optionIndex),
                option,
            ).setDisplayName(resourceName)
            .setIcon(Icon.createWithResource(this, CoreUiR.drawable.passbolt_passkey))
            .setLastUsedTime(resource.modified.toInstant())
            .build()
    }

    private fun buildCreateEntry(requestJson: String): CreateEntry =
        CreateEntry
            .Builder(
                getString(LocalizationR.string.app_name),
                createPendingIntent(requestJson),
            ).setIcon(Icon.createWithResource(this, CoreUiR.drawable.passbolt_passkey))
            .setDescription(getString(LocalizationR.string.passkey_create_entry_description))
            .build()

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun credentialPendingIntent(
        resourceId: String,
        optionIndex: Int,
    ): PendingIntent {
        val requestCode = "$resourceId:$optionIndex".hashCode()
        val intent =
            Intent(this, PasskeyCredentialProviderActivity::class.java)
                .setAction(ACTION_GET_PASSKEY)
                .putExtra(PasskeyCredentialProviderActivity.EXTRA_RESOURCE_ID, resourceId)
        return PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createPendingIntent(requestJson: String): PendingIntent {
        val requestCode = "create:$requestJson".hashCode()
        val intent =
            Intent(this, PasskeyCredentialProviderActivity::class.java)
                .setAction(ACTION_CREATE_PASSKEY)
        return PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private fun BeginCreateCredentialRequest.publicKeyCreationRequestJson(): String? {
        if (this is BeginCreatePublicKeyCredentialRequest) {
            return requestJson
        }

        if (!looksLikePublicKeyCreationRequest()) {
            return null
        }

        return candidateQueryData
            .getString(CREDENTIAL_BUNDLE_KEY_REQUEST_JSON)
            ?.takeIf { it.isNotBlank() }
    }

    private fun BeginCreateCredentialRequest.looksLikePublicKeyCreationRequest(): Boolean =
        type == PUBLIC_KEY_CREDENTIAL_TYPE ||
            type == FRAMEWORK_PUBLIC_KEY_CREDENTIAL_TYPE ||
            candidateQueryData.getString(CREDENTIAL_BUNDLE_KEY_SUBTYPE) ==
            CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST_SUBTYPE ||
            candidateQueryData.containsKey(CREDENTIAL_BUNDLE_KEY_REQUEST_JSON)

    private fun ResourceModel.matchesRpId(rpId: String): Boolean {
        val candidates =
            buildList {
                runCatching { metadataJsonModel.uri }.getOrNull()?.let(::add)
                runCatching { metadataJsonModel.uris }.getOrNull()?.let(::addAll)
            }
        return candidates.any { uri ->
            val host = uri.hostOrNull()
            host == rpId || host?.endsWith(".$rpId") == true
        }
    }

    private fun String.hostOrNull(): String? =
        runCatching {
            val normalized = if (contains("://")) this else "https://$this"
            URI(normalized).host?.lowercase()
        }.getOrNull()

    private companion object {
        private const val ACTION_GET_PASSKEY = "net.svaroh.passly.action.GET_PASSKEY"
        private const val ACTION_CREATE_PASSKEY = "net.svaroh.passly.action.CREATE_PASSKEY"
        private const val PASSKEY_LABEL = "Passkey"
        private const val PUBLIC_KEY_CREDENTIAL_TYPE = "androidx.credentials.TYPE_PUBLIC_KEY_CREDENTIAL"
        private const val FRAMEWORK_PUBLIC_KEY_CREDENTIAL_TYPE = "android.credentials.TYPE_PUBLIC_KEY_CREDENTIAL"
        private const val CREDENTIAL_BUNDLE_KEY_REQUEST_JSON = "androidx.credentials.BUNDLE_KEY_REQUEST_JSON"
        private const val CREDENTIAL_BUNDLE_KEY_SUBTYPE = "androidx.credentials.BUNDLE_KEY_SUBTYPE"
        private const val CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST_SUBTYPE =
            "androidx.credentials.BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST"
    }
}
