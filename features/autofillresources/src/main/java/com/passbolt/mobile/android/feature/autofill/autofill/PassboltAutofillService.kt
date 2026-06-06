package net.svaroh.passly.feature.autofill.autofill

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import net.svaroh.passly.core.autofill.system.AssistStructureParser
import net.svaroh.passly.core.autofill.system.classification.AutofillFieldClassifier
import net.svaroh.passly.core.navigation.ActivityIntents
import net.svaroh.passly.core.navigation.AutofillMode
import net.svaroh.passly.core.navigation.AutofillType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

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

@SuppressLint("UnspecifiedImmutableFlag")
class PassboltAutofillService :
    AutofillService(),
    KoinComponent {
    private val assistStructureParser: AssistStructureParser by inject()
    private val autofillFieldClassifier: AutofillFieldClassifier by inject()
    private val remoteViewsFactory: RemoteViewsFactory by inject()

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        Timber.d("Received fill request")
        runCatching {
            val parsedAutofillStructures =
                assistStructureParser
                    .parse(
                        request.fillContexts.last().structure,
                    )

            val classification = autofillFieldClassifier.classifyFill(parsedAutofillStructures.structures)

            // autofillable views not found
            if (parsedAutofillStructures.hasDifferentDomains || classification == null) {
                Timber.d("Did not find any autofillable views or views have different web domains")
                null
            } else {
                Timber.d("Showing authentication prompt for ${classification.type}")
                FillResponse
                    .Builder()
                    .setAuthentication(
                        classification.allFields.map { it.id }.toTypedArray(),
                        autofillResourcesPendingIntent(
                            uri = classification.anchorDomain,
                            type = classification.type,
                        ).intentSender,
                        remoteViewsFactory.getAutofillSelectDropdown(),
                    ).build()
            }
        }.onFailure {
            Timber.e(it)
            callback.onFailure(it.message)
        }.onSuccess {
            callback.onSuccess(it)
        }
    }

    override fun onSaveRequest(
        request: SaveRequest,
        callback: SaveCallback,
    ) {
        // save is currently not supported
    }

    private fun autofillResourcesPendingIntent(
        uri: String?,
        type: AutofillType,
    ): PendingIntent =
        PendingIntent.getActivity(
            applicationContext,
            AUTOFILL_RESOURCES_REQUEST_CODE,
            ActivityIntents.autofill(
                context = this,
                autofillModeName = AutofillMode.AUTOFILL.name,
                uri = uri,
                autofillTypeName = type.name,
            ),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

    private companion object {
        private const val AUTOFILL_RESOURCES_REQUEST_CODE = 1001
    }
}
