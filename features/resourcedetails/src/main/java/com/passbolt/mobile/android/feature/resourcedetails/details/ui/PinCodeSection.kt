/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
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

package net.svaroh.passly.feature.resourcedetails.details.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import net.svaroh.passly.core.compose.PassboltTheme
import net.svaroh.passly.core.localization.R
import net.svaroh.passly.core.ui.header.ActionIcon
import net.svaroh.passly.core.ui.header.ItemWithHeader
import net.svaroh.passly.core.ui.header.ValueStyle
import net.svaroh.passly.core.ui.section.Section
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.CopyPinCode
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.TogglePinCodeVisibility
import net.svaroh.passly.testtags.composetags.ResourceDetails

@Composable
internal fun PinCodeSection(
    pinCode: String,
    isPinCodeVisible: Boolean,
    onIntent: (ResourceDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(
        title = stringResource(R.string.resource_details_pin_code_header),
        modifier = modifier.testTag(ResourceDetails.PIN_CODE_SECTION),
    ) {
        ItemWithHeader(
            headerText = stringResource(R.string.resource_details_pin_code_code),
            value = if (isPinCodeVisible) pinCode else "",
            valueStyle =
                ValueStyle.Secret(
                    differentiateCharacters = isPinCodeVisible,
                    isRevealed = isPinCodeVisible,
                ),
            actionIcon = if (isPinCodeVisible) ActionIcon.HIDE else ActionIcon.VIEW,
            isTextSelectable = isPinCodeVisible,
            onItemClick = { onIntent(CopyPinCode) },
            onActionClick = { onIntent(TogglePinCodeVisibility) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PinCodeSectionConcealedPreview() {
    PassboltTheme {
        PinCodeSection(
            pinCode = "1234",
            isPinCodeVisible = false,
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PinCodeSectionRevealedPreview() {
    PassboltTheme {
        PinCodeSection(
            pinCode = "123456",
            isPinCodeVisible = true,
            onIntent = {},
        )
    }
}
