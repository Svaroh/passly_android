package net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object BrowserFirstLoginPrivateKeyPayloadCrypto {
    fun encrypt(
        secret: String,
        privateKey: PrivateKeyPayload,
        iv: ByteArray = secureRandomIv(),
    ): String {
        require(secret.isNotBlank()) { "The browser first-login secret is missing." }
        require(iv.size == IV_BYTES) { "The browser first-login private key payload IV is invalid." }

        val key = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(UTF_8))
        val cipher =
            Cipher
                .getInstance(TRANSFORMATION)
                .apply {
                    init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, KEY_ALGORITHM), GCMParameterSpec(TAG_BITS, iv))
                }
        val ciphertext = cipher.doFinal(Json.encodeToString(privateKey).toByteArray(UTF_8))

        return Json.encodeToString(
            EncryptedPayload(
                iv = base64UrlEncode(iv),
                ciphertext = base64UrlEncode(ciphertext),
            ),
        )
    }

    private fun base64UrlEncode(value: ByteArray): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value)

    private fun secureRandomIv(): ByteArray =
        ByteArray(IV_BYTES).also {
            SecureRandom().nextBytes(it)
        }

    @Serializable
    data class PrivateKeyPayload(
        @SerialName("armored_key")
        val armoredKey: String,
        @SerialName("user_id")
        val userId: String,
        @SerialName("fingerprint")
        val fingerprint: String,
        @SerialName("username")
        val username: String,
        @SerialName("first_name")
        val firstName: String,
        @SerialName("last_name")
        val lastName: String,
        @SerialName("role_name")
        val roleName: String?,
    )

    @Serializable
    private data class EncryptedPayload(
        @SerialName("v")
        val version: Int = PAYLOAD_VERSION,
        @SerialName("alg")
        val algorithm: String = PAYLOAD_ALGORITHM,
        @SerialName("iv")
        val iv: String,
        @SerialName("ciphertext")
        val ciphertext: String,
    )

    private const val PAYLOAD_VERSION = 1
    private const val PAYLOAD_ALGORITHM = "A256GCM"
    private const val KEY_ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128
    private const val IV_BYTES = 12
}
