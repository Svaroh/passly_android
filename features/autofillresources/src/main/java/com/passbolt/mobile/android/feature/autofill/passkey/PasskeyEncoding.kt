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

import java.nio.charset.StandardCharsets
import java.util.Base64

internal object PasskeyEncoding {
    fun base64UrlEncode(value: ByteArray): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value)

    fun base64UrlDecode(value: String): ByteArray =
        Base64
            .getUrlDecoder()
            .decode(value.padEnd((value.length + 3) / 4 * 4, '='))

    fun utf8(value: String): ByteArray = value.toByteArray(StandardCharsets.UTF_8)

    fun uint32ToBigEndian(value: Long): ByteArray =
        byteArrayOf(
            ((value ushr 24) and 0xff).toByte(),
            ((value ushr 16) and 0xff).toByte(),
            ((value ushr 8) and 0xff).toByte(),
            (value and 0xff).toByte(),
        )
}
