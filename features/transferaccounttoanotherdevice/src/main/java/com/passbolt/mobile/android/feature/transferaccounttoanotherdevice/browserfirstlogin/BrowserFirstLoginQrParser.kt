package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.browserfirstlogin

import com.passbolt.mobile.android.dto.response.qrcode.BrowserFirstLoginPageDto
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class BrowserFirstLoginQrParser(
    private val json: Json,
) {
    fun parse(data: ByteArray?): BrowserFirstLoginPageDto? =
        data?.let { parse(String(it)) }

    fun parse(data: String): BrowserFirstLoginPageDto? =
        runCatching {
            when {
                data.startsWith(BROWSER_FIRST_LOGIN_DEEP_LINK_PREFIX) -> parseDeepLink(data)
                data.startsWith(BROWSER_FIRST_LOGIN_ANDROID_INTENT_PREFIX) -> parseDeepLink(data)
                data.length > RESERVED_BYTES_COUNT -> parseReservedBytesPayload(data)
                else -> null
            }
        }.onFailure {
            Timber.e(it, "Could not parse browser first-login QR code")
        }.getOrNull()

    private fun parseDeepLink(data: String): BrowserFirstLoginPageDto? {
        val uri = URI(data)
        if (
            uri.scheme !in BROWSER_FIRST_LOGIN_SUPPORTED_LINK_SCHEMES ||
            uri.host != BROWSER_FIRST_LOGIN_DEEP_LINK_HOST
        ) {
            return null
        }
        val queryParameters = uri.rawQuery.parseQueryParameters()

        return BrowserFirstLoginPageDto(
            type = queryParameters["type"].orEmpty(),
            version = queryParameters["version"]?.toIntOrNull() ?: -1,
            domain = queryParameters["domain"].orEmpty(),
            requestId = queryParameters["request_id"].orEmpty(),
            secret = queryParameters["secret"].orEmpty(),
        )
    }

    private fun String?.parseQueryParameters(): Map<String, String> =
        this
            ?.split("&")
            ?.mapNotNull { parameter ->
                val keyValue = parameter.split("=", limit = 2)
                val key = keyValue.getOrNull(0)?.urlDecode()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val value = keyValue.getOrNull(1)?.urlDecode().orEmpty()
                key to value
            }?.toMap()
            .orEmpty()

    private fun String.urlDecode(): String =
        URLDecoder.decode(this, StandardCharsets.UTF_8.name())

    private fun parseReservedBytesPayload(data: String): BrowserFirstLoginPageDto? {
        val version = data.substring(0, 1).toInt(RESERVED_BYTES_NUMBER_RADIX)
        val page = data.substring(1, RESERVED_BYTES_COUNT).toInt(RESERVED_BYTES_NUMBER_RADIX)
        if (version != BROWSER_FIRST_LOGIN_PROTOCOL_VERSION || page != FIRST_PAGE) {
            return null
        }

        return json.decodeFromString<BrowserFirstLoginPageDto>(data.substring(RESERVED_BYTES_COUNT))
    }

    private companion object {
        private const val BROWSER_FIRST_LOGIN_DEEP_LINK_SCHEME = "passbolt"
        private const val BROWSER_FIRST_LOGIN_ANDROID_INTENT_SCHEME = "intent"
        private const val BROWSER_FIRST_LOGIN_DEEP_LINK_HOST = "browser-first-login"
        private const val BROWSER_FIRST_LOGIN_DEEP_LINK_PREFIX = "passbolt://browser-first-login"
        private const val BROWSER_FIRST_LOGIN_ANDROID_INTENT_PREFIX = "intent://browser-first-login"
        private val BROWSER_FIRST_LOGIN_SUPPORTED_LINK_SCHEMES =
            setOf(BROWSER_FIRST_LOGIN_DEEP_LINK_SCHEME, BROWSER_FIRST_LOGIN_ANDROID_INTENT_SCHEME)
        private const val BROWSER_FIRST_LOGIN_PROTOCOL_VERSION = 3
        private const val FIRST_PAGE = 0
        private const val RESERVED_BYTES_NUMBER_RADIX = 16
        private const val RESERVED_BYTES_COUNT = 3
    }
}
