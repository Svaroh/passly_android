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

package com.passbolt.mobile.android.feature.startup.deprecatedoswarning

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.compose.PassboltTheme
import com.passbolt.mobile.android.core.ui.bottomsheet.BottomSheetHeader
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeprecatedOsWarningBottomSheet(
    onAcknowledge: () -> Unit,
    onHide: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onAcknowledge,
        containerColor = colorResource(CoreUiR.color.elevated_background),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        DeprecatedOsWarningContent(
            onAcknowledge = onAcknowledge,
            onHide = onHide,
        )
    }
}

@Composable
private fun DeprecatedOsWarningContent(
    onAcknowledge: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
    ) {
        BottomSheetHeader(
            title = stringResource(LocalizationR.string.deprecated_os_warning_title),
            onClose = onAcknowledge,
        )

        Icon(
            painter = painterResource(CoreUiR.drawable.ic_alert_triangle),
            contentDescription = null,
            tint = colorResource(CoreUiR.color.warning),
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 24.dp)
                    .size(126.dp),
        )

        Text(
            text = stringResource(LocalizationR.string.deprecated_os_warning_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp),
        )

        Text(
            text = AnnotatedString.fromHtml(stringResource(LocalizationR.string.deprecated_os_warning_call_to_action)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
        )

        PrimaryButton(
            text = stringResource(LocalizationR.string.deprecated_os_warning_acknowledge),
            onClick = onAcknowledge,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 32.dp),
        )

        TextButton(
            onClick = onHide,
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 24.dp),
        ) {
            Text(
                text = stringResource(LocalizationR.string.deprecated_os_warning_hide),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeprecatedOsWarningContentPreview() {
    PassboltTheme {
        DeprecatedOsWarningContent(
            onAcknowledge = {},
            onHide = {},
        )
    }
}
