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

import org.json.JSONObject

internal data class PasskeySecret(
    val credentialId: String,
    val rpId: String,
    val userHandle: String,
    val privateKeyPkcs8: String,
    val signCount: Long,
    val backupEligible: Boolean,
    val backupState: Boolean,
) {
    companion object {
        fun parse(json: String): PasskeySecret {
            val source = JSONObject(json)
            require(source.requiredString("object_type") == PASSKEY_OBJECT_TYPE) {
                "The decrypted secret is not a Passly passkey."
            }
            require(source.optInt("schema_version") == PASSKEY_SCHEMA_VERSION) {
                "The passkey secret schema version is not supported."
            }
            require(source.optInt("cose_alg") == PASSKEY_ALGORITHM_ES256) {
                "Only ES256 passkeys are supported."
            }

            return PasskeySecret(
                credentialId = source.requiredString("credential_id"),
                rpId = source.requiredString("rp_id"),
                userHandle = source.requiredString("user_handle"),
                privateKeyPkcs8 = source.requiredString("private_key_pkcs8"),
                signCount = source.optLong("sign_count", 0).coerceIn(0, UInt.MAX_VALUE.toLong()),
                backupEligible = source.optBoolean("backup_eligible", false),
                backupState = source.optBoolean("backup_state", false),
            )
        }

        private const val PASSKEY_OBJECT_TYPE = "PASSLY_PASSKEY"
        private const val PASSKEY_SCHEMA_VERSION = 1
        private const val PASSKEY_ALGORITHM_ES256 = -7
    }
}

private fun JSONObject.requiredString(name: String): String =
    optString(name)
        .takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("The passkey secret is missing $name.")
