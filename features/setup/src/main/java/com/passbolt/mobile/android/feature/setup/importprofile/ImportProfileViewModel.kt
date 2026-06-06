package net.svaroh.passly.feature.setup.importprofile

import net.svaroh.passly.common.validation.StringIsHttpsWebUrl
import net.svaroh.passly.common.validation.StringIsUuid
import net.svaroh.passly.common.validation.StringNotBlank
import net.svaroh.passly.common.validation.validation
import net.svaroh.passly.core.accounts.AccountsInteractor
import net.svaroh.passly.core.accounts.AccountsInteractor.InjectAccountFailureType.ACCOUNT_ALREADY_LINKED
import net.svaroh.passly.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_NON_HTTPS_DOMAIN
import net.svaroh.passly.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_WHEN_SAVING_PRIVATE_KEY
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.navigation.AccountSetupDataModel
import net.svaroh.passly.feature.setup.importprofile.ImportProfileIntent.ChangeAccountUrl
import net.svaroh.passly.feature.setup.importprofile.ImportProfileIntent.ChangePrivateKey
import net.svaroh.passly.feature.setup.importprofile.ImportProfileIntent.ChangeUserId
import net.svaroh.passly.feature.setup.importprofile.ImportProfileIntent.Import
import net.svaroh.passly.feature.setup.importprofile.ImportProfileSideEffect.NavigateBack
import net.svaroh.passly.feature.setup.importprofile.ImportProfileSideEffect.NavigateToSummary
import net.svaroh.passly.ui.ResultStatus.AlreadyLinked
import net.svaroh.passly.ui.ResultStatus.Failure
import net.svaroh.passly.ui.ResultStatus.HttpNotSupported
import net.svaroh.passly.ui.ResultStatus.Success

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

internal class ImportProfileViewModel(
    private val accountsInteractor: AccountsInteractor,
) : SideEffectViewModel<ImportProfileState, ImportProfileSideEffect>(ImportProfileState()) {
    fun onIntent(intent: ImportProfileIntent) {
        when (intent) {
            is ChangeUserId -> updateViewState { copy(userId = intent.userId.trim()) }
            is ChangeAccountUrl -> updateViewState { copy(accountUrl = intent.accountUrl.trim()) }
            is ChangePrivateKey -> updateViewState { copy(privateKey = intent.privateKey.trim()) }
            ImportProfileIntent.GoBack -> emitSideEffect(NavigateBack)
            Import -> importAccount()
        }
    }

    private fun importAccount() {
        clearValidationErrors()

        val currentState = viewState.value

        validation {
            of(currentState.userId) {
                withRules(StringNotBlank, StringIsUuid) {
                    onInvalid {
                        updateViewState {
                            copy(hasUserIdValidationError = true)
                        }
                    }
                }
            }
            of(currentState.accountUrl) {
                withRules(StringNotBlank, StringIsHttpsWebUrl) {
                    onInvalid {
                        updateViewState {
                            copy(hasAccountUrlValidationError = true)
                        }
                    }
                }
            }
            of(currentState.privateKey) {
                withRules(StringNotBlank) {
                    onInvalid {
                        updateViewState {
                            copy(hasPrivateKeyValidationError = true)
                        }
                    }
                }
            }
            onValid {
                performImport()
            }
        }
    }

    private fun clearValidationErrors() {
        updateViewState {
            copy(
                hasUserIdValidationError = false,
                hasAccountUrlValidationError = false,
                hasPrivateKeyValidationError = false,
            )
        }
    }

    private fun performImport() {
        val currentState = viewState.value
        accountsInteractor.injectPredefinedAccountData(
            AccountSetupDataModel.withRequiredValues(
                serverUserId = currentState.userId,
                domain = currentState.accountUrl,
                armoredKey = currentState.privateKey,
            ),
            onSuccess = { userId ->
                emitSideEffect(NavigateToSummary(Success(userId)))
            },
            onFailure = { failureType ->
                emitSideEffect(
                    NavigateToSummary(
                        when (failureType) {
                            ACCOUNT_ALREADY_LINKED -> AlreadyLinked()
                            ERROR_NON_HTTPS_DOMAIN -> HttpNotSupported()
                            ERROR_WHEN_SAVING_PRIVATE_KEY -> Failure(failureType.name)
                        },
                    ),
                )
            },
        )
    }
}
