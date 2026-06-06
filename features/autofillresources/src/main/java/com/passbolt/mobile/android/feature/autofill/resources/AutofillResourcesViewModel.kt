package net.svaroh.passly.feature.autofill.resources

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.otpcore.TotpParametersProvider
import net.svaroh.passly.core.otpcore.TotpParametersProvider.OtpParametersResult.InvalidTotpInput
import net.svaroh.passly.core.otpcore.TotpParametersProvider.OtpParametersResult.OtpParameters
import net.svaroh.passly.domain.accounts.usecase.GetAccountsUseCase
import net.svaroh.passly.domain.resources.actions.SecretPropertiesActionsInteractor
import net.svaroh.passly.domain.resources.actions.performSecretPropertyAction
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.domain.secrets.model.SecretJsonModel
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesIntent.NewResourceCreated
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesIntent.SelectAutofillItem
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesIntent.UserAuthenticated
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesSideEffect.AutofillReturn
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToAuth
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToSetup
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesSideEffect.ShowToast
import net.svaroh.passly.feature.autofill.resources.ToastType.DECRYPTION_FAILURE
import net.svaroh.passly.feature.autofill.resources.ToastType.FETCH_FAILURE
import net.svaroh.passly.feature.autofill.resources.ToastType.INVALID_TOTP_PARAMETERS
import net.svaroh.passly.feature.autofill.resources.datasetstrategy.AutofillPayload
import net.svaroh.passly.jsonmodel.delegates.TotpSecret
import net.svaroh.passly.ui.ResourceUiModel
import net.svaroh.passly.ui.contentType
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class AutofillResourcesViewModel(
    getAccountsUseCase: GetAccountsUseCase,
    private val uri: String?,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val totpParametersProvider: TotpParametersProvider,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<AutofillResourcesState, AutofillResourcesSideEffect>(AutofillResourcesState()),
    KoinComponent {
    init {
        if (getAccountsUseCase.execute(Unit).users.isNotEmpty()) {
            emitSideEffect(NavigateToAuth)
        } else {
            emitSideEffect(NavigateToSetup)
        }
    }

    fun onIntent(intent: AutofillResourcesIntent) {
        when (intent) {
            is UserAuthenticated -> userAuthenticated()
            is SelectAutofillItem -> selectAutofillItem(intent.resourceModel)
            is NewResourceCreated -> newResourceCreated(intent.resourceId)
        }
    }

    private fun userAuthenticated() {
        updateViewState { copy(showHome = true) }
    }

    private fun selectAutofillItem(resource: ResourceUiModel) {
        updateViewState { copy(showProgress = true) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            val payload = buildPayload(resource)
            if (payload != null) {
                emitSideEffect(AutofillReturn(payload))
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private suspend fun buildPayload(resource: ResourceUiModel): AutofillPayload? {
        val contentType = resource.contentType()
        val username = resource.metadataJsonModel.username
        val secret = fetchDecryptedSecret(resource)

        val password = secret?.getPassword(contentType)
        val totpCode = secret?.totp?.let { totpCode(it) }

        return if (username == null && password == null && totpCode == null) {
            null
        } else {
            AutofillPayload(
                username = username,
                password = password,
                totpCode = totpCode,
                uri = uri,
            )
        }
    }

    private suspend fun fetchDecryptedSecret(resource: ResourceUiModel): SecretJsonModel? {
        val interactor: SecretPropertiesActionsInteractor = get { parametersOf(resource) }
        var secret: SecretJsonModel? = null
        performSecretPropertyAction(
            action = { interactor.provideDecryptedSecret() },
            doOnFetchFailure = { emitSideEffect(ShowToast(FETCH_FAILURE)) },
            doOnDecryptionFailure = { emitSideEffect(ShowToast(DECRYPTION_FAILURE)) },
            doOnSuccess = { secret = it.result },
        )
        return secret
    }

    private fun totpCode(totp: TotpSecret): String? =
        when (
            val parameters =
                totpParametersProvider.provideOtpParameters(
                    secretKey = totp.key,
                    digits = totp.digits,
                    period = totp.period,
                    algorithm = totp.algorithm,
                )
        ) {
            is OtpParameters -> parameters.otpValue
            InvalidTotpInput -> {
                Timber.e("Invalid TOTP parameters")
                emitSideEffect(ShowToast(INVALID_TOTP_PARAMETERS))
                null
            }
        }

    private fun newResourceCreated(resourceId: String) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            selectAutofillItem(
                getLocalResourceUseCase
                    .execute(
                        GetLocalResourceUseCase.Input(resourceId),
                    ).resource,
            )
        }
    }
}
