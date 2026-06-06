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

package net.svaroh.passly.domain.inappreview.usecase

import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.inappreview.InAppReviewRepository
import net.svaroh.passly.domain.inappreview.model.InAppReviewParameters
import net.svaroh.passly.domain.inappreview.model.InAppReviewShowMode.ConsecutiveShow
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import java.time.Period

class InAppReviewInteractor(
    private val inAppReviewRepository: InAppReviewRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val clock: Clock,
) {
    fun shouldShowInAppReviewFlow(): Boolean {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val parameters = inAppReviewRepository.getInAppReviewParameters(userId)
        val showMode = inAppReviewRepository.getInAppReviewShowMode(userId)

        val minimumIntervalPassed =
            parameters.inAppReviewShowIntervalStartDate != null &&
                Period
                    .between(
                        parameters.inAppReviewShowIntervalStartDate,
                        LocalDate.now(clock),
                    ).days > showMode.daysCount
        val minimumSignInsPassed = parameters.signInCount > showMode.signInCount

        val logTemplate =
            "Checking in app review show parameters. " +
                "Show mode is: %s. " +
                "Show interval start date: %s. " +
                "Sign in count is: %d. " +
                "Should show review: %s"

        Timber.d(
            logTemplate,
            showMode.javaClass.simpleName,
            parameters.inAppReviewShowIntervalStartDate,
            parameters.signInCount,
            minimumSignInsPassed && minimumIntervalPassed,
        )

        return minimumSignInsPassed && minimumIntervalPassed
    }

    fun inAppReviewFlowShowed() {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        inAppReviewRepository.saveInAppReviewShowMode(userId, ConsecutiveShow())
        inAppReviewRepository.saveInAppReviewParameters(
            userId,
            InAppReviewParameters(
                inAppReviewShowIntervalStartDate = null,
                signInCount = 0,
            ),
        )
    }

    fun processSuccessfulSignIn() {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val currentParameters = inAppReviewRepository.getInAppReviewParameters(userId)
        val newSignInCount = currentParameters.signInCount + 1
        val newStartIntervalDate = currentParameters.inAppReviewShowIntervalStartDate ?: LocalDate.now(clock)

        inAppReviewRepository.saveInAppReviewParameters(
            userId,
            InAppReviewParameters(
                inAppReviewShowIntervalStartDate = newStartIntervalDate,
                signInCount = newSignInCount,
            ),
        )
    }
}
