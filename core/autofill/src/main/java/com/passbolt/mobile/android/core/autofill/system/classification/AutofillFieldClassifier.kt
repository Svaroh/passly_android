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

package net.svaroh.passly.core.autofill.system.classification

import net.svaroh.passly.core.autofill.system.AutofillField
import net.svaroh.passly.core.autofill.system.FillableInputsFinder
import net.svaroh.passly.core.autofill.system.classification.FillClassification.Credentials
import net.svaroh.passly.core.autofill.system.classification.FillClassification.CredentialsAndTotp
import net.svaroh.passly.core.autofill.system.classification.FillClassification.Totp
import net.svaroh.passly.ui.ParsedStructure
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class AutofillFieldClassifier(
    private val fillableInputsFinder: FillableInputsFinder,
) {
    fun classifyFill(structures: Set<ParsedStructure>): FillClassification? {
        val username = findAutofillableView(AutofillField.USERNAME, structures)
        val password = findAutofillableView(AutofillField.PASSWORD, structures)
        val totp = findAutofillableView(AutofillField.TOTP, structures)

        return when {
            canFillCredentialsAndTotp(username, password, totp) ->
                CredentialsAndTotp(username, password, totp)
            canFillCredentials(username, password) ->
                Credentials(username, password)
            totp != null ->
                Totp(totp)
            else -> null
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun canFillCredentials(
        username: ParsedStructure?,
        password: ParsedStructure?,
    ): Boolean {
        contract {
            returns(true) implies (username != null)
            returns(true) implies (password != null)
        }
        return username != null &&
            password != null &&
            username.domain == password.domain
    }

    @OptIn(ExperimentalContracts::class)
    private fun canFillCredentialsAndTotp(
        username: ParsedStructure?,
        password: ParsedStructure?,
        totp: ParsedStructure?,
    ): Boolean {
        contract {
            returns(true) implies (username != null)
            returns(true) implies (password != null)
            returns(true) implies (totp != null)
        }
        return canFillCredentials(username, password) &&
            totp != null &&
            password.domain == totp.domain
    }

    private fun findAutofillableView(
        field: AutofillField,
        autofillStructure: Set<ParsedStructure>,
    ) = fillableInputsFinder.findStructureForAutofillFields(field, autofillStructure)
}
