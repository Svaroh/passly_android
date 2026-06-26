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

import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.webauthn.AuthenticatorAssertionResponse
import androidx.credentials.webauthn.Cbor
import androidx.credentials.webauthn.FidoPublicKeyCredential
import androidx.credentials.webauthn.PublicKeyCredentialCreationOptions
import androidx.credentials.webauthn.PublicKeyCredentialRequestOptions
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.net.URI
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Locale

internal object PasskeyWebauthn {
    fun extractRpId(requestJson: String): String? =
        runCatching {
            val json = JSONObject(requestJson)
            json.optStringOrNull("rpId")
                ?: json.optJSONObject("rp")?.optStringOrNull("id")
                ?: json.optStringOrNull("origin")?.let { origin -> java.net.URI(origin).host }
        }.getOrNull()

    fun supportsCreateRequest(requestJson: String): Boolean =
        supportsCreateRequestWithAndroidXParser(requestJson) || supportsCreateRequestWithJsonParser(requestJson)

    private fun supportsCreateRequestWithAndroidXParser(requestJson: String): Boolean =
        runCatching {
            val options = PublicKeyCredentialCreationOptions(requestJson)
            options.pubKeyCredParams.any { parameter ->
                parameter.type == PUBLIC_KEY_CREDENTIAL_TYPE && parameter.alg == PASSKEY_ALGORITHM_ES256.toLong()
            }
        }.getOrDefault(false)

    private fun supportsCreateRequestWithJsonParser(requestJson: String): Boolean =
        runCatching {
            val parameters = JSONObject(requestJson).optJSONArray("pubKeyCredParams") ?: return@runCatching false
            (0 until parameters.length()).any { index ->
                val parameter = parameters.optJSONObject(index) ?: return@any false
                parameter.optString("type") == PUBLIC_KEY_CREDENTIAL_TYPE &&
                    parameter.optLong("alg") == PASSKEY_ALGORITHM_ES256.toLong()
            }
        }.getOrDefault(false)

    fun buildRegistration(
        request: CreatePublicKeyCredentialRequest,
        callingAppInfo: CallingAppInfo,
    ): PasskeyRegistration =
        buildRegistration(
            requestJson = request.requestJson,
            clientDataHash = request.clientDataHash,
            callingAppInfo = callingAppInfo,
        )

    fun buildRegistration(
        requestJson: String,
        clientDataHash: ByteArray?,
        callingAppInfo: CallingAppInfo,
    ): PasskeyRegistration {
        val requestOptions = PublicKeyCredentialCreationOptions(requestJson)
        require(
            requestOptions.pubKeyCredParams.any { parameter ->
                parameter.type == PUBLIC_KEY_CREDENTIAL_TYPE &&
                    parameter.alg == PASSKEY_ALGORITHM_ES256.toLong()
            },
        ) {
            "Passly supports only ES256 public-key passkeys."
        }

        val explicitRpId =
            requestOptions.rp.id.ifBlank {
                extractRpId(requestJson).orEmpty()
            }
        val origin =
            resolveOrigin(
                requestJson,
                explicitRpId.ifBlank { requestOptions.rp.name },
                callingAppInfo,
            )
        val rpId =
            explicitRpId
                .ifBlank {
                    URI(origin.value).host
                        ?: throw IllegalArgumentException("The passkey creation request does not contain an RP ID.")
                }.lowercase(Locale.US)
        val credentialId = randomBytes(PASSKEY_CREDENTIAL_ID_LENGTH)
        val keyPair =
            KeyPairGenerator
                .getInstance("EC")
                .apply { initialize(ECGenParameterSpec(EC_P256_CURVE), SecureRandom()) }
                .generateKeyPair()
        val publicKeyCose = buildCoseEc2PublicKey(keyPair.public as ECPublicKey)
        val effectiveClientDataHash = clientDataHash?.takeIf { origin.isPrivilegedWebOrigin }
        val credentialResponseJson =
            buildRegistrationResponseJson(
                requestOptions = requestOptions,
                credentialId = credentialId,
                rpId = rpId,
                origin = origin.value,
                packageName = callingAppInfo.packageName,
                publicKeyCose = publicKeyCose,
                publicKeyDer = keyPair.public.encoded,
                clientDataHash = effectiveClientDataHash,
            )

        return PasskeyRegistration(
            responseJson = credentialResponseJson,
            metadataJson = buildMetadataJson(requestOptions, origin.value),
            secretJson =
                buildSecretJson(
                    requestOptions = requestOptions,
                    credentialId = credentialId,
                    rpId = rpId,
                    origin = origin.value,
                    publicKeyCose = publicKeyCose,
                    privateKeyPkcs8 = keyPair.private.encoded,
                ),
        )
    }

    fun buildAssertionResponseJson(
        option: GetPublicKeyCredentialOption,
        callingAppInfo: CallingAppInfo,
        secret: PasskeySecret,
    ): String {
        val requestOptions = PublicKeyCredentialRequestOptions(option.requestJson)
        val rpId =
            requestOptions.rpId
                .ifBlank {
                    extractRpId(option.requestJson)
                        ?: throw IllegalArgumentException("The passkey request does not contain an RP ID.")
                }.lowercase(Locale.US)
        require(rpId == secret.rpId) {
            "The passkey does not belong to this relying party."
        }
        require(isCredentialAllowed(option.requestJson, secret.credentialId)) {
            "The relying party did not allow this passkey credential."
        }

        val origin = resolveOrigin(option.requestJson, rpId, callingAppInfo)
        val clientDataHash = option.clientDataHash?.takeIf { origin.isPrivilegedWebOrigin }
        val response =
            AuthenticatorAssertionResponse(
                requestOptions,
                PasskeyEncoding.base64UrlDecode(secret.credentialId),
                origin.value,
                true,
                true,
                secret.backupEligible,
                secret.backupState,
                PasskeyEncoding.base64UrlDecode(secret.userHandle),
                callingAppInfo.packageName,
                clientDataHash,
            )
        response.authenticatorData = buildAuthenticatorData(secret)
        response.signature = signAssertion(secret.privateKeyPkcs8, response.dataToSign())

        return FidoPublicKeyCredential(
            PasskeyEncoding.base64UrlDecode(secret.credentialId),
            response,
            AUTHENTICATOR_ATTACHMENT_PLATFORM,
        ).json()
    }

    private fun buildMetadataJson(
        requestOptions: PublicKeyCredentialCreationOptions,
        origin: String,
    ): String {
        val rpName = requestOptions.rp.name.ifBlank { requestOptions.rp.id }
        return JSONObject()
            .put("name", truncate("$rpName passkey", RESOURCE_NAME_MAX_LENGTH))
            .put("username", truncate(requestOptions.user.name, RESOURCE_USERNAME_MAX_LENGTH))
            .put("uris", JSONArray().put(truncate(origin, RESOURCE_URI_MAX_LENGTH)))
            .put("description", JSONObject.NULL)
            .toString()
    }

    private fun buildSecretJson(
        requestOptions: PublicKeyCredentialCreationOptions,
        credentialId: ByteArray,
        rpId: String,
        origin: String,
        publicKeyCose: ByteArray,
        privateKeyPkcs8: ByteArray,
    ): String =
        JSONObject()
            .put("object_type", PASSKEY_OBJECT_TYPE)
            .put("schema_version", PASSKEY_SECRET_SCHEMA_VERSION)
            .put("credential_id", PasskeyEncoding.base64UrlEncode(credentialId))
            .put("rp_id", rpId)
            .put("origin", origin)
            .put("user_handle", PasskeyEncoding.base64UrlEncode(requestOptions.user.id))
            .put("user_name", truncate(requestOptions.user.name, RESOURCE_USERNAME_MAX_LENGTH))
            .put(
                "user_display_name",
                requestOptions.user.displayName
                    .takeIf { it.isNotBlank() }
                    ?.let { truncate(it, RESOURCE_USERNAME_MAX_LENGTH) }
                    ?: JSONObject.NULL,
            ).put("cose_alg", PASSKEY_ALGORITHM_ES256)
            .put("public_key_cose", PasskeyEncoding.base64UrlEncode(publicKeyCose))
            .put("private_key_pkcs8", PasskeyEncoding.base64UrlEncode(privateKeyPkcs8))
            .put("aaguid", PASSKEY_AAGUID_UUID)
            .put("backup_eligible", PASSKEY_BACKUP_ELIGIBLE)
            .put("backup_state", PASSKEY_BACKUP_STATE)
            .put("sign_count", PASSKEY_INITIAL_SIGN_COUNT)
            .put("transports", JSONArray(listOf(PASSKEY_TRANSPORT_INTERNAL, PASSKEY_TRANSPORT_HYBRID)))
            .toString()

    private fun buildRegistrationResponseJson(
        requestOptions: PublicKeyCredentialCreationOptions,
        credentialId: ByteArray,
        rpId: String,
        origin: String,
        packageName: String?,
        publicKeyCose: ByteArray,
        publicKeyDer: ByteArray,
        clientDataHash: ByteArray?,
    ): String {
        val clientDataJson =
            buildClientDataJson(
                type = WEBAUTHN_CREATE_TYPE,
                challenge = requestOptions.challenge,
                origin = origin,
                packageName = packageName,
            )
        val authenticatorData =
            buildAttestationAuthenticatorData(
                rpId = rpId,
                credentialId = credentialId,
                publicKeyCose = publicKeyCose,
            )
        val response =
            JSONObject()
                .put(
                    "attestationObject",
                    PasskeyEncoding.base64UrlEncode(
                        buildAttestationObject(authenticatorData),
                    ),
                ).put(
                    "authenticatorData",
                    PasskeyEncoding.base64UrlEncode(authenticatorData),
                ).put(
                    "publicKey",
                    PasskeyEncoding.base64UrlEncode(publicKeyDer),
                ).put(
                    "publicKeyAlgorithm",
                    PASSKEY_ALGORITHM_ES256,
                ).put(
                    "transports",
                    JSONArray(listOf(PASSKEY_TRANSPORT_INTERNAL, PASSKEY_TRANSPORT_HYBRID)),
                )
        if (clientDataHash == null) {
            response.put(
                "clientDataJSON",
                PasskeyEncoding.base64UrlEncode(PasskeyEncoding.utf8(clientDataJson.toString())),
            )
        }

        return buildPublicKeyCredentialJson(credentialId, response)
    }

    private fun buildClientDataJson(
        type: String,
        challenge: ByteArray,
        origin: String,
        packageName: String?,
    ): JSONObject =
        JSONObject()
            .put("type", type)
            .put("challenge", PasskeyEncoding.base64UrlEncode(challenge))
            .put("origin", origin)
            .apply {
                if (packageName != null) {
                    put("androidPackageName", packageName)
                }
            }

    private fun buildAttestationObject(authenticatorData: ByteArray): ByteArray =
        Cbor().encode(
            linkedMapOf<String, Any>(
                "fmt" to "none",
                "attStmt" to emptyMap<String, Any>(),
                "authData" to authenticatorData,
            ),
        )

    private fun buildAttestationAuthenticatorData(
        rpId: String,
        credentialId: ByteArray,
        publicKeyCose: ByteArray,
    ): ByteArray {
        var flags =
            WEBAUTHN_FLAG_USER_PRESENT or
                WEBAUTHN_FLAG_USER_VERIFIED or
                WEBAUTHN_FLAG_ATTESTED_CREDENTIAL_DATA
        if (PASSKEY_BACKUP_ELIGIBLE) {
            flags = flags or WEBAUTHN_FLAG_BACKUP_ELIGIBLE
        }
        if (PASSKEY_BACKUP_STATE) {
            flags = flags or WEBAUTHN_FLAG_BACKUP_STATE
        }
        return MessageDigest
            .getInstance("SHA-256")
            .digest(PasskeyEncoding.utf8(rpId)) +
            byteArrayOf(flags.toByte()) +
            PasskeyEncoding.uint32ToBigEndian(PASSKEY_INITIAL_SIGN_COUNT.toLong()) +
            ByteArray(PASSKEY_AAGUID_LENGTH) +
            credentialId.size.toUInt16BigEndian() +
            credentialId +
            publicKeyCose
    }

    private fun buildPublicKeyCredentialJson(
        credentialId: ByteArray,
        response: JSONObject,
    ): String {
        val encodedCredentialId = PasskeyEncoding.base64UrlEncode(credentialId)
        return JSONObject()
            .put("id", encodedCredentialId)
            .put("rawId", encodedCredentialId)
            .put("type", PUBLIC_KEY_CREDENTIAL_TYPE)
            .put("authenticatorAttachment", AUTHENTICATOR_ATTACHMENT_PLATFORM)
            .put("response", response)
            .put("clientExtensionResults", JSONObject())
            .toString()
    }

    private fun buildCoseEc2PublicKey(publicKey: ECPublicKey): ByteArray {
        val cosePublicKey =
            linkedMapOf<Int, Any>(
                1 to COSE_KEY_TYPE_EC2,
                3 to PASSKEY_ALGORITHM_ES256,
                -1 to COSE_CURVE_P256,
                -2 to publicKey.w.affineX.toUnsignedFixedLength(PASSKEY_P256_COORDINATE_LENGTH),
                -3 to publicKey.w.affineY.toUnsignedFixedLength(PASSKEY_P256_COORDINATE_LENGTH),
            )
        return Cbor().encode(cosePublicKey)
    }

    private fun BigInteger.toUnsignedFixedLength(length: Int): ByteArray {
        val unsigned =
            toByteArray()
                .dropWhile { it == 0.toByte() }
                .toByteArray()
        require(unsigned.size <= length) {
            "The ES256 public key coordinate is too large."
        }
        return ByteArray(length - unsigned.size) + unsigned
    }

    private fun Int.toUInt16BigEndian(): ByteArray {
        require(this in 0..UShort.MAX_VALUE.toInt()) {
            "The passkey credential ID is too large."
        }
        return byteArrayOf(
            ((this ushr 8) and 0xff).toByte(),
            (this and 0xff).toByte(),
        )
    }

    private fun randomBytes(length: Int): ByteArray =
        ByteArray(length).also {
            SecureRandom().nextBytes(it)
        }

    private fun truncate(
        value: String?,
        maxLength: Int,
    ): String? = value?.take(maxLength)

    private fun resolveOrigin(
        requestJson: String,
        rpId: String,
        callingAppInfo: CallingAppInfo,
    ): Origin {
        val privilegedOrigin =
            runCatching { callingAppInfo.getOrigin(CHROME_PRIVILEGED_ALLOWLIST) }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        if (privilegedOrigin != null) {
            return Origin(privilegedOrigin, isPrivilegedWebOrigin = true)
        }

        val requestOrigin =
            runCatching { JSONObject(requestJson).optStringOrNull("origin") }
                .getOrNull()
        return Origin(requestOrigin ?: "https://$rpId", isPrivilegedWebOrigin = false)
    }

    private fun isCredentialAllowed(
        requestJson: String,
        credentialId: String,
    ): Boolean {
        val allowCredentials =
            runCatching { JSONObject(requestJson).optJSONArray("allowCredentials") }
                .getOrNull()
                ?: return true
        if (allowCredentials.length() == 0) {
            return true
        }

        return (0 until allowCredentials.length()).any { index ->
            val descriptor = allowCredentials.optJSONObject(index) ?: return@any false
            descriptor.optString("type") == "public-key" && descriptor.optString("id") == credentialId
        }
    }

    private fun buildAuthenticatorData(secret: PasskeySecret): ByteArray {
        var flags = WEBAUTHN_FLAG_USER_PRESENT or WEBAUTHN_FLAG_USER_VERIFIED
        if (secret.backupEligible) {
            flags = flags or WEBAUTHN_FLAG_BACKUP_ELIGIBLE
        }
        if (secret.backupState) {
            flags = flags or WEBAUTHN_FLAG_BACKUP_STATE
        }
        return MessageDigest
            .getInstance("SHA-256")
            .digest(PasskeyEncoding.utf8(secret.rpId)) +
            byteArrayOf(flags.toByte()) +
            PasskeyEncoding.uint32ToBigEndian(secret.signCount)
    }

    private fun signAssertion(
        privateKeyPkcs8: String,
        dataToSign: ByteArray,
    ): ByteArray {
        val privateKey =
            KeyFactory
                .getInstance("EC")
                .generatePrivate(PKCS8EncodedKeySpec(PasskeyEncoding.base64UrlDecode(privateKeyPkcs8)))
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(dataToSign)
        return signature.sign()
    }

    private data class Origin(
        val value: String,
        val isPrivilegedWebOrigin: Boolean,
    )

    data class PasskeyRegistration(
        val responseJson: String,
        val metadataJson: String,
        val secretJson: String,
    )

    private fun JSONObject.optStringOrNull(name: String): String? =
        optString(name)
            .takeIf { it.isNotBlank() }

    private const val AUTHENTICATOR_ATTACHMENT_PLATFORM = "platform"
    private const val PUBLIC_KEY_CREDENTIAL_TYPE = "public-key"
    private const val EC_P256_CURVE = "secp256r1"
    private const val PASSKEY_OBJECT_TYPE = "PASSLY_PASSKEY"
    private const val PASSKEY_SECRET_SCHEMA_VERSION = 1
    private const val PASSKEY_ALGORITHM_ES256 = -7
    private const val PASSKEY_CREDENTIAL_ID_LENGTH = 32
    private const val PASSKEY_P256_COORDINATE_LENGTH = 32
    private const val PASSKEY_AAGUID_LENGTH = 16
    private const val PASSKEY_AAGUID_UUID = "00000000-0000-0000-0000-000000000000"
    private const val PASSKEY_BACKUP_ELIGIBLE = false
    private const val PASSKEY_BACKUP_STATE = false
    private const val PASSKEY_INITIAL_SIGN_COUNT = 0
    private const val PASSKEY_TRANSPORT_INTERNAL = "internal"
    private const val PASSKEY_TRANSPORT_HYBRID = "hybrid"
    private const val COSE_KEY_TYPE_EC2 = 2
    private const val COSE_CURVE_P256 = 1
    private const val RESOURCE_NAME_MAX_LENGTH = 255
    private const val RESOURCE_USERNAME_MAX_LENGTH = 255
    private const val RESOURCE_URI_MAX_LENGTH = 1024
    private const val WEBAUTHN_FLAG_USER_PRESENT = 0x01
    private const val WEBAUTHN_FLAG_USER_VERIFIED = 0x04
    private const val WEBAUTHN_FLAG_BACKUP_ELIGIBLE = 0x08
    private const val WEBAUTHN_FLAG_BACKUP_STATE = 0x10
    private const val WEBAUTHN_FLAG_ATTESTED_CREDENTIAL_DATA = 0x40
    private const val WEBAUTHN_CREATE_TYPE = "webauthn.create"

    private const val CHROME_PRIVILEGED_ALLOWLIST = """
        {
          "apps": [
            {
              "type": "android",
              "info": {
                "package_name": "com.android.chrome",
                "signatures": [
                  {
                    "build": "release",
                    "cert_fingerprint_sha256": "F0:FD:6C:5B:41:0F:25:CB:25:C3:B5:33:46:C8:97:2F:AE:30:F8:EE:74:11:DF:91:04:80:AD:6B:2D:60:DB:83"
                  },
                  {
                    "build": "userdebug",
                    "cert_fingerprint_sha256": "19:75:B2:F1:71:77:BC:89:A5:DF:F3:1F:9E:64:A6:CA:E2:81:A5:3D:C1:D1:D5:9B:1D:14:7F:E1:C8:2A:FA:00"
                  }
                ]
              }
            },
            {
              "type": "android",
              "info": {
                "package_name": "com.chrome.beta",
                "signatures": [
                  {
                    "build": "release",
                    "cert_fingerprint_sha256": "DA:63:3D:34:B6:9E:63:AE:21:03:B4:9D:53:CE:05:2F:C5:F7:F3:C5:3A:AB:94:FD:C2:A2:08:BD:FD:14:24:9C"
                  },
                  {
                    "build": "release",
                    "cert_fingerprint_sha256": "3D:7A:12:23:01:9A:A3:9D:9E:A0:E3:43:6A:B7:C0:89:6B:FB:4F:B6:79:F4:DE:5F:E7:C2:3F:32:6C:8F:99:4A"
                  }
                ]
              }
            },
            {
              "type": "android",
              "info": {
                "package_name": "com.chrome.dev",
                "signatures": [
                  {
                    "build": "release",
                    "cert_fingerprint_sha256": "90:44:EE:5F:EE:4B:BC:5E:21:DD:44:66:54:31:C4:EB:1F:1F:71:A3:27:16:A0:BC:92:7B:CB:B3:92:33:CA:BF"
                  },
                  {
                    "build": "release",
                    "cert_fingerprint_sha256": "3D:7A:12:23:01:9A:A3:9D:9E:A0:E3:43:6A:B7:C0:89:6B:FB:4F:B6:79:F4:DE:5F:E7:C2:3F:32:6C:8F:99:4A"
                  }
                ]
              }
            },
            {
              "type": "android",
              "info": {
                "package_name": "com.chrome.canary",
                "signatures": [
                  {
                    "build": "release",
                    "cert_fingerprint_sha256": "20:19:DF:A1:FB:23:EF:BF:70:C5:BC:D1:44:3C:5B:EA:B0:4F:3F:2F:F4:36:6E:9A:C1:E3:45:76:39:A2:4C:FC"
                  }
                ]
              }
            },
            {
              "type": "android",
              "info": {
                "package_name": "org.chromium.chrome",
                "signatures": [
                  {
                    "build": "release",
                    "cert_fingerprint_sha256": "C6:AD:B8:B8:3C:6D:4C:17:D2:92:AF:DE:56:FD:48:8A:51:D3:16:FF:8F:2C:11:C5:41:02:23:BF:F8:A7:DB:B3"
                  },
                  {
                    "build": "userdebug",
                    "cert_fingerprint_sha256": "19:75:B2:F1:71:77:BC:89:A5:DF:F3:1F:9E:64:A6:CA:E2:81:A5:3D:C1:D1:D5:9B:1D:14:7F:E1:C8:2A:FA:00"
                  }
                ]
              }
            },
            {
              "type": "android",
              "info": {
                "package_name": "com.google.android.apps.chrome",
                "signatures": [
                  {
                    "build": "userdebug",
                    "cert_fingerprint_sha256": "19:75:B2:F1:71:77:BC:89:A5:DF:F3:1F:9E:64:A6:CA:E2:81:A5:3D:C1:D1:D5:9B:1D:14:7F:E1:C8:2A:FA:00"
                  }
                ]
              }
            }
          ]
        }
    """
}
