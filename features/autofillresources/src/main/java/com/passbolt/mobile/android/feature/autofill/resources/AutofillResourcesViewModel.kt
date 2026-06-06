package net.svaroh.passly.feature.autofill.resources

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.core.accounts.usecase.accounts.GetAccountsUseCase
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.resources.actions.SecretPropertiesActionsInteractor
import net.svaroh.passly.core.resources.actions.performSecretPropertyAction
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesIntent.NewResourceCreated
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesIntent.SelectAutofillItem
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesIntent.UserAuthenticated
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesSideEffect.AutofillReturn
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToAuth
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToSetup
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesSideEffect.ShowToast
import net.svaroh.passly.feature.autofill.resources.ToastType.DECRYPTION_FAILURE
import net.svaroh.passly.feature.autofill.resources.ToastType.FETCH_FAILURE
import net.svaroh.passly.ui.ResourceModel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class AutofillResourcesViewModel(
    getAccountsUseCase: GetAccountsUseCase,
    private val uri: String?,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
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

    private fun selectAutofillItem(resourceModel: ResourceModel) {
        updateViewState { copy(showProgress = true) }
        val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor =
            get { parametersOf(resourceModel) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.providePassword() },
                doOnFetchFailure = { emitSideEffect(ShowToast(FETCH_FAILURE)) },
                doOnDecryptionFailure = { emitSideEffect(ShowToast(DECRYPTION_FAILURE)) },
                doOnSuccess = {
                    emitSideEffect(
                        AutofillReturn(
                            username = resourceModel.metadataJsonModel.username.orEmpty(),
                            password = it.result.orEmpty(),
                            uri = uri,
                        ),
                    )
                },
            )
            updateViewState { copy(showProgress = false) }
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
