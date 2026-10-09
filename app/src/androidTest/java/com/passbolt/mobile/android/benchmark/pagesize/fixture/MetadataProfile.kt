/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package net.svaroh.passly.benchmark.pagesize.fixture

data class MetadataProfile(
    val name: String,
    val uris: Int,
    val descriptionChars: Int,
    val customFields: Int,
    val customFieldValueChars: Int,
) {
    companion object {
        val HEAVY =
            MetadataProfile(
                name = "heavy-data",
                uris = 10,
                descriptionChars = 4_000,
                customFields = 16,
                customFieldValueChars = 500,
            )

        val MEDIUM =
            MetadataProfile(
                name = "medium-data",
                uris = 5,
                descriptionChars = 2_500,
                customFields = 10,
                customFieldValueChars = 350,
            )

        val LIGHT =
            MetadataProfile(
                name = "low-intensity-data",
                uris = 0,
                descriptionChars = 0,
                customFields = 0,
                customFieldValueChars = 0,
            )
    }
}
