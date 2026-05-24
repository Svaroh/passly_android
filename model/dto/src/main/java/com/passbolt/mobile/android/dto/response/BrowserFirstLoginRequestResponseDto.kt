/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 */
package com.passbolt.mobile.android.dto.response

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class BrowserFirstLoginRequestResponseDto(
    val id: UUID,
    val status: String,
    @SerializedName("user_id")
    val userId: String?,
    @SerializedName("user_key_fingerprint")
    val userKeyFingerprint: String?,
)
