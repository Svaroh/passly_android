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

package net.svaroh.passly.feature.setup.scanqr

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.common.HttpsVerifier
import net.svaroh.passly.common.UuidProvider
import net.svaroh.passly.common.usecase.FetchFileAsStringUseCase
import net.svaroh.passly.core.accounts.AccountKitParser
import net.svaroh.passly.core.accounts.AccountsInteractor
import net.svaroh.passly.core.accounts.AccountsInteractor.InjectAccountFailureType.ACCOUNT_ALREADY_LINKED
import net.svaroh.passly.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_NON_HTTPS_DOMAIN
import net.svaroh.passly.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_WHEN_SAVING_PRIVATE_KEY
import net.svaroh.passly.core.accounts.usecase.accountdata.UpdateAccountDataUseCase
import net.svaroh.passly.core.accounts.usecase.accounts.CheckAccountExistsUseCase
import net.svaroh.passly.core.accounts.usecase.privatekey.SavePrivateKeyUseCase
import net.svaroh.passly.core.accounts.usecase.selectedaccount.SaveCurrentApiUrlUseCase
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.navigation.AccountSetupDataModel
import net.svaroh.passly.feature.setup.scanqr.ScanQrIntent.AccessLogs
import net.svaroh.passly.feature.setup.scanqr.ScanQrIntent.ConfirmSetupLeave
import net.svaroh.passly.feature.setup.scanqr.ScanQrIntent.DismissHelpMenu
import net.svaroh.passly.feature.setup.scanqr.ScanQrIntent.DismissServerNotReachable
import net.svaroh.passly.feature.setup.scanqr.ScanQrIntent.DismissSetupLeave
import net.svaroh.passly.feature.setup.scanqr.ScanQrIntent.GoBack
import net.svaroh.passly.feature.setup.scanqr.ScanQrIntent.ImportProfileManually
import net.svaroh.passly.feature.setup.scanqr.ScanQrIntent.OpenHelpMenu
import net.svaroh.passly.feature.setup.scanqr.ScanQrIntent.SelectedAccountKit
import net.svaroh.passly.feature.setup.scanqr.ScanQrIntent.StartCameraError
import net.svaroh.passly.feature.setup.scanqr.ScanQrSideEffect.NavigateBack
import net.svaroh.passly.feature.setup.scanqr.ScanQrSideEffect.NavigateToImportProfile
import net.svaroh.passly.feature.setup.scanqr.ScanQrSideEffect.NavigateToLogs
import net.svaroh.passly.feature.setup.scanqr.ScanQrSideEffect.NavigateToSummary
import net.svaroh.passly.feature.setup.scanqr.ScanQrState.TooltipMessage.CAMERA_ERROR
import net.svaroh.passly.feature.setup.scanqr.qrparser.ParseResult
import net.svaroh.passly.feature.setup.scanqr.qrparser.ParseResult.Failure
import net.svaroh.passly.feature.setup.scanqr.qrparser.ParseResult.FinishedWithSuccess
import net.svaroh.passly.feature.setup.scanqr.qrparser.ParseResult.PassboltQr
import net.svaroh.passly.feature.setup.scanqr.qrparser.ParseResult.PassboltQr.AccountKitPage
import net.svaroh.passly.feature.setup.scanqr.qrparser.ParseResult.PassboltQr.BrowserFirstLoginPage
import net.svaroh.passly.feature.setup.scanqr.qrparser.ParseResult.PassboltQr.FirstPage
import net.svaroh.passly.feature.setup.scanqr.qrparser.ParseResult.PassboltQr.SubsequentPage
import net.svaroh.passly.feature.setup.scanqr.qrparser.ParseResult.ScanFailure
import net.svaroh.passly.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError
import net.svaroh.passly.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError.ErrorType.MULTIPLE_BARCODES
import net.svaroh.passly.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError.ErrorType.NOT_A_PASSBOLT_QR
import net.svaroh.passly.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE
import net.svaroh.passly.feature.setup.scanqr.qrparser.ScanQrParser
import net.svaroh.passly.feature.setup.scanqr.usecase.UpdateTransferUseCase
import net.svaroh.passly.ui.ResultStatus
import net.svaroh.passly.ui.Status
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.properties.Delegates

internal class ScanQrViewModel(
    private val updateTransferUseCase: UpdateTransferUseCase,
    private val qrParser: ScanQrParser,
    private val uuidProvider: UuidProvider,
    private val savePrivateKeyUseCase: SavePrivateKeyUseCase,
    private val updateAccountDataUseCase: UpdateAccountDataUseCase,
    private val checkAccountExistsUseCase: CheckAccountExistsUseCase,
    private val httpsVerifier: HttpsVerifier,
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase,
    private val accountsInteractor: AccountsInteractor,
    private val accountKitParser: AccountKitParser,
    private val fetchFileAsStringUseCase: FetchFileAsStringUseCase,
) : SideEffectViewModel<ScanQrState, ScanQrSideEffect>(ScanQrState()) {
    private lateinit var authToken: String
    private lateinit var transferUuid: String
    private lateinit var userId: String
    private lateinit var serverDomain: String
    private var totalPages: Int by Delegates.notNull()
    private var currentPage = 0
    private var qrScanningJob: Job? = null

    fun onIntent(intent: ScanQrIntent) {
        when (intent) {
            GoBack -> updateViewState { copy(showSetupLeaveConfirmationDialog = true) }
            ConfirmSetupLeave -> {
                updateViewState { copy(showSetupLeaveConfirmationDialog = false) }
                emitSideEffect(NavigateBack)
            }
            DismissSetupLeave -> updateViewState { copy(showSetupLeaveConfirmationDialog = false) }
            OpenHelpMenu -> updateViewState { copy(showHelpMenu = true) }
            DismissHelpMenu -> updateViewState { copy(showHelpMenu = false) }
            ImportProfileManually -> emitSideEffect(NavigateToImportProfile)
            AccessLogs -> emitSideEffect(NavigateToLogs)
            is StartCameraError -> {
                Timber.e(intent.exception)
                updateViewState { copy(tooltipMessage = CAMERA_ERROR) }
            }
            DismissServerNotReachable -> updateViewState { copy(showServerNotReachableDialog = false) }
            is SelectedAccountKit -> accountKitSelected(intent.accountKit)
            is ScanQrIntent.Initialize -> initialize(intent)
        }
    }

    private fun initialize(intent: ScanQrIntent.Initialize) {
        if (intent.accountSetupDataModel != null) {
            injectPredefinedAccount(intent.accountSetupDataModel)
        } else {
            qrScanningJob?.cancel()
            qrScanningJob =
                viewModelScope.launch {
                    launch { qrParser.startParsing(intent.barcodeScanFlow) }
                    launch { qrParser.parseResultFlow.collect { processParseResult(it) } }
                }
        }
    }

    private suspend fun processParseResult(parserResult: ParseResult) {
        when (parserResult) {
            is Failure -> parserFailure(parserResult.exception)
            is PassboltQr ->
                when (parserResult) {
                    is FirstPage -> parserFirstPage(parserResult)
                    is SubsequentPage -> parserSubsequentPage(parserResult)
                    is AccountKitPage -> setupFromAccountKit(parserResult)
                    is BrowserFirstLoginPage -> updateViewState { copy(tooltipMessage = ScanQrState.TooltipMessage.NOT_A_PASSBOLT_QR) }
                }
            is FinishedWithSuccess -> parserFinishedWithSuccess(parserResult.armoredKey)
            is UserResolvableError ->
                when (parserResult.errorType) {
                    MULTIPLE_BARCODES -> updateViewState { copy(tooltipMessage = ScanQrState.TooltipMessage.MULTIPLE_BARCODES) }
                    NO_BARCODES_IN_RANGE -> updateViewState { copy(tooltipMessage = ScanQrState.TooltipMessage.CENTER_CAMERA_ON_BARCODE) }
                    NOT_A_PASSBOLT_QR -> updateViewState { copy(tooltipMessage = ScanQrState.TooltipMessage.NOT_A_PASSBOLT_QR) }
                }
            is ScanFailure ->
                updateViewState {
                    copy(
                        tooltipMessage = ScanQrState.TooltipMessage.SCAN_ERROR,
                        scanErrorMessage = parserResult.exception?.message,
                    )
                }
        }
    }

    private suspend fun setupFromAccountKit(accountKitPage: AccountKitPage) {
        updateViewState { copy(showProgress = true) }
        val failureAction = { emitSideEffect(NavigateToSummary(ResultStatus.Failure(""))) }
        try {
            when (
                val fileContentResult =
                    fetchFileAsStringUseCase.execute(
                        FetchFileAsStringUseCase.Input(accountKitPage.content.accountKitUrl),
                    )
            ) {
                is FetchFileAsStringUseCase.Output.Failure -> failureAction()
                is FetchFileAsStringUseCase.Output.Success ->
                    accountKitParser.parseAndVerify(
                        fileContentResult.fileContent,
                        onSuccess = { setupDataModel -> injectPredefinedAccount(setupDataModel) },
                        onFailure = { failureAction() },
                    )
            }
            updateViewState { copy(showProgress = false) }
        } catch (e: Exception) {
            Timber.e(e, "Error while reading account kit file")
            updateViewState { copy(showProgress = false) }
            emitSideEffect(NavigateToSummary(ResultStatus.Failure("")))
        }
    }

    private suspend fun parserFailure(exception: Throwable?) {
        exception?.let { Timber.e(it) }
        updateTransfer(pageNumber = currentPage, Status.ERROR)
    }

    private suspend fun parserFirstPage(firstPage: FirstPage) {
        val userId = firstPage.content.userId
        transferUuid = firstPage.content.transferId.toString()
        authToken = firstPage.content.authenticationToken
        totalPages = firstPage.content.totalPages
        serverDomain = firstPage.content.domain

        val userExistsResult = checkAccountExistsUseCase.execute(CheckAccountExistsUseCase.Input(userId.toString()))
        if (userExistsResult.exist) {
            currentPage = totalPages - 1
            updateTransferAlreadyLinked(currentPage)
        } else if (!httpsVerifier.isHttps(firstPage.content.domain)) {
            emitSideEffect(NavigateToSummary(ResultStatus.HttpNotSupported()))
        } else {
            if (currentPage > 0) {
                parserFailure(Throwable("Other qr code scanning has been already started"))
            } else {
                updateViewState {
                    copy(
                        totalPages = this@ScanQrViewModel.totalPages,
                        tooltipMessage = ScanQrState.TooltipMessage.KEEP_GOING,
                    )
                }
                saveAccountDetails(userId.toString(), firstPage.content.domain)
                updateTransfer(pageNumber = firstPage.reservedBytesDto.page + 1)
            }
        }
    }

    private suspend fun parserSubsequentPage(subsequentPage: SubsequentPage) {
        currentPage = subsequentPage.reservedBytesDto.page
        updateViewState { copy(tooltipMessage = ScanQrState.TooltipMessage.KEEP_GOING) }

        if (subsequentPage.reservedBytesDto.page < totalPages - 1) {
            updateTransfer(pageNumber = currentPage + 1)
        } else {
            qrParser.verifyScannedKey()
        }
    }

    private suspend fun parserFinishedWithSuccess(armoredKey: String) {
        when (savePrivateKeyUseCase.execute(SavePrivateKeyUseCase.Input(userId, armoredKey))) {
            SavePrivateKeyUseCase.Output.Failure -> {
                updateTransfer(pageNumber = currentPage, Status.ERROR)
                emitSideEffect(NavigateToSummary(ResultStatus.Failure("")))
            }
            SavePrivateKeyUseCase.Output.Success -> {
                updateTransfer(pageNumber = currentPage, Status.COMPLETE)
                emitSideEffect(NavigateToSummary(ResultStatus.Success(userId)))
            }
        }
    }

    private suspend fun updateTransferAlreadyLinked(pageNumber: Int) {
        updateTransferUseCase.execute(
            UpdateTransferUseCase.Input(
                uuid = transferUuid,
                authToken = authToken,
                currentPage = pageNumber,
                status = Status.COMPLETE,
            ),
        )
        // ignoring result
        emitSideEffect(NavigateToSummary(ResultStatus.AlreadyLinked()))
    }

    private suspend fun updateTransfer(
        pageNumber: Int,
        status: Status = Status.IN_PROGRESS,
    ) {
        // in case of the first qr code is not a correct one
        if (!::transferUuid.isInitialized || !::authToken.isInitialized || !::serverDomain.isInitialized) {
            emitSideEffect(NavigateToSummary(ResultStatus.Failure("Could not initialize private key transfer")))
            return
        }
        val response =
            updateTransferUseCase.execute(
                UpdateTransferUseCase.Input(
                    uuid = transferUuid,
                    authToken = authToken,
                    currentPage = pageNumber,
                    status = status,
                ),
            )
        when (response) {
            is UpdateTransferUseCase.Output.Failure -> {
                Timber.e(response.error.exception, "There was an error during transfer update")
                if (status == Status.ERROR || status == Status.CANCEL) {
                    // ignoring
                } else {
                    if (response.error.isServerNotReachable) {
                        updateViewState {
                            copy(
                                showServerNotReachableDialog = true,
                                serverDomain = serverDomain,
                            )
                        }
                    } else if (response.error.isNoNetworkException) {
                        emitSideEffect(NavigateToSummary(ResultStatus.NoNetwork()))
                    } else {
                        emitSideEffect(ScanQrSideEffect.ShowToast(ToastType.UPDATE_TRANSFER_ERROR))
                    }
                }
            }
            is UpdateTransferUseCase.Output.Success -> {
                onUpdateTransferSuccess(pageNumber, status, response)
            }
        }
    }

    private fun onUpdateTransferSuccess(
        pageNumber: Int,
        status: Status,
        response: UpdateTransferUseCase.Output.Success,
    ) {
        updateViewState { copy(currentPage = pageNumber) }
        when (status) {
            Status.COMPLETE -> {
                updateAccountDataUseCase.execute(
                    UpdateAccountDataUseCase.Input(
                        userId = userId,
                        firstName = response.updateTransferModel.firstName,
                        lastName = response.updateTransferModel.lastName,
                        avatarUrl = response.updateTransferModel.avatarUrl,
                        email = response.updateTransferModel.email,
                    ),
                )
            }
            Status.ERROR -> {
                emitSideEffect(NavigateToSummary(ResultStatus.Failure("")))
            }
            else -> {
                // ignoring
            }
        }
    }

    private fun saveAccountDetails(
        serverId: String,
        url: String,
    ) {
        userId = uuidProvider.get()
        saveCurrentApiUrlUseCase.execute(SaveCurrentApiUrlUseCase.Input(url))
        updateAccountDataUseCase.execute(
            UpdateAccountDataUseCase.Input(
                userId = userId,
                url = url,
                serverId = serverId,
            ),
        )
    }

    private fun injectPredefinedAccount(accountSetupData: AccountSetupDataModel) {
        accountsInteractor.injectPredefinedAccountData(
            accountSetupData,
            onSuccess = { userId ->
                emitSideEffect(NavigateToSummary(ResultStatus.Success(userId)))
            },
            onFailure = { failureType ->
                emitSideEffect(
                    NavigateToSummary(
                        when (failureType) {
                            ACCOUNT_ALREADY_LINKED -> ResultStatus.AlreadyLinked()
                            ERROR_NON_HTTPS_DOMAIN -> ResultStatus.HttpNotSupported()
                            ERROR_WHEN_SAVING_PRIVATE_KEY -> ResultStatus.Failure(failureType.name)
                        },
                    ),
                )
            },
        )
    }

    private fun accountKitSelected(accountKit: String) {
        viewModelScope.launch {
            accountKitParser.parseAndVerify(
                accountKit,
                onSuccess = { injectPredefinedAccount(it) },
                onFailure = { emitSideEffect(NavigateToSummary(ResultStatus.Failure(""))) },
            )
        }
    }
}
